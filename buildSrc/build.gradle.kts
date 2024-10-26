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
    implementation("io.github.jwharm.javagi:generator:0.11.0-SNAPSHOT")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(22)
}

kotlin {
    jvmToolchain(22)
}
