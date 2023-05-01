package io.github.jwharm.javagi;

import io.github.jwharm.javagi.patches.*;

import java.io.File;
import java.util.Set;

public class AdwaitaBuild extends JavaGIProject {

    private static final String MODULE_INFO = """
        module org.gnome.adwaita {
            requires static org.jetbrains.annotations;
            requires transitive org.gnome.gtk;
            %s
        }
    """;

    public AdwaitaBuild(JavaGIBuild bld) {
        super(bld, "adwaita");

        pkg = "org.gnome.adwaita";
        name = "adwaita";
        version = version(1, 3).withQualifier(bld.version().toString());

        generateSourcesOperation()
            .source("GLib-2.0.gir", "org.gnome.glib", false, Set.of("glib-2.0"), new GLibPatch())
            .source("GObject-2.0.gir", "org.gnome.gobject", false, Set.of("gobject-2.0"), new GObjectPatch())
            .source("Gio-2.0.gir", "org.gnome.gio", false, Set.of("gio-2.0"), new GioPatch())
            .source("GModule-2.0.gir", "org.gnome.gmodule", false, null, null)

            .source("cairo-1.0.gir", "org.cairographics", false, Set.of("cairo", "cairo-gobject"), new CairoPatch())
            .source("freetype2-2.0.gir", "org.freetype", false, null, null)
            .source("HarfBuzz-0.0.gir", "org.harfbuzz", false, Set.of("harfbuzz"), new HarfBuzzPatch())
            .source("Pango-1.0.gir", "org.pango", false, Set.of("pango-1.0"), null)
            .source("PangoCairo-1.0.gir", "org.pango.cairo", false, Set.of("pangocairo-1.0"), null)
            .source("GdkPixbuf-2.0.gir", "org.gnome.gdkpixbuf", false, Set.of("gdk_pixbuf-2.0"), null)
            .source("Gdk-4.0.gir", "org.gnome.gdk", false, null, null)
            .source("Graphene-1.0.gir", "org.gnome.graphene", false, Set.of("graphene-1.0"), null)
            .source("Gsk-4.0.gir", "org.gnome.gsk", false, null, null)
            .source("Gtk-4.0.gir", "org.gnome.gtk", false, Set.of("gtk-4"), new GtkPatch())

            .source("Adw-1.gir", "org.gnome.adw", true, Set.of("adwaita-1"), new AdwaitaPatch())

            .moduleInfo(MODULE_INFO);

        javadocOperation().javadocOptions()
            .linkOffline("https://jwharm.github.io/java-gi/glib", new File(bld.buildJavadocDirectory(), "glib").getAbsolutePath())
            .linkOffline("https://jwharm.github.io/java-gi/gtk", new File(bld.buildJavadocDirectory(), "gtk").getAbsolutePath());
    }
}
