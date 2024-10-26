plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gio"))
    api(project(":harfbuzz"))
    api(libs.cairo)
}

tasks.withType<GenerateSources> {
    namespace = "Pango"
}
