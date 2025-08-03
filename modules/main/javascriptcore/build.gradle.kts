plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gobject"))
}

tasks.withType<GenerateSources> {
    namespace = "JavaScriptCore"
    version = "6.0"
}
