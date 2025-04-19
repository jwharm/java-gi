plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gtk"))
    api(project(":javascriptcore"))
    api(project(":soup"))
}

tasks.withType<GenerateSources> {
    namespace = "WebKit"
}
