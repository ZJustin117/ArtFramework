plugins {
    java
}

val artVersion: String =
    providers.gradleProperty("artframework.version").orNull
        ?: throw GradleException("Missing artframework.version in gradle.properties")

version = artVersion
group = "artframework"

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
    testImplementation(files(stsJar, baseModJar, modTheSpireJar))
    testImplementation("junit:junit:4.13.2")
}

tasks.processResources {
    inputs.property("artframework.version", artVersion)
    filesMatching("ModTheSpire.json") {
        filter { line: String ->
            line.replace("@ART_VERSION@", artVersion)
        }
    }
}

tasks.jar {
    archiveFileName = "ArtFramework.jar"
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    // compileOnly STS/BaseMod/MTS — do not fat-jar game deps into ArtFramework.jar
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to "ArtFramework",
                "Implementation-Version" to artVersion,
                "Implementation-Vendor" to "ArtFramework",
                "ModTheSpire-ModId" to "artframework",
            ),
        )
    }
}

tasks.register("installDistJar") {
    group = "distribution"
    description = "Copy ArtFramework.jar to -PinstallDir=... (consumer mods folder or build cache)"
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
    val junitXml = reports.getByName("junitXml") as org.gradle.api.tasks.testing.JUnitXmlReport
    junitXml.includeSystemOutLog.set(false)
    junitXml.includeSystemErrLog.set(false)
    reports.html.required.set(false)
    testLogging {
        events("passed", "failed", "skipped")
    }
}
