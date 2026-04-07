plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":glib"))
    api(libs.cairo)
}
