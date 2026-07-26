plugins {
    java
}

val spireUiVersion: String =
    providers.gradleProperty("spireui.version").orNull
        ?: throw GradleException("Missing spireui.version in gradle.properties")

version = spireUiVersion
group = "spireui"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

fun requiredJar(propertyName: String): File {
    val path = providers.gradleProperty(propertyName).orNull
        ?: throw GradleException("Missing -P$propertyName=/absolute/path/to/file.jar")
    val jar = file(path)
    if (!jar.isFile) {
        throw GradleException("-P$propertyName does not point to a file: ${jar.absolutePath}")
    }
    return jar
}

val stsJar = requiredJar("stsJar")
val baseModJar = requiredJar("baseModJar")
val modTheSpireJar = requiredJar("modTheSpireJar")

dependencies {
    compileOnly(files(stsJar, baseModJar, modTheSpireJar))
    testImplementation("junit:junit:4.13.2")
}

tasks.processResources {
    inputs.property("spireui.version", spireUiVersion)
    filesMatching("ModTheSpire.json") {
        filter { line: String ->
            line.replace("@SPIREUI_VERSION@", spireUiVersion)
        }
    }
}

tasks.jar {
    archiveFileName = "SpireUI.jar"
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    // compileOnly STS/BaseMod/MTS — do not fat-jar game deps into SpireUI.jar
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to "SpireUI",
                "Implementation-Version" to spireUiVersion,
                "Implementation-Vendor" to "SpireUI",
                "ModTheSpire-ModId" to "spireui",
            ),
        )
    }
}

tasks.register("installDistJar") {
    group = "distribution"
    description = "Copy SpireUI.jar to -PinstallDir=... (consumer mods folder or build cache)"
    dependsOn(tasks.jar)
    doLast {
        val installDirPath =
            providers.gradleProperty("installDir").orNull
                ?: throw GradleException("Missing -PinstallDir=/path/to/dir")
        val dir = file(installDirPath)
        if (!dir.isDirectory && !dir.mkdirs()) {
            throw GradleException("Cannot create installDir: ${dir.absolutePath}")
        }
        copy {
            from(tasks.jar.get().archiveFile)
            into(dir)
        }
    }
}

tasks.test {
    testLogging {
        events("passed", "failed", "skipped")
    }
}
