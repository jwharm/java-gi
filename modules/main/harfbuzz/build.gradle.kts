plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gobject"))
    api(libs.cairo)
}

tasks.withType<GenerateSources> {
    namespace = "HarfBuzz"
}
