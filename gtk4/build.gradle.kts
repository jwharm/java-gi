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
        module org.gtk {
            requires static org.jetbrains.annotations;
            requires transitive org.glib;
            %s
        }
    """.trimIndent()

    source("GLib-2.0", "org.gtk.glib", false, "glib-2.0") { repo ->
        // This method has parameters that jextract does not support
        removeFunction(repo, "assertion_message_cmpnum")
        // Incompletely defined
        removeFunction(repo, "clear_error")
    }
    source("GObject-2.0", "org.gtk.gobject", false, "gobject-2.0") { repo ->
        // This is an alias for Callback type
        removeType(repo, "VaClosureMarshal")
        removeType(repo, "SignalCVaMarshaller")
        removeFunction(repo, "signal_set_va_marshaller")
        // Override with different return type
        renameMethod(repo, "TypeModule", "use", "use_type_module")
        // These functions have two Callback parameters, this isn't supported yet
        removeFunction(repo, "signal_new_valist")
        removeFunction(repo, "signal_newv")
        removeFunction(repo, "signal_new")
        removeFunction(repo, "signal_new_class_handler")
    }
    source("Gio-2.0", "org.gtk.gio", false, "gio-2.0") { repo ->
        // Override with different return type
        renameMethod(repo, "BufferedInputStream", "read_byte", "read_int")
        // g_async_initable_new_finish is a method declaration in the interface AsyncInitable.
        // It is meant to be implemented as a constructor (actually, a static factory method).
        // However, Java does not allow a (non-static) method to be implemented/overridden by a static method.
        // The current solution is to remove the method from the interface. It is still available in the implementing classes.
        removeMethod(repo, "AsyncInitable", "new_finish")
    }
    source("GModule-2.0", "org.gtk.gmodule", false)
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
    source("GdkPixbuf-2.0", "org.gtk.gdkpixbuf", true, "gdk_pixbuf-2.0")
    source("Gdk-4.0", "org.gtk.gdk", true)
    source("Graphene-1.0", "org.gtk.graphene", true, "graphene-1.0")
    source("Gsk-4.0", "org.gtk.gsk", true)
    source("Gtk-4.0", "org.gtk.gtk", true, "gtk-4") { repo ->
        // Override with different return type
        renameMethod(repo, "MenuButton", "get_direction", "get_arrow_direction")
        renameMethod(repo, "PrintUnixDialog", "get_settings", "get_print_settings")
        renameMethod(repo, "PrintSettings", "get", "get_string")

        // This method returns void in interface ActionGroup, but returns boolean in class Widget.
        // Subclasses from Widget that implement ActionGroup throw a compile error.
        setReturnVoid(repo, "Widget", "activate_action")

        // These calls return floating references
        setReturnFloating(findMethod(repo, "FileFilter", "to_gvariant"))
        setReturnFloating(findMethod(repo, "PageSetup", "to_gvariant"))
        setReturnFloating(findMethod(repo, "PaperSize", "to_gvariant"))
        setReturnFloating(findMethod(repo, "PrintSettings", "to_gvariant"))
    }
    source("Adw-1", "org.gnome.adw", true, "adwaita-1") { repo ->
        // Override with different return type
        renameMethod(repo, "ActionRow", "activate", "activate_row")
        renameMethod(repo, "SplitButton", "get_direction", "get_arrow_direction")
    }
}

tasks.javadoc {
    options.linksOffline("https://jwharm.github.io/java-gi/glib", project(":glib"))
}