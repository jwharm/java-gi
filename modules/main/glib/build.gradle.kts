plugins {
    id("java-gi.library-conventions")
}

tasks.withType<GenerateSources> {
    namespace = "GLib"
    metadata = file("GLib-2.0.metadata")
}
