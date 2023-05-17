package io.github.jwharm.javagi.modules;

import io.github.jwharm.javagi.JavaGIBuild;
import io.github.jwharm.javagi.AbstractProject;
import io.github.jwharm.javagi.patches.*;

import java.io.File;
import java.util.Set;

public class GLibBuild extends AbstractProject {

    private static final String MODULE_INFO = """
        module org.gnome.glib {
            requires static org.jetbrains.annotations;
            exports io.github.jwharm.javagi.annotations;
            exports io.github.jwharm.javagi.base;
            exports io.github.jwharm.javagi.interop;
            exports io.github.jwharm.javagi.pointer;
            exports io.github.jwharm.javagi.types;
            exports io.github.jwharm.javagi.util;
            %s
        }
        """;

    public GLibBuild(JavaGIBuild bld) {
        super(bld, "glib");
        version = version(2,76).withQualifier(bld.version().toString());
        srcDirectory = new File(workDirectory(), name);

        generateSourcesOperation()
            .source("GLib-2.0.gir", "org.gnome.glib", "https://docs.gtk.org/glib/", true, Set.of("glib-2.0"), new GLibPatch())
            .source("GObject-2.0.gir", "org.gnome.gobject", "https://docs.gtk.org/gobject/", true, Set.of("gobject-2.0"), new GObjectPatch())
            .source("Gio-2.0.gir", "org.gnome.gio", "https://docs.gtk.org/gio/", true, Set.of("gio-2.0"), new GioPatch())
            .source("GModule-2.0.gir", "org.gnome.gmodule", null, true, null, null)
            .moduleInfo(MODULE_INFO);
    }
}