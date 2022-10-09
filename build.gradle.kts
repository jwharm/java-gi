tasks.register("build") {
    dependsOn(gradle.includedBuild("generator").task(":build"))
    dependsOn(gradle.includedBuild("gtk4").task(":build"))
}

tasks.register("clean") {
    dependsOn(gradle.includedBuild("generator").task(":clean"))
    dependsOn(gradle.includedBuild("gtk4").task(":clean"))
}