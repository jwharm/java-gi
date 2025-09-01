plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":glib"))
    api(project(":gtk"))
}

tasks.withType<GenerateSources> {
    girFiles.set(listOf("Adw-1"))
}
