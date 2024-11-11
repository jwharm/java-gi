plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    // Workaround for https://github.com/gradle/gradle/issues/15383
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(libs.javapoet)
    implementation(libs.annotations)
    implementation(libs.javagi.generator)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.jdk.get())
}

kotlin {
    jvmToolchain(libs.versions.jdk.get().toInt())
}
