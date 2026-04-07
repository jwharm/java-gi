plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":glib"))
    api(project(":gdkpixbuf"))
    api(libs.cairo)
}
