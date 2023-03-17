plugins {
    java
    application
}

val flavor = System.getProperty("os.name").toLowerCase().let { name ->
    when {
        name.contains("nux") -> "linux"
        name.contains("windows") -> "windows"
        name.contains("mac") || name.contains("darwin") -> "macos"
        else -> throw Error("Unrecognized or unsupported platform")
    }
} + "Flavor" // Because we use the project directly

application {
    mainClass.set("io.github.jwharm.javagi.example.HelloWorld")
    mainModule.set("io.github.jwharm.javagi.example")
}

dependencies {
    implementation(project(":glib", flavor))
    implementation(project(":gtk", flavor))
    implementation(project(":gstreamer", flavor))
}

tasks.compileJava.configure { options.compilerArgs.add("--enable-preview") }

// Temporarily needed until panama is out of preview
tasks.run.configure {
    jvmArgs(
        "--enable-preview",
        "--enable-native-access=org.gnome.glib",
        "--enable-native-access=org.gnome.gtk",
        "--enable-native-access=org.freedesktop.gstreamer",
        "--enable-native-access=io.github.jwharm.javagi.example"
    )
}