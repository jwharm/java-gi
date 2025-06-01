plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":glib"))
}

tasks.withType<GenerateSources> {
    namespace = "GObject"
    metadata = file("GObject-2.0.metadata")
}
