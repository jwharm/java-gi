plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":glib"))
    api(project(":gmodule"))
    api(project(":gobject"))
}

tasks.withType<GenerateSources> {
    namespace = "Gst"
    version = "1.0"
}
