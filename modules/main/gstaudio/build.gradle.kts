plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gstbase"))
}

tasks.withType<GenerateSources> {
    namespace = "GstAudio"
}
