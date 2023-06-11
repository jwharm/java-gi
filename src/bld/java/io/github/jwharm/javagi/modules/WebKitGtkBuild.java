package io.github.jwharm.javagi.modules;

import io.github.jwharm.javagi.AbstractProject;
import io.github.jwharm.javagi.JavaGIBuild;
import io.github.jwharm.javagi.patches.*;

import java.io.File;
import java.util.Set;

public class WebKitGtkBuild extends AbstractProject {

    private static final String MODULE_INFO = """
        module org.gnome.webkitgtk {
            requires static org.jetbrains.annotations;
            requires transitive org.gnome.gtk;
            %s
        }
        """;

    public WebKitGtkBuild(JavaGIBuild bld) {
        super(bld, "webkitgtk");
        version = version(2,41).withQualifier(bld.version().toString());

        generateSourcesOperation()
            .source("GLib-2.0.gir", "org.gnome.glib", "https://docs.gtk.org/glib/", false, Set.of("glib-2.0"), new GLibPatch())
            .source("GObject-2.0.gir", "org.gnome.gobject", "https://docs.gtk.org/gobject/", false, Set.of("gobject-2.0"), new GObjectPatch())
            .source("Gio-2.0.gir", "org.gnome.gio", "https://docs.gtk.org/gio/", false, Set.of("gio-2.0"), new GioPatch())
            .source("GModule-2.0.gir", "org.gnome.gmodule", null, false, null, null)

            .source("cairo-1.0.gir", "org.freedesktop.cairo", null, false, Set.of("cairo", "cairo-gobject"), new CairoPatch())
            .source("freetype2-2.0.gir", "org.freedesktop.freetype", null, false, null, null)
            .source("HarfBuzz-0.0.gir", "org.freedesktop.harfbuzz", null, false, Set.of("harfbuzz"), new HarfBuzzPatch())
            .source("Pango-1.0.gir", "org.gnome.pango", "https://docs.gtk.org/Pango/", false, Set.of("pango-1.0"), null)
            .source("PangoCairo-1.0.gir", "org.gnome.pango.cairo", "https://docs.gtk.org/Pango/", false, Set.of("pangocairo-1.0"), null)
            .source("GdkPixbuf-2.0.gir", "org.gnome.gdkpixbuf", "https://docs.gtk.org/gdk-pixbuf/", false, Set.of("gdk_pixbuf-2.0"), null)
            .source("Gdk-4.0.gir", "org.gnome.gdk", "https://docs.gtk.org/gdk4/", false, null, null)
            .source("Graphene-1.0.gir", "org.gnome.graphene", "https://developer-old.gnome.org/graphene/stable/", false, Set.of("graphene-1.0"), null)
            .source("Gsk-4.0.gir", "org.gnome.gsk", null, false, null, null)
            .source("Gtk-4.0.gir", "org.gnome.gtk", "https://docs.gtk.org/gtk4/", false, Set.of("gtk-4"), new GtkPatch())

            .source("Soup-3.0.gir", "org.gnome.soup", "https://libsoup.org/libsoup-3.0/", true, Set.of("soup-3.0"), null)
            .source("JavaScriptCore-6.0.gir", "org.gnome.webkit.jsc", "https://webkitgtk.org/reference/jsc-glib/stable/", true, Set.of("javascriptcoregtk-6.0"), null)
            .source("WebKitWebProcessExtension-6.0.gir", "org.gnome.webkit.wpe", "https://webkitgtk.org/reference/webkit2gtk-web-extension/stable/", true, null, null)
            .source("WebKit-6.0.gir", "org.gnome.webkit", "https://webkitgtk.org/reference/webkit2gtk/stable/", true, Set.of("webkitgtk-6.0"), new WebkitGtkPatch())

            .moduleInfo(MODULE_INFO);

        javadocOperation().javadocOptions()
            .linkOffline("https://jwharm.github.io/java-gi/glib", new File(bld.buildJavadocDirectory(), "glib").getAbsolutePath())
            .linkOffline("https://jwharm.github.io/java-gi/gtk", new File(bld.buildJavadocDirectory(), "gtk").getAbsolutePath());
    }
}
