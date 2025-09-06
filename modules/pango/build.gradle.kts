plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":glib"))
    api(project(":harfbuzz"))
    api(libs.cairo)
}

tasks.withType<GenerateSources> {
    girFiles.set(listOf("Pango-1.0", "PangoCairo-1.0"))
}
