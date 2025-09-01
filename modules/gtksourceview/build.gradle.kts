plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gtk"))
}

tasks.withType<GenerateSources> {
    girFiles.set(listOf("GtkSource-5"))
}
