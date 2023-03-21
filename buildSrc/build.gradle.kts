plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("io.github.jwharm.javagi:generator:1.0")
    implementation("de.undercouch:gradle-download-task:5.3.1")
}