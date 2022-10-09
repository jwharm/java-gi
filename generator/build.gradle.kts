plugins {
    application
    `java-library`
}

application {
    mainClass.set("io.github.jwharm.javagi.JavaGI")
}

repositories {
    mavenCentral()
}

dependencies {
}

group = "io.github.jwharm.javagi"
version = "1.0"