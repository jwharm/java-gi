plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":glib"))
}

tasks.withType<GenerateSources> {
    namespace = "GObject"
    version = "2.0"
}
