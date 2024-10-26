plugins {
    `java-library`
}

tasks.named("build") {
    dependsOn(gradle.includedBuild("generator").task(":build"))
}

tasks.named("clean") {
    dependsOn(gradle.includedBuild("generator").task(":clean"))
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
    source(subprojects.flatMap { it.sourceSets["main"].allJava.srcDirs })

    // Exclude external dependencies from the classpath
    classpath = files(subprojects.flatMap { subproject ->
        subproject.sourceSets["main"].compileClasspath.filter { file ->
            val path = file.absolutePath
            path.contains("/org.jetbrains/annotations/")
                || path.contains("/io.github.jwharm.cairobindings/cairo/")
                || path.contains("\\org.jetbrains\\annotations\\")
                || path.contains("\\io.github.jwharm.cairobindings\\cairo\\")
        }
    })

    // Ensure all source code is generated before the Javadoc task starts
    subprojects.forEach {
        dependsOn(it.tasks.named("generateSources"))
    }
}
