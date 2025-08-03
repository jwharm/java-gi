plugins {
    id("java-gi.library-conventions")
}

tasks.withType<GenerateSources> {
    namespace = "GLib"
    version = "2.0"
}
