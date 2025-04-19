plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gio"))
    api(project(":utility"))
    api(libs.cairo)
}

tasks.withType<GenerateSources> {
    namespace = "Regress"
}
