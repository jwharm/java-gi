plugins {
    id("java-gi.test-conventions")
}

dependencies {
    api(project(":gio"))
}

tasks.withType<GenerateSources> {
    namespace = "GIMarshallingTests"
    metadata = file("GIMarshallingTests-1.0.metadata")
}
