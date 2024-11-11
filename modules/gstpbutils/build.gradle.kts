plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gsk"))
    api(project(":gdk"))
}

tasks.withType<GenerateSources> {
    namespace = "Gtk"
}
