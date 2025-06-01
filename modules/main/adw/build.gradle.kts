plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gio"))
    api(project(":gtk"))
}

tasks.withType<GenerateSources> {
    namespace = "Adw"
    metadata = file("Adw-1.metadata")
}
