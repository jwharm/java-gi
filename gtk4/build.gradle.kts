import io.github.jwharm.javagi.JavaGI

plugins {
    `java-library`
}

buildscript {
    dependencies {
        classpath("io.github.jwharm.javagi:generator:1.0")
    }
}

java {
    // Temporarily needed until gradle 7.6 is out
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(19))
    }
    // Temporarily disabled since the generated docs were apparently invalid
//    withJavadocJar()
}

group = "io.github.jwharm.javagi"
version = "gtk4"

repositories {
    mavenCentral()
}

dependencies {
}

val generatedPath = buildDir.resolve("generated/sources/javagi/java/main")

sourceSets {
    main {
        java {
            srcDir(generatedPath)
        }
    }
}

val genSources by tasks.registering {
    doLast {
        JavaGI.run(projectDir.resolve("input.xml").absolutePath, generatedPath.absolutePath)
    }
}

tasks.compileJava.get().dependsOn(genSources.get())

// Temporarily needed until panama is out of preview
tasks.compileJava.get().options.compilerArgs.add("--enable-preview")
//(tasks.javadoc.get().options as CoreJavadocOptions).run {
//    addStringOption("source", "19")
//    addBooleanOption("-enable-preview", true)
//}