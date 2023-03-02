import ext.*
import io.github.jwharm.javagi.generator.PatchSet.*

plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":glib"))
}

setupGenSources {
    moduleInfo = """
        module org.gnome.gtk {
            requires static org.jetbrains.annotations;
            requires transitive org.gnome.glib;
            exports io.github.jwharm.javagi.gtk.annotations;
            exports io.github.jwharm.javagi.gtk.util;
            %s
        }
    """.trimIndent()

    source("GLib-2.0", "org.gnome.glib", false, "glib-2.0")
    source("GObject-2.0", "org.gnome.gobject", false, "gobject-2.0")
    source("Gio-2.0", "org.gnome.gio", false, "gio-2.0")
    source("GModule-2.0", "org.gnome.gmodule", false)

    source("cairo-1.0", "org.cairographics", true, "cairo", "cairo-gobject") { repo ->
        // Incompletely defined
        removeFunction(repo, "image_surface_create")
    }
    source("freetype2-2.0", "org.freetype", true)
    source("HarfBuzz-0.0", "org.harfbuzz", true, "harfbuzz") { repo ->
        // This constant has type "language_t" which cannot be instantiated
        removeConstant(repo, "LANGUAGE_INVALID")
    }
    source("Pango-1.0", "org.pango", true, "pango-1.0")
    source("PangoCairo-1.0", "org.pango.cairo", true, "pangocairo-1.0")
    source("GdkPixbuf-2.0", "org.gnome.gdkpixbuf", true, "gdk_pixbuf-2.0")
    source("Gdk-4.0", "org.gnome.gdk", true)
    source("Graphene-1.0", "org.gnome.graphene", true, "graphene-1.0")
    source("Gsk-4.0", "org.gnome.gsk", true)
    source("Gtk-4.0", "org.gnome.gtk", true, "gtk-4") { repo ->
        // Override with different return type
        renameMethod(repo, "ApplicationWindow", "get_id", "get_window_id")
        renameMethod(repo, "MenuButton", "get_direction", "get_arrow_direction")
        renameMethod(repo, "PrintSettings", "get", "get_string")
        renameMethod(repo, "PrintUnixDialog", "get_settings", "get_print_settings")
        renameMethod(repo, "Widget", "activate", "activate_widget")

        // These calls return floating references
        setReturnFloating(findMethod(repo, "FileFilter", "to_gvariant"))
        setReturnFloating(findMethod(repo, "PageSetup", "to_gvariant"))
        setReturnFloating(findMethod(repo, "PaperSize", "to_gvariant"))
        setReturnFloating(findMethod(repo, "PrintSettings", "to_gvariant"))
    }
}

tasks.javadoc {
    linksOffline("https://jwharm.github.io/java-gi/glib", project(":glib"))
}