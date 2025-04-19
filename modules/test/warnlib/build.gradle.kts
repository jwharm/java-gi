plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gio"))
}

tasks.withType<GenerateSources> {
    namespace = "WarnLib"
}
