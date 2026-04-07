plugins {
    id("java-gi.module")
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("org.java-gi", project.name, project.version.toString())
    pom {
        val capitalizedId = project.name.replaceFirstChar(Char::titlecase)
        name = capitalizedId
        description = "Java language bindings for $capitalizedId, generated with Java-GI"
        url = "https://java-gi.org/"
        licenses {
            license {
                name = "GNU Lesser General Public License, version 2.1"
                url = "https://www.gnu.org/licenses/lgpl-2.1.txt"
            }
        }
        developers {
            developer {
                id = "jwharm"
                name = "Jan-Willem Harmannij"
                email = "jwharmannij@gmail.com"
                url = "https://codeberg.org/jwharm"
            }
        }
        scm {
            connection = "scm:git:git://codeberg.org/java-gi/java-gi.git"
            developerConnection = "scm:git:ssh://codeberg.org:java-gi/java-gi.git"
            url = "https://codeberg.org/java-gi/java-gi"
        }
    }
}
