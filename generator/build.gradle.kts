plugins {
    application
    `java-library`
    `maven-publish`
}

application {
    mainClass.set("io.github.jwharm.javagi.JavaGI")
}

group = "io.github.jwharm.javagi"
version = "0.3"

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
            version = "${project.version}-SNAPSHOT"

            from(components["java"])
        }
    }
}
