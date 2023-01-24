plugins {
    id("java-gi.library-conventions")
    application
}

application {
    mainClass.set("io.github.jwharm.javagi.example.HelloWorld")
    mainModule.set("io.github.jwharm.javagi.example")
}

dependencies {
    implementation(project(":glib"))
    implementation(project(":gtk4"))
    implementation(project(":gstreamer"))
}

// Temporarily needed until panama is out of preview
tasks.run.configure {
    jvmArgs(
        "--enable-preview",
        "--enable-native-access=org.glib",
        "--enable-native-access=org.gtk",
        "--enable-native-access=org.gstreamer",
        "--enable-native-access=io.github.jwharm.javagi.example"
    )
}