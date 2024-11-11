plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":pango"))
    api(libs.cairo)
}

tasks.withType<GenerateSources> {
    namespace = "PangoCairo"
}
