plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gio"))
}

tasks.withType<GenerateSources> {
    namespace = "Soup"
    metadata = file("Soup-3.0.metadata")
}
