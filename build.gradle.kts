tasks.register("build") {
    dependsOn(gradle.includedBuild("generator").task(":build"))
    dependsOn(gradle.includedBuild("gtk4").task(":build"))
    dependsOn(gradle.includedBuild("example").task(":build"))
}

tasks.register("clean") {
    dependsOn(gradle.includedBuild("generator").task(":clean"))
    dependsOn(gradle.includedBuild("gtk4").task(":clean"))
    dependsOn(gradle.includedBuild("example").task(":clean"))
}

tasks.register("example") {
    dependsOn(gradle.includedBuild("example").task(":run"))
}