plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.javapoet)
    implementation(libs.annotations)
    implementation(libs.javagi.generator)
    implementation(libs.jreleaser)
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
