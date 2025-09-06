plugins {
    id("java-gi.library-conventions")
}

tasks.withType<GenerateSources> {
    girFiles.set(listOf("GLib-2.0", "GObject-2.0", "GModule-2.0", "Gio-2.0"))
}
