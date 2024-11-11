import java.io.*

plugins {
    id("application")
    id("java-library")
}

group = "io.github.jwharm.javagi"
version = libs.versions.javagi.get()

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.picocli)
    implementation(libs.annotations)
    implementation(libs.javapoet)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.jdk.get())
}

application {
    applicationName = "java-gi"
    mainModule = "io.github.jwharm.javagi.generator"
    mainClass = "io.github.jwharm.javagi.JavaGI"
}

// Include the gir-files repository as a zip file with the java-gi command-line
// utility. To decrease size, the documentation is excluded.
tasks.register<Zip>("includeGirFiles") {
    destinationDirectory = layout.buildDirectory.dir("gir-files")
    archiveFileName = "gir-files.zip"

    // Use `../` instead of `rootDir/`, because generator is an included build
    from(projectDir.resolve("../ext/gir-files")) {
        include("**/*.gir")

        // Strip out doc elements from zipped gir files.
        filter(RemoveDocs::class.java)
    }
}

// Filter class that will remove <doc>...</doc> elements
class RemoveDocs(reader: Reader) : FilterReader(StringReader(
    reader.readText().replace(Regex("<doc [\\s\\S]*?</doc>", RegexOption.MULTILINE), "")
))

// Add the created gir-files archive to the resources
tasks.named<ProcessResources>("processResources") {
    dependsOn("includeGirFiles")
    from(layout.buildDirectory.dir("gir-files"))
}
