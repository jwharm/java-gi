plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":glib"))
}

tasks.withType<GenerateSources> {
    girFiles.set(listOf("GdkPixbuf-2.0"))
}
