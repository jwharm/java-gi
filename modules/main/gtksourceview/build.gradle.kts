plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gtk"))
}

tasks.withType<GenerateSources> {
    namespace = "GtkSource"
    version = "5"
}
