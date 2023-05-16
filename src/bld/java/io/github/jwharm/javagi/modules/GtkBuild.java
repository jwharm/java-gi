package io.github.jwharm.javagi.modules;

import io.github.jwharm.javagi.JavaGIBuild;
import io.github.jwharm.javagi.AbstractProject;
import io.github.jwharm.javagi.patches.*;

import java.io.File;
import java.util.Set;

public class GtkBuild extends AbstractProject {

    private static final String MODULE_INFO = """
        module org.gnome.gtk {
            requires static org.jetbrains.annotations;
            requires transitive org.gnome.glib;
            exports io.github.jwharm.javagi.gtk.annotations;
            exports io.github.jwharm.javagi.gtk.types;
            exports io.github.jwharm.javagi.gtk.util;
            %s
        }
        """;

    public GtkBuild(JavaGIBuild bld) {
        super(bld, "gtk");
        version = version(4, 10).withQualifier(bld.version().toString());
        srcDirectory = new File(workDirectory(), name);

        generateSourcesOperation()
            .source("GLib-2.0.gir", "org.gnome.glib", false, Set.of("glib-2.0"), new GLibPatch())
            .source("GObject-2.0.gir", "org.gnome.gobject", false, Set.of("gobject-2.0"), new GObjectPatch())
            .source("Gio-2.0.gir", "org.gnome.gio", false, Set.of("gio-2.0"), new GioPatch())
            .source("GModule-2.0.gir", "org.gnome.gmodule", false, null, null)

            .source("cairo-1.0.gir", "org.cairographics", true, Set.of("cairo", "cairo-gobject"), new CairoPatch())
            .source("freetype2-2.0.gir", "org.freetype", true, null, null)
            .source("HarfBuzz-0.0.gir", "org.harfbuzz", true, Set.of("harfbuzz"), new HarfBuzzPatch())
            .source("Pango-1.0.gir", "org.pango", true, Set.of("pango-1.0"), null)
            .source("PangoCairo-1.0.gir", "org.pango.cairo", true, Set.of("pangocairo-1.0"), null)
            .source("GdkPixbuf-2.0.gir", "org.gnome.gdkpixbuf", true, Set.of("gdk_pixbuf-2.0"), null)
            .source("Gdk-4.0.gir", "org.gnome.gdk", true, null, null)
            .source("Graphene-1.0.gir", "org.gnome.graphene", true, Set.of("graphene-1.0"), null)
            .source("Gsk-4.0.gir", "org.gnome.gsk", true, null, null)
            .source("Gtk-4.0.gir", "org.gnome.gtk", true, Set.of("gtk-4"), new GtkPatch())

            .moduleInfo(MODULE_INFO);

        javadocOperation().javadocOptions()
            .linkOffline("https://jwharm.github.io/java-gi/glib", new File(bld.buildJavadocDirectory(), "glib").getAbsolutePath());
    }
}
