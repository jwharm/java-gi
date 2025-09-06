plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":glib"))
    api(project(":gdkpixbuf"))
    api(project(":harfbuzz"))
    api(project(":pango"))
    api(libs.cairo)
}

tasks.withType<GenerateSources> {
    girFiles.set(listOf("Gdk-4.0", "Graphene-1.0", "Gsk-4.0", "Gtk-4.0"))
}
