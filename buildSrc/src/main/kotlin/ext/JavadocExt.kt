package ext

import org.gradle.api.Project
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.MinimalJavadocOptions
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.named
import java.util.LinkedList

fun Javadoc.linksOffline(url: String, project: Project) {
    if (options is StandardJavadocDocletOptions) {
        val opt = options as StandardJavadocDocletOptions
        val task = project.tasks.named<Javadoc>("javadoc")
        opt.linksOffline(url, task.get().destinationDir.toString())
        dependsOn(task)
    } else throw TypeCastException("Unexpected javadoc options type")
}