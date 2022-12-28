plugins {
    `java-library`
    `maven-publish`
}

group = "io.github.jwharm.javagi"
version = "0.3"

java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(19))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:23.0.0")
}

publishing {
    repositories {
        mavenLocal()
        if (System.getenv("GITHUB_ACTIONS") == "true") {
            maven("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY")}") {
                name = "GitHubPackages"
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            if (System.getenv("GITHUB_REF_TYPE") != "tag") version = "${project.version}-SNAPSHOT"

            from(components["java"])
        }
    }
}

// Temporarily needed until panama is out of preview
tasks.compileJava.get().options.compilerArgs.add("--enable-preview")
(tasks.javadoc.get().options as CoreJavadocOptions).run {
    addStringOption("source", "19")
    addBooleanOption("-enable-preview", true)
}
