plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gst"))
    api(project(":gstaudio"))
    api(project(":gstbase"))
    api(project(":gstvideo"))
}

tasks.withType<GenerateSources> {
    namespace = "GstPbutils"
    version = "1.0"
}
