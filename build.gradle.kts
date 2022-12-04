tasks.register("build") {
    dependsOn(gradle.includedBuild("generator").task(":build"))
}

tasks.register("clean") {
    dependsOn(gradle.includedBuild("generator").task(":clean"))
}

tasks.register("publish") {
    dependsOn(gradle.includedBuild("generator").task(":publish"))
}

tasks.register("example") {
    dependsOn(project(":example").tasks["run"])
}
