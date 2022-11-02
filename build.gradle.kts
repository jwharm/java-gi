tasks.register("build") {
    dependsOn(gradle.includedBuild("generator").task(":build"))
}

tasks.register("clean") {
    dependsOn(gradle.includedBuild("generator").task(":clean"))
}

tasks.register("example") {
    dependsOn(project(":example").task(":run"))
}