plugins {
    id("java-gi.test-conventions")
}

dependencies {
    api(project(":glib"))
    api(libs.cairo)
}

tasks.withType<GenerateSources> {
    girFiles.set(listOf("GIMarshallingTests-1.0", "Regress-1.0", "Utility-1.0", "WarnLib-1.0"))
}
