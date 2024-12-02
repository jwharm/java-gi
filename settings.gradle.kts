plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "java-gi"

includeBuild("generator")
include ("adw")
include("gdk")
include("gdkpixbuf")
include("gio")
include("glib")
include("gmodule")
include("gobject")
include("graphene")
include("gsk")
include("gst")
include("gstaudio")
include("gstbase")
include("gstpbutils")
include("gstvideo")
include("gtk")
include("gtksourceview")
include("harfbuzz")
include("javascriptcore")
include("pango")
include("pangocairo")
include("soup")
include("webkit")
include("webkitwebprocessextension")

// All child projects are located in the modules/ directory
for (p in rootProject.children) {
    val dir = File(settingsDir, "modules/${p.name}")
    if (dir.exists())
        p.projectDir = dir
}
