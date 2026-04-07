plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":gtk"))
    api(project(":soup"))
}
