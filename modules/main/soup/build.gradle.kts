plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gio"))
}

tasks.withType<GenerateSources> {
    namespace = "Soup"
    version = "3.0"
}
