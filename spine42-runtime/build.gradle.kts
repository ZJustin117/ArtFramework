import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.io.FileOutputStream

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "artframework"
version = "1.0.0-alpha.4"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val spineSources = configurations.create("spineSources")

dependencies {
    compileOnly(files(System.getenv("ART_STS_JAR") ?: ""))
    compileOnly("com.badlogicgames.gdx:gdx:1.14.0")
    add(spineSources.name, "com.esotericsoftware.spine:spine-libgdx:4.2.12:sources")
}

val prepareSpineSources = tasks.register("prepareSpineSources") {
    val output = layout.buildDirectory.dir("generated/spine42-src")
    outputs.dir(output)
    doLast {
        val dir = output.get().asFile
        dir.deleteRecursively()
        dir.mkdirs()
        copy {
            val sourceJar = spineSources.files.single { it.name.startsWith("spine-libgdx-") && it.name.endsWith("-sources.jar") }
            from(zipTree(sourceJar))
            into(dir)
        }
        fileTree(dir).matching { include("**/*.java") }.forEach { file ->
            val normalized = file.invariantSeparatorsPath
            if (normalized.endsWith("/SkeletonRenderer.java")
                    || normalized.endsWith("/SkeletonJson.java")
                    || normalized.endsWith("/SkeletonActor.java")
                    || normalized.endsWith("/SkeletonActorPool.java")
                    || normalized.endsWith("/SkeletonDrawable.java")
                    || normalized.endsWith("/SkeletonDataLoader.java")
                    || normalized.endsWith("/TwoColorPolygonBatch.java")) {
                file.delete()
                return@forEach
            }
            var text = file.readText()
            text = text.replace("com.esotericsoftware.spine", "artframework.shaded.spine42.com.esotericsoftware.spine")
            text = text.replace("region.degrees", "(region.rotate ? 90 : 0)")
            if (file.name == "BlendMode.java") {
                text = text.replace(
                    "batch.setBlendFunctionSeparate(premultipliedAlpha ? sourcePMA : source, destColor, sourceAlpha, destColor);",
                    "batch.setBlendFunction(premultipliedAlpha ? sourcePMA : source, destColor);"
                )
            }
            if (file.name == "Skin.java") {
                text = text.replace(
                    "if (!attachments.add(entry)) attachments.get(entry).attachment = attachment;",
                    "for (SkinEntry candidate : attachments.orderedItems()) if (candidate.equals(entry)) { candidate.attachment = attachment; return; }\n\t\tattachments.add(entry);"
                )
                text = text.replace(
                    "SkinEntry entry = attachments.get(lookup);",
                    "SkinEntry entry = null;\n\t\tfor (SkinEntry candidate : attachments.orderedItems()) if (candidate.equals(lookup)) { entry = candidate; break; }"
                )
            }
            if (file.name == "AnimationState.java") {
                text = text.replace(
                    "propertyIds.addAll(((Timeline)timelines[i]).getPropertyIds()) ? HOLD_FIRST : HOLD_SUBSEQUENT",
                    "addAll(propertyIds, ((Timeline)timelines[i]).getPropertyIds()) ? HOLD_FIRST : HOLD_SUBSEQUENT"
                )
                text = text.replace("if (!propertyIds.addAll(ids))", "if (!addAll(propertyIds, ids))")
                val marker = "\n\tprivate void computeHold (TrackEntry entry) {"
                text = text.replace(marker, "\n\tprivate static boolean addAll (ObjectSet<String> set, String[] values) { boolean changed = false; for (String value : values) changed |= set.add(value); return changed; }" + marker)
            }
            file.writeText(text)
        }
        val forbiddenAtlasDegrees = fileTree(dir).matching { include("**/*.java") }
            .filter { it.readText().contains("region.degrees") }
        require(forbiddenAtlasDegrees.isEmpty()) {
            "Spine 4.2 source still references libGDX AtlasRegion.degrees: " + forbiddenAtlasDegrees.joinToString {
                it.invariantSeparatorsPath
            }
        }
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf(prepareSpineSources.map { it }))
    }
}

tasks.compileJava { dependsOn(prepareSpineSources) }

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveFileName = "ArtFramework-Spine42Runtime.jar"
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    configurations = listOf(project.configurations.runtimeClasspath.get())
    exclude("**/*.skel", "**/*.atlas", "**/*.png", "**/*.jpg", "**/*.tres")
    exclude("com/badlogic/gdx/**")
    exclude("com/esotericsoftware/spine.gwt.xml")
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer())
    from("LICENSE-Spine-Runtimes.txt") {
        into("META-INF")
    }
}

tasks.register("verifyRuntimeArtifact") {
    dependsOn(tasks.shadowJar)
    doLast {
        val artifact = tasks.shadowJar.get().archiveFile.get().asFile
        require(artifact.isFile) { "missing runtime artifact: $artifact" }
        ZipFile(artifact).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toList()
            require(names.any { it.startsWith("artframework/shaded/spine42/") }) {
                "runtime artifact contains no relocated Spine classes"
            }
            require(names.none { it.startsWith("com/esotericsoftware/spine/") }) {
                "runtime artifact contains unrelocated Spine classes"
            }
            require(names.none { it.startsWith("com/badlogic/gdx/") }) {
                "runtime artifact contains host libGDX classes"
            }
            require(names.none { it.endsWith(".skel") || it.endsWith(".atlas") || it.endsWith(".png") }) {
                "runtime artifact contains developer assets"
            }
            val atlasLoader = zip.getEntry("artframework/shaded/spine42/com/esotericsoftware/spine/attachments/AtlasAttachmentLoader.class")
                ?: throw IllegalStateException("runtime artifact contains no relocated AtlasAttachmentLoader")
            val loaderBytes = zip.getInputStream(atlasLoader).readBytes()
            require(!String(loaderBytes, Charsets.ISO_8859_1).contains("degrees")) {
                "runtime AtlasAttachmentLoader still references incompatible AtlasRegion.degrees"
            }
        }
    }
}
