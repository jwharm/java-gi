plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gmodule"))
    api(project(":gio"))
}

tasks.withType<GenerateSources> {
    namespace = "GdkPixbuf"
}
