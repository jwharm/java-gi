package ext

import de.undercouch.gradle.tasks.download.Download
import io.github.jwharm.javagi.JavaGI
import io.github.jwharm.javagi.JavaGI.Source
import io.github.jwharm.javagi.generator.PatchSet
import io.github.jwharm.javagi.generator.Platform
import io.github.jwharm.javagi.model.Repository
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.dsl.ArtifactHandler
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.*
import java.io.File
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.*

private fun DependencyHandler.compileOnly(dependencyNotation: Any) = add("compileOnly", dependencyNotation)
private val Project.publishing get() = extensions.getByName("publishing") as PublishingExtension

fun DependencyHandlerScope.platformDependency(proj: String) {
    compileOnly(project(proj, "apiFlavor"))
    add("windowsImplementation", project(proj, "windowsFlavor"))
    add("linuxImplementation", project(proj, "linuxFlavor"))
    add("macosImplementation", project(proj, "macosFlavor"))
    add("apiImplementation", project(proj, "apiFlavor"))
}

fun Project.setupGenSources(setup: Action<Model>) {
    val girTask = rootProject.tasks.named("downloadGir", Download::class)
    val maven = publishing.publications.named<MavenPublication>("maven").get()

    val models = Platform.values().map { platform ->
        val generatedPath = buildDir.resolve("generated/sources/javagi/$platform")

        val model = ModelImpl(platform, generatedPath)
        setup(model)

        val flavor = flavor(platform.name)
        flavor.sourceSet.java.srcDir("src/main/java")
        flavor.sourceSet.java.srcDir(generatedPath)

        val genSources = tasks.create("${platform}GenSources") {
            dependsOn(girTask)
            doLast {
                girTask.get().dest.toPath().openZip().use { fs ->
                    val path = fs.getPath("gir-files-main").resolve(platform.name)
                    val generated = JavaGI.generate(platform, generatedPath.toPath(), *model.getSources(path))
                    if (model.moduleInfo != null) generated.writeModuleInfo(model.moduleInfo)
                }
            }
        }

        flavor.jarTask.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        flavor.compileTask.dependsOn(genSources)

        maven.artifact(flavor.jarArtifact)

        model
    }

    val generatedPath = buildDir.resolve("generated/sources/javagi/api")

    val model = ModelImpl(null, generatedPath)
    setup(model)

    val flavor = flavor("api", false)
    flavor.sourceSet.java.srcDir("src/main/java")
    flavor.sourceSet.java.srcDir(generatedPath)

    val apiGenSources by tasks.creating {
        dependsOn(girTask)
        doLast {
            girTask.get().dest.toPath().openZip().use { fs ->
                val generated = JavaGI.generateApi(generatedPath.toPath(), models.map {
                    val path = fs.getPath("gir-files-main").resolve(it.platform!!.name)
                    Pair(it.platform, it.getSources(path))
                }.toMap())
                if (model.moduleInfo != null) generated.writeModuleInfo(model.moduleInfo)
            }
        }
    }

    flavor.jarTask.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    flavor.compileTask.dependsOn(apiGenSources)

    maven.artifact(flavor.jarArtifact)

    dependencies {
        compileOnly(flavor.sourceSet.output)
    }

    val javadoc = tasks.named("javadoc", Javadoc::class).get()
    javadoc.source(generatedPath)

    val sourcesJar by tasks.registering(Jar::class) {
        archiveClassifier.set("sources")
        from(flavor.sourceSet.allSource)
    }

    val javadocJar by tasks.registering(Jar::class) {
        dependsOn(javadoc)
        archiveClassifier.set("javadoc")
        from(javadoc)
    }

    artifacts {
        maven.artifact(archives(javadocJar))
        maven.artifact(archives(sourcesJar))
    }
}

private fun Path.openZip(): FileSystem {
    val fileURI = toUri()
    return FileSystems.newFileSystem(URI("jar:" + fileURI.scheme, fileURI.path, null), HashMap<String, String>())
}

interface Model {
    val platform: Platform?
    val generatedPath: File
    var moduleInfo: String?
    fun source(name: String, pkg: String, generate: Boolean, vararg natives: String, patches: ProtoPatchSet? = null)
}
typealias ProtoPatchSet = PatchSet.(repo: Repository) -> Unit

private data class ModelImpl(override val platform: Platform?, override val generatedPath: File) : Model {
    override var moduleInfo: String? = null
    override fun source(name: String, pkg: String, generate: Boolean, vararg natives: String, patches: ProtoPatchSet?) {
        protoSources.add(ProtoSource(name, pkg, generate, arrayOf(*natives), patches))
    }

    private val protoSources: MutableList<ProtoSource> = LinkedList()
    fun getSources(sourcePath: Path): Array<Source> = protoSources.map {
        JavaGI.Source(
            sourcePath.resolve("${it.name}.gir"),
            it.pkg,
            it.generate,
            setOf(*it.natives),
            if (it.patches == null) PatchSet.EMPTY
            else WrappedPatchSet(it.patches)
        )
    }.toTypedArray()
}

private data class ProtoSource(val name: String, val pkg: String, val generate: Boolean, val natives: Array<String>, val patches: ProtoPatchSet? = null)
private class WrappedPatchSet(private val patches: ProtoPatchSet) : PatchSet() {
    override fun patch(repo: Repository) = patches(this, repo)
}