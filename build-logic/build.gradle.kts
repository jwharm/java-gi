plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.javapoet)
    implementation(libs.jspecify)
    implementation(libs.javagi.generator)
    implementation(libs.mavenpublish)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.jdk.get())
}

kotlin {
    jvmToolchain(libs.versions.jdk.get().toInt())
}
