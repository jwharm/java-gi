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
    parameters.inputDirectories.from(project(":ext").projectDir.resolve("gir-files"))
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

    val ext = project(":ext")
    val mesonBuildDir = ext.layout.buildDirectory.dir("meson").get().asFile.absolutePath
    dependsOn(ext.tasks.named("mesonBuild"))

    // Configure library path for macOS (Homebrew) and set MacOS-specific JVM parameter
    if (Os.isFamily(Os.FAMILY_MAC)) {
        jvmArgs("-Djava.library.path=$mesonBuildDir:/opt/homebrew/lib")
        jvmArgs("-XstartOnFirstThread")
    }

    // Configure library path for Arch, Fedora and Debian/Ubuntu
    else if (Os.isFamily(Os.FAMILY_UNIX)) {
        jvmArgs("-Djava.library.path=$mesonBuildDir:/usr/lib64:/lib64:/lib:/usr/lib:/lib/x86_64-linux-gnu")
    }

    // Configure library path for Windows (MSYS2)
    else if (Os.isFamily(Os.FAMILY_WINDOWS)) {
        jvmArgs("-Djava.library.path=$mesonBuildDir;C:/msys64/mingw64/bin")
    }

    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
