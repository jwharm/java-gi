package ext

import org.gradle.api.Project
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.MinimalJavadocOptions
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.named

fun Javadoc.linksOffline(url: String, project: Project) {
    if (options is StandardJavadocDocletOptions) {
        val task = project.tasks.named<Javadoc>("javadoc").get()
        (options as StandardJavadocDocletOptions).linksOffline(url, task.destinationDir.toString())
        dependsOn(task)
    } else throw TypeCastException("Unexpected javadoc options type")
}