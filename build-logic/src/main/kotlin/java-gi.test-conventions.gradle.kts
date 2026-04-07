plugins {
    id("java-gi.module")
}

afterEvaluate {
    tasks.withType<GenerateSources>().configureEach {
        dependsOn(project(":ext").tasks.named("mesonBuild"))
    }
}
