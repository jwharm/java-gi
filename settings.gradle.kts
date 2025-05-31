plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
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

include("gimarshallingtests")
include("regress")
include("regressunix")
include("utility")
include("warnlib")

// All child projects are located in modules/main or modules/test
for (p in rootProject.children) {
    p.projectDir = listOf(
        File(settingsDir, "modules/main/${p.name}"),
        File(settingsDir, "modules/test/${p.name}")
    ).firstOrNull { it.exists() }!!
}

include("ext")
