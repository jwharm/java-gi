plugins {
    id("java-gi.test-conventions")
}

dependencies {
    api(project(":gobject"))
}

tasks.withType<GenerateSources> {
    namespace = "Utility"
    version = "1.0"
}
