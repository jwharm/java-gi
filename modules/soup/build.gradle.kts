plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":glib"))
}

tasks.withType<GenerateSources> {
    girFiles.set(listOf("Soup-3.0"))
}
