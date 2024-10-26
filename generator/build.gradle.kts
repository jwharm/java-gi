plugins {
    id("application")
    id("java-library")
}

group = "io.github.jwharm.javagi"
version = "0.11.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.annotations)
    implementation(libs.javapoet)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(22)
}

application {
    mainClass = "io.github.jwharm.javagi.JavaGI"
}
