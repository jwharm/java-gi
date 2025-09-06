plugins {
    id("java-library")
}

tasks.named("build") {
    dependsOn(gradle.includedBuild("generator").task(":build"))
    dependsOn(gradle.includedBuild("generator").task(":assembleDist"))
}

tasks.named("clean") {
    dependsOn(gradle.includedBuild("generator").task(":clean"))
}

tasks.register("assembleDist") {
    dependsOn(gradle.includedBuild("generator").task(":assembleDist"))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

// Disable jar for top-level project
tasks.withType<Jar>().configureEach {
    enabled = false
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.jdk.get())
}

// Generate javadoc for all modules
tasks.withType<Javadoc>().configureEach {
    options {
        this as StandardJavadocDocletOptions
        addStringOption("tag", "apiNote:a:API Note:")
        addStringOption("Xdoclint:none", "-quiet")
        addStringOption("-add-modules", "org.jetbrains.annotations,org.freedesktop.cairo")
        encoding = "UTF-8"
    }
    exclude("**/module-info.java")

    // Include only the main modules in the javadoc
    val mainModules = subprojects.filter {
        it.plugins.hasPlugin("java-gi.library-conventions")
    }

    source(mainModules.flatMap {
        it.sourceSets["main"].allJava.srcDirs
    })

    // Exclude external dependencies from the classpath
    doFirst {
        classpath = files(mainModules.flatMap { subproject ->
            subproject.sourceSets["main"].compileClasspath.filter { file ->
                val path = file.absolutePath.replace("\\", "/")
                path.contains("/org.jetbrains/annotations/")
                        || path.contains("/io.github.jwharm.cairobindings/cairo/")
            }
        })
    }

    // Ensure all source code is generated before the Javadoc task starts
    mainModules.forEach {
        dependsOn(it.tasks.named("generateSources"))
    }
}
