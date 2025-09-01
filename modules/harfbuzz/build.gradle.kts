plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":glib"))
    api(libs.cairo)
}

tasks.withType<GenerateSources> {
    girFiles.set(listOf("HarfBuzz-0.0"))
}
