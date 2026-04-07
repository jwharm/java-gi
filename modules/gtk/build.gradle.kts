plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":glib"))
    api(project(":gdkpixbuf"))
    api(project(":harfbuzz"))
    api(project(":pango"))
    api(libs.cairo)
}
