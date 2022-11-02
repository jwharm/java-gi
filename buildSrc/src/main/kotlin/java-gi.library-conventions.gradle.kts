plugins {
    `java-library`
}

group = "io.github.jwharm.javagi"
version = "1.0"

java {
    // Temporarily needed until gradle 7.6 is out
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(19))
    }
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:23.0.0")
}

// Temporarily needed until panama is out of preview
tasks.compileJava.get().options.compilerArgs.add("--enable-preview")
(tasks.javadoc.get().options as CoreJavadocOptions).run {
    addStringOption("source", "19")
    addBooleanOption("-enable-preview", true)
}
