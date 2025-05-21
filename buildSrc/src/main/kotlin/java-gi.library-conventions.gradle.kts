import org.apache.tools.ant.taskdefs.condition.Os
import com.vanniktech.maven.publish.SonatypeHost

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
    id("com.vanniktech.maven.publish")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.annotations)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

group = "io.github.jwharm.javagi"
version = libs.versions.javagi.get()

java {
    toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.jdk.get())
}

// Register a build service that will parse and cache GIR files
gradle.sharedServices.registerIfAbsent("gir", GirParserService::class) {
    val girFilesLocation = rootDir.resolve(project.findProperty("girFilesLocation").toString())
    parameters.inputDirectories.from(girFilesLocation)
}

// For each subproject: Get the build service
@Suppress("UNCHECKED_CAST") // cast is safe, we know what type it is
val girServiceRegistration = gradle.sharedServices.registrations.named("gir").get()
        as BuildServiceRegistration<*, GirParserService.Params>

// For each subproject: Add the '/gir' folder to the build service's inputDirectories
val girDir = layout.projectDirectory.dir("gir")
if (girDir.asFile.exists()) {
    girServiceRegistration.parameters.inputDirectories.from(girDir)
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
        jvmArgs("-Djava.library.path=lib:/opt/homebrew/lib")
        jvmArgs("-XstartOnFirstThread")
    }

    // Configure library path for Arch, Fedora and Debian/Ubuntu
    else if (Os.isFamily(Os.FAMILY_UNIX)) {
        jvmArgs("-Djava.library.path=lib:/usr/lib64:/lib64:/lib:/usr/lib:/lib/x86_64-linux-gnu")
    }

    // Configure library path for Windows (MSYS2)
    else if (Os.isFamily(Os.FAMILY_WINDOWS)) {
        jvmArgs("-Djava.library.path=lib;C:/msys64/mingw64/bin")
    }

    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

// Check if the module is located under "modules/test"
val isTestModule = project.projectDir.toPath().startsWith(rootDir.toPath().resolve("modules/test"))

if (!isTestModule) {
    mavenPublishing {
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
        signAllPublications()
        coordinates("io.github.jwharm.javagi", project.name, project.version.toString())
        pom {
            val capitalizedId = project.name.replaceFirstChar(Char::titlecase)
            name = capitalizedId
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
