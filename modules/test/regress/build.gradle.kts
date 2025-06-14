plugins {
    id("java-gi.test-conventions")
}

dependencies {
    api(project(":gio"))
    api(project(":utility"))
    api(libs.cairo)
}

tasks.withType<GenerateSources> {
    namespace = "Regress"
    metadata = file("Regress-1.0.metadata")
}
