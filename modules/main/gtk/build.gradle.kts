plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gdk"))
    api(project(":gsk"))
}

tasks.withType<GenerateSources> {
    namespace = "Gtk"
    version = "4.0"
}
