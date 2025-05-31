plugins {
    id("java-gi.test-conventions")
}

dependencies {
    api(project(":gio"))
    api(project(":utility"))
    api(libs.cairo)
}

tasks.withType<GenerateSources> {
    namespace = "RegressUnix"
}
