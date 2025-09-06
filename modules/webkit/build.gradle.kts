plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gtk"))
    api(project(":soup"))
}

tasks.withType<GenerateSources> {
    girFiles.set(listOf("WebKit-6.0", "WebKitWebProcessExtension-6.0", "JavaScriptCore-6.0"))
}
