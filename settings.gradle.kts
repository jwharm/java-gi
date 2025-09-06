plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "java-gi"

includeBuild("generator")

include("adw")
include("gdkpixbuf")
include("glib")
include("gobject-introspection-tests")
include("gstreamer")
include("gtk")
include("gtksourceview")
include("harfbuzz")
include("pango")
include("soup")
include("webkit")

// All child projects are located in modules/main or modules/test
for (p in rootProject.children) {
    p.projectDir = File(settingsDir, "modules/${p.name}")
}

include("ext")
