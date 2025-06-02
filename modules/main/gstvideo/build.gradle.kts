plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gstbase"))
}

tasks.withType<GenerateSources> {
    namespace = "GstVideo"
    metadata = file("GstVideo-1.0.metadata")
}
