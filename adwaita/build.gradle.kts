import ext.*
import io.github.jwharm.javagi.generator.PatchSet.*

plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":glib"))
    api(project(":gtk"))
}

val pkgVersion = "pkg-config --modversion libadwaita-1".runCommand(project, "1.0")
version = "$pkgVersion-$version"

setupGenSources {
    moduleInfo = """
        module org.gnome.adwaita {
            requires static org.jetbrains.annotations;
            requires transitive org.gnome.gtk;
            %s
        }
    """.trimIndent()

    source("GLib-2.0", "org.gnome.glib", false, "glib-2.0")
    source("GObject-2.0", "org.gnome.gobject", false, "gobject-2.0")
    source("Gio-2.0", "org.gnome.gio", false, "gio-2.0")
    source("GModule-2.0", "org.gnome.gmodule", false)
    source("cairo-1.0", "org.cairographics", false, "cairo", "cairo-gobject")
    source("freetype2-2.0", "org.freetype", false)
    source("HarfBuzz-0.0", "org.harfbuzz", false, "harfbuzz")
    source("Pango-1.0", "org.pango", false, "pango-1.0")
    source("PangoCairo-1.0", "org.pango.cairo", false, "pangocairo-1.0")
    source("GdkPixbuf-2.0", "org.gnome.gdkpixbuf", false, "gdk_pixbuf-2.0")
    source("Gdk-4.0", "org.gnome.gdk", false)
    source("Graphene-1.0", "org.gnome.graphene", false, "graphene-1.0")
    source("Gsk-4.0", "org.gnome.gsk", false)
    source("Gtk-4.0", "org.gnome.gtk", false, "gtk-4")

    source("Adw-1", "org.gnome.adw", true, "adwaita-1") { repo ->
        // Override with different return type
        renameMethod(repo, "ActionRow", "activate", "activate_row")
        renameMethod(repo, "SplitButton", "get_direction", "get_arrow_direction")
    }
}

tasks.javadoc {
    linksOffline("https://jwharm.github.io/java-gi/glib", project(":glib"))
    linksOffline("https://jwharm.github.io/java-gi/gtk", project(":gtk"))
}