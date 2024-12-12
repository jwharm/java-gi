import org.gradle.accessors.dm.LibrariesForLibs
import org.apache.tools.ant.taskdefs.condition.Os

/*
 * Common build settings for Java-GI modules:
 *
 * - Load plugins
 * - Set maven repositories
 * - Load common dependencies
 * - Set group and Java-GI version number
 * - Set JDK version
 * - Configure 'generateSources' action
 * - Set OS-specific library paths and parameters for unit tests
 * - Set common POM metadata and enable signing
 */

plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
}

repositories {
    mavenCentral()
}

// Workaround for https://github.com/gradle/gradle/issues/15383
val libs: LibrariesForLibs
    get() = the<LibrariesForLibs>()

dependencies {
    compileOnly(libs.annotations)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

group = "io.github.jwharm.javagi"
version = libs.versions.javagi.get()

java {
    toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.jdk.get())
    withJavadocJar()
    withSourcesJar()
}

// Register a build service that will parse and cache GIR files
gradle.sharedServices.registerIfAbsent("gir", GirParserService::class) {
    val girFilesLocation = rootDir.resolve(project.findProperty("girFilesLocation").toString())
    parameters.inputDirectory.set(girFilesLocation)
}

// Register the task that will generate Java sources from GIR files
val generateSources by tasks.registering(GenerateSources::class) {
    mainJavaSourcesDirectory = layout.projectDirectory.dir("src/main/java")
    outputDirectory = layout.buildDirectory.dir("generated/sources/java-gi")
}

// Add the generated sources to the main sourceSet
sourceSets["main"].java.srcDir(generateSources)

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>().configureEach {
    options {
        this as StandardJavadocDocletOptions
        addStringOption("tag", "apiNote:a:API Note:")
        addStringOption("Xdoclint:none", "-quiet")
        encoding = "UTF-8"
    }
}

tasks.withType<Test>().configureEach {
    // Don't run tests in Github action. The native libraries aren't installed.
    if (System.getenv().containsKey("CI")) {
        enabled = false
    }

    useJUnitPlatform()

    // Log standard output and error streams when running tests
    testLogging.showStandardStreams = true

    // Configure library path for macOS (Homebrew) and set MacOS-specific JVM parameter
    if (Os.isFamily(Os.FAMILY_MAC)) {
        jvmArgs("-Djava.library.path=/opt/homebrew/lib")
        jvmArgs("-XstartOnFirstThread")
    }

    // Configure library path for Arch, Fedora and Debian/Ubuntu
    else if (Os.isFamily(Os.FAMILY_UNIX)) {
        jvmArgs("-Djava.library.path=/usr/lib64:/lib64:/lib:/usr/lib:/lib/x86_64-linux-gnu")
    }

    // Configure library path for Windows (MSYS2)
    else if (Os.isFamily(Os.FAMILY_WINDOWS)) {
        jvmArgs("-Djava.library.path=C:/msys64/mingw64/bin")
    }

    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "${groupId}:${artifactId}"
                groupId = "io.github.jwharm.javagi"
                val capitalizedId = artifactId.replaceFirstChar(Char::titlecase)
                description = "Java language bindings for $capitalizedId, generated with Java-GI"
                url = "https://jwharm.github.io/java-gi/"
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
                        url = "https://github.com/jwharm"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/jwharm/java-gi.git"
                    developerConnection = "scm:git:ssh://github.com:jwharm/java-gi.git"
                    url = "http://github.com/jwharm/java-gi/tree/master"
                }
            }
        }
    }

    if (project.hasProperty("ossrhUsername") && project.hasProperty("ossrhPassword")) {
        repositories {
            maven {
                name = "OSSRH"
                val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                credentials {
                    username = project.findProperty("ossrhUsername").toString()
                    password = project.findProperty("ossrhPassword").toString()
                }
            }
        }
        signing {
            sign(publishing.publications["mavenJava"])
        }
    }
}
