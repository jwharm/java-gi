import ext.*
import io.github.jwharm.javagi.generator.PatchSet.*

plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":glib"))
    api(project(":gtk4"))
}

setupGenSources {
    moduleInfo = """
        module org.gnome.adwaita {
            requires static org.jetbrains.annotations;
            requires transitive org.gtk;
            %s
        }
    """.trimIndent()

    source("GLib-2.0", "org.gtk.glib", false, "glib-2.0")
    source("GObject-2.0", "org.gtk.gobject", false, "gobject-2.0")
    source("Gio-2.0", "org.gtk.gio", false, "gio-2.0")
    source("GModule-2.0", "org.gtk.gmodule", false)
    source("cairo-1.0", "org.cairographics", false, "cairo", "cairo-gobject")
    source("freetype2-2.0", "org.freetype", false)
    source("HarfBuzz-0.0", "org.harfbuzz", false, "harfbuzz")
    source("Pango-1.0", "org.pango", false, "pango-1.0")
    source("PangoCairo-1.0", "org.pango.cairo", false, "pangocairo-1.0")
    source("GdkPixbuf-2.0", "org.gtk.gdkpixbuf", false, "gdk_pixbuf-2.0")
    source("Gdk-4.0", "org.gtk.gdk", false)
    source("Graphene-1.0", "org.gtk.graphene", false, "graphene-1.0")
    source("Gsk-4.0", "org.gtk.gsk", false)
    source("Gtk-4.0", "org.gtk.gtk", false, "gtk-4")

    source("Adw-1", "org.gnome.adw", true, "adwaita-1") { repo ->
        // Override with different return type
        renameMethod(repo, "ActionRow", "activate", "activate_row")
        renameMethod(repo, "SplitButton", "get_direction", "get_arrow_direction")
    }
}

tasks.javadoc {
    linksOffline("https://jwharm.github.io/java-gi/glib", project(":glib"))
    linksOffline("https://jwharm.github.io/java-gi/gtk4", project(":gtk4"))
}