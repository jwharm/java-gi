package io.github.jwharm.javagi.generator

/**
 * The PackageNames class defines the mapping between GIR namespaces
 * and Java package names.
 */
class PackageNames {

    static Map<String, String> map =
        [
                GLib                     : 'org.gnome.glib',
                GObject                  : 'org.gnome.gobject',
                GModule                  : 'org.gnome.gmodule',
                Gio                      : 'org.gnome.gio',
                cairo                    : 'org.freedesktop.cairo',
                freetype2                : 'org.freedesktop.freetype',
                HarfBuzz                 : 'org.freedesktop.harfbuzz',
                Pango                    : 'org.gnome.pango',
                PangoCairo               : 'org.gnome.pango.cairo',
                GdkPixbuf                : 'org.gnome.gdkpixbuf',
                Gdk                      : 'org.gnome.gdk',
                Graphene                 : 'org.gnome.graphene',
                Gsk                      : 'org.gnome.gsk',
                Gtk                      : 'org.gnome.gtk',
                GtkSource                : 'org.gnome.gtksourceview',
                Adw                      : 'org.gnome.adw',
                Soup                     : 'org.gnome.soup',
                JavaScriptCore           : 'org.gnome.webkit.jsc',
                WebKit                   : "org.gnome.webkit",
                WebKitWebProcessExtension: "org.gnome.webkit.wpe",
                Gst                      : 'org.freedesktop.gstreamer',
                GstBase                  : 'org.freedesktop.gstreamer.base',
                GstAudio                 : 'org.freedesktop.gstreamer.audio',
                GstPbutils               : 'org.freedesktop.gstreamer.pbutils',
                GstVideo                 : 'org.freedesktop.gstreamer.video'
        ]
}
