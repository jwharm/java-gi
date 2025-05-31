plugins {
    id("java-gi.module")
}

with(gradle.sharedServices.registrations["gir"].service.get() as GirParserService) {
    parameters.inputDirectories.from(project(":ext")
        .layout.buildDirectory
        .dir("testgir")
    )
}

afterEvaluate {
    tasks.withType<GenerateSources>().configureEach {
        dependsOn(project(":ext").tasks.named("buildGir"))
    }
}
