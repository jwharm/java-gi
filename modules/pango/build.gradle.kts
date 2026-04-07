plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":glib"))
    api(project(":harfbuzz"))
    api(libs.cairo)
}
