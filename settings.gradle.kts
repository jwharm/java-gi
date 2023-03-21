rootProject.name = "java-gi"

includeBuild("generator")
include("glib")
include("gtk")
include("adwaita")
include("gstreamer")
include("example")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")