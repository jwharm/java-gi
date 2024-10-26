plugins {
    id("application")
    id("java-library")
    alias(libs.plugins.jlink)
}

group = "io.github.jwharm.javagi"
version = libs.versions.javagi.get()

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.annotations)
    implementation(libs.javapoet)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.jdk.get())
}

application {
    mainClass = "io.github.jwharm.javagi.JavaGI"
}

jlink {
    options = listOf(
        "--strip-debug",
        "--no-header-files",
        "--no-man-pages",
    )
    launcher {
        name = "Java-GI"
    }
}
