plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":glib"))
}

tasks.withType<GenerateSources> {
    girFiles.set(listOf("Gst-1.0", "GstAudio-1.0", "GstBase-1.0", "GstPbutils-1.0", "GstVideo-1.0"))
}
