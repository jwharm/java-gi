plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gmodule"))
    api(project(":gobject"))
}

tasks.withType<GenerateSources> {
    namespace = "Gio"
    metadata = file("Gio-2.0.metadata")
}
