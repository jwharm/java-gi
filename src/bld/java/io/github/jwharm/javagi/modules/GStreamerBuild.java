package io.github.jwharm.javagi.modules;

import io.github.jwharm.javagi.JavaGIBuild;
import io.github.jwharm.javagi.AbstractProject;
import io.github.jwharm.javagi.patches.*;

import java.io.File;
import java.util.Set;

public class GStreamerBuild extends AbstractProject {

    private static final String MODULE_INFO = """
        module org.freedesktop.gstreamer {
            requires static org.jetbrains.annotations;
            requires transitive org.gnome.glib;
            %s
        }
        """;

    public GStreamerBuild(JavaGIBuild bld) {
        super(bld, "gstreamer");
        version = version(1, 20).withQualifier(bld.version().toString());

        generateSourcesOperation()
                .source("GLib-2.0.gir", "org.gnome.glib", "https://docs.gtk.org/glib/", false, Set.of("glib-2.0"), new GLibPatch())
                .source("GObject-2.0.gir", "org.gnome.gobject", "https://docs.gtk.org/gobject/", false, Set.of("gobject-2.0"), new GObjectPatch())
                .source("Gio-2.0.gir", "org.gnome.gio", "https://docs.gtk.org/gio/", false, Set.of("gio-2.0"), new GioPatch())
                .source("GModule-2.0.gir", "org.gnome.gmodule", null, false, null, null)

            .source("Gst-1.0.gir", "org.freedesktop.gstreamer.gst", null, true, Set.of("gstreamer-1.0"), new GstPatch())
            .source("GstBase-1.0.gir", "org.freedesktop.gstreamer.base", null, true, Set.of("gstbase-1.0"), null)
            .source("GstAudio-1.0.gir", "org.freedesktop.gstreamer.audio", null, true, Set.of("gstaudio-1.0"), new GstAudioPatch())
            .source("GstPbutils-1.0.gir", "org.freedesktop.gstreamer.pbutils", null, true, Set.of("gstpbutils-1.0"), null)
            .source("GstVideo-1.0.gir", "org.freedesktop.gstreamer.video", null, true, Set.of("gstvideo-1.0"), null)

            .moduleInfo(MODULE_INFO);

        javadocOperation().javadocOptions()
            .linkOffline("https://jwharm.github.io/java-gi/glib", new File(bld.buildJavadocDirectory(), "glib").getAbsolutePath());
    }
}
