package ext

import org.gradle.api.Project
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.MinimalJavadocOptions
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.named

fun MinimalJavadocOptions.linksOffline(url: String, project: Project) {
    if (this is StandardJavadocDocletOptions) {
        linksOffline(url, project.tasks.named<Javadoc>("javadoc").get().destinationDir.toString())
    } else throw TypeCastException("Unexpected javadoc options type")
}