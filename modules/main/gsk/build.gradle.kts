plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gdk"))
    api(project(":graphene"))
}

tasks.withType<GenerateSources> {
    namespace = "Gsk"
    version = "4.0"
}
