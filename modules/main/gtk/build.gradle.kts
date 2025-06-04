plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gdk"))
    api(project(":gsk"))
}

tasks.withType<GenerateSources> {
    namespace = "Gtk"
    metadata = file("Gtk-4.0.metadata")
}
