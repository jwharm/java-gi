package ext

import io.github.jwharm.javagi.JavaGI
import io.github.jwharm.javagi.JavaGI.Source
import io.github.jwharm.javagi.generator.PatchSet
import io.github.jwharm.javagi.model.Repository
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*
import java.io.File
import java.util.LinkedList

fun Project.setupGenSources(setup: Action<Model>) : TaskProvider<Task> {
    val generatedPath = buildDir.resolve("generated/sources/javagi/java/main")
    val girSourcesConvention = File(if (hasProperty("girSources")) property("girSources").toString() else "/usr/share/gir-1.0")

    extensions.configure<SourceSetContainer>("sourceSets") {
        named<SourceSet>("main") {
            java {
                srcDir(generatedPath)
            }
        }
    }

    val genSources by tasks.registering {
        val model = ModelImpl(girSourcesConvention, generatedPath)
        setup(model)
        doLast {
            val generated = JavaGI.generate(model.generatedPath.toPath(), *model.sources)
            if (model.moduleInfo != null) generated.writeModuleInfo(model.moduleInfo)
        }
    }

    tasks.named<JavaCompile>("compileJava").get().dependsOn(genSources)

    return genSources
}

interface Model {
    var sourcePath: File
    var generatedPath: File
    var moduleInfo: String?
    fun source(name: String, pkg: String, generate: Boolean, vararg natives: String, patches: ProtoPatchSet? = null)
}
typealias ProtoPatchSet = PatchSet.(repo: Repository) -> Unit

private data class ModelImpl(override var sourcePath: File, override var generatedPath: File) : Model {
    override var moduleInfo: String? = null
    override fun source(name: String, pkg: String, generate: Boolean, vararg natives: String, patches: ProtoPatchSet?) {
        protoSources.add(ProtoSource(name, pkg, generate, arrayOf(*natives), patches))
    }

    private val protoSources: MutableList<ProtoSource> = LinkedList()
    val sources: Array<Source> get() = protoSources.map {
        JavaGI.Source(
            sourcePath.toPath().resolve("${it.name}.gir"),
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