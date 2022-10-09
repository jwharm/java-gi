plugins {
    application
    `java-library`
}

application {
    mainClass.set("io.github.jwharm.javagi.example.HelloWorld")
}

java {
    // Temporarily needed until gradle 7.6 is out
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(19))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.jwharm.javagi:gtk4:1.0")
}

group = "io.github.jwharm.javagi"
version = "1.0"

// Temporarily needed until panama is out of preview
tasks.compileJava.get().options.compilerArgs.add("--enable-preview")
tasks.run.get().jvmArgs!!.add("--enable-preview")