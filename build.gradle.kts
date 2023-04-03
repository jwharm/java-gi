import de.undercouch.gradle.tasks.download.Download

plugins {
    id("de.undercouch.download")
}

tasks.register("build") {
    dependsOn(gradle.includedBuild("generator").task(":build"))
}

tasks.register("clean") {
    dependsOn(gradle.includedBuild("generator").task(":clean"))
}

tasks.register("publish") {
    dependsOn(gradle.includedBuild("generator").task(":publish"))
}

tasks.register("javadoc") {
    dependsOn(gradle.includedBuild("generator").task(":javadoc"))
}

val downloadGir by tasks.registering(Download::class) {
    src("https://github.com/gircore/gir-files/archive/refs/heads/main.zip")
    dest(buildDir.resolve("gir.zip"))
}
