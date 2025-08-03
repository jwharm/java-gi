plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gdkpixbuf"))
    api(project(":gio"))
    api(project(":pango"))
    api(project(":pangocairo"))
}

tasks.withType<GenerateSources> {
    namespace = "Gdk"
    version = "4.0"
}
