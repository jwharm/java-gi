package ext

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.dsl.ArtifactHandler
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.*

private val Project.sourceSets: SourceSetContainer get() = extensions.getByName("sourceSets") as SourceSetContainer
private val SourceSetContainer.main get() = named<SourceSet>("main")

fun Project.flavor(flavor: String, linkMain: Boolean = true): Flavor {
    val sourceSet = sourceSets.create(flavor)
    sourceSet.java.srcDir("src/$flavor/java")
    sourceSet.resources.srcDir("src/$flavor/resources")

    if (linkMain) {
        sourceSet.compileClasspath += sourceSets.main.get().output
        sourceSet.runtimeClasspath += sourceSets.main.get().output
    }

    extensions.configure<JavaPluginExtension>("java") {
        registerFeature(flavor) {
            usingSourceSet(sourceSet)
            capability(project.group.toString(), project.name, project.version.toString())
            capability(project.group.toString(), "${project.name}-$flavor", project.version.toString())
        }
    }

    dependencies {
        add("${flavor}CompileOnly", "org.jetbrains:annotations:24.0.0")
    }

    return Flavor(
        sourceSet,
        tasks.named<JavaCompile>("compile${flavor.capitalized()}Java").get(),
        tasks.named<Jar>("${flavor}Jar").get()
    )
}

data class Flavor(val sourceSet: SourceSet, val compileTask: JavaCompile, val jarTask: Jar)