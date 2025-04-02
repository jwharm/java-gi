plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.javapoet)
    implementation(libs.annotations)
    implementation(libs.javagi.generator)
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
