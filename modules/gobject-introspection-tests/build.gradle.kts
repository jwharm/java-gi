plugins {
    id("java-gi.test-conventions")
}

dependencies {
    api(project(":glib"))
    api(libs.cairo)
}
