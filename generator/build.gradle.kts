plugins {
    application
    `java-library`
    `maven-publish`
}

java {
    withJavadocJar()
    withSourcesJar()
}

application {
    mainClass.set("io.github.jwharm.javagi.JavaGI")
}

group = "io.github.jwharm.javagi"
version = "0.4"

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
