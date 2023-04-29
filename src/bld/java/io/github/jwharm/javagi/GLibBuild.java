package io.github.jwharm.javagi;

import io.github.jwharm.javagi.patches.*;

import java.io.File;
import java.util.Set;

public class GLibBuild extends JavaGIProject {

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

        pkg = "io.jwharm.javagi.glib";
        name = "glib";
        version = version(2,74).withQualifier(bld.version().toString());

        srcDirectory = new File(workDirectory(), "glib");

        javaGIOperation()
            .source("GLib-2.0.gir", "org.gnome.glib", true, Set.of("glib-2.0"), new GLibPatch())
            .source("GObject-2.0.gir", "org.gnome.gobject", true, Set.of("gobject-2.0"), new GObjectPatch())
            .source("Gio-2.0.gir", "org.gnome.gio", true, Set.of("gio-2.0"), new GioPatch())
            .source("GModule-2.0.gir", "org.gnome.gmodule", true, null, null)
            .moduleInfo(MODULE_INFO);
    }
}