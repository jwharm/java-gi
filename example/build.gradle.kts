plugins {
    id("java-gi.library-conventions")
    application
}

application {
    mainClass.set("io.github.jwharm.javagi.example.HelloWorld")
}

dependencies {
    implementation(project(":glib"))
    implementation(project(":gtk4"))
    implementation(project(":gstreamer"))
}

// Temporarily needed until panama is out of preview
tasks.run.get().jvmArgs!!.add("--enable-preview")