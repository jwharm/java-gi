package ext

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.dsl.ArtifactHandler
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.*

private val Project.sourceSets: SourceSetContainer get() = extensions.getByName("sourceSets") as SourceSetContainer
private fun Project.sourceSets(configure: Action<SourceSetContainer>) = extensions.configure("sourceSets", configure)
private val SourceSetContainer.main get() = named<SourceSet>("main")
private val SourceSetContainer.test get() = named<SourceSet>("test")
private val TaskContainer.check get() = named<DefaultTask>("check")
fun ArtifactHandler.archives(artifactNotation: Any): PublishArtifact = add("archives", artifactNotation)

fun Project.flavor(flavor: String, linkMain: Boolean = true): Flavor {
    val sourceSet = sourceSets.create(flavor)
    sourceSet.java.srcDir("src/$flavor/java")
    sourceSet.resources.srcDir("src/$flavor/resources")

    val testSourceSet = sourceSets.create("${flavor}Test")
    testSourceSet.java.srcDir("src/${flavor}Test/java")
    testSourceSet.resources.srcDir("src/${flavor}Test/resources")
    testSourceSet.compileClasspath += sourceSet.output
    testSourceSet.runtimeClasspath += sourceSet.output

    if (linkMain) {
        sourceSet.compileClasspath += sourceSets.main.get().output
        sourceSet.runtimeClasspath += sourceSets.main.get().output

        testSourceSet.compileClasspath += sourceSets.test.get().output
        testSourceSet.runtimeClasspath += sourceSets.test.get().output
    }

    arrayOf("implementation", "runtimeOnly").forEach { suffix ->
        val config = configurations.getByName("$flavor${suffix.capitalized()}")
        val testConfig = configurations.getByName("${flavor}Test${suffix.capitalized()}")

        config.extendsFrom(configurations.getByName(suffix))
        testConfig.extendsFrom(configurations.getByName("test${suffix.capitalized()}"))
        testConfig.extendsFrom(config)
    }

    dependencies {
        add("${flavor}CompileOnly", "org.jetbrains:annotations:24.0.0")
    }

    val testTask = tasks.create("${flavor}Test", Test::class) {
        group = JavaBasePlugin.VERIFICATION_GROUP
        description = "Runs the tests for ${flavor}."
        testClassesDirs = testSourceSet.output.classesDirs
        classpath = testSourceSet.runtimeClasspath
    }
    tasks.check { dependsOn(testTask) }

    val jarTask = tasks.create("${flavor}Jar", Jar::class) {
        group = BasePlugin.BUILD_GROUP
        description = "Assembles a jar archive containing the $flavor classes combined with the main classes."
        from(sourceSet.output)
        from(sourceSets.main.get().output)
        archiveClassifier.set(flavor)
    }

    configurations.create("${flavor}Flavor") {
        isCanBeConsumed = true
        isCanBeResolved = false
        extendsFrom(configurations.named("implementation").get(), configurations.named("runtimeOnly").get())
    }

    var jarArtifact: PublishArtifact? = null
    artifacts {
        jarArtifact = archives(jarTask)
        add("${flavor}Flavor", jarTask)
    }

    return Flavor(sourceSet, jarTask, tasks.named<JavaCompile>("compile${flavor.capitalized()}Java").get(), jarArtifact!!)
}

data class Flavor(val sourceSet: SourceSet, val jarTask: Jar, val compileTask: JavaCompile, val jarArtifact: PublishArtifact)