plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gst"))
}

tasks.withType<GenerateSources> {
    namespace = "GstBase"
    version = "1.0"
}
