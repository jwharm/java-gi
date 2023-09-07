/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2023 Jan-Willem Harmannij
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package io.github.jwharm.javagi.configuration;

import java.util.Map;

import static java.util.Map.entry;

/**
 * The PackageNames class defines the mapping between GIR namespaces
 * and Java package names.
 */
public class PackageNames {

    public static Map<String, String> getMap() {
        return Map.ofEntries(
                entry("GLib",                      "org.gnome.glib"),
                entry("GObject",                   "org.gnome.gobject"),
                entry("GModule",                   "org.gnome.gmodule"),
                entry("Gio",                       "org.gnome.gio"),
                entry("cairo",                     "org.freedesktop.cairo"),
                entry("freetype2",                 "org.freedesktop.freetype"),
                entry("HarfBuzz",                  "org.freedesktop.harfbuzz"),
                entry("Pango",                     "org.gnome.pango"),
                entry("PangoCairo",                "org.gnome.pango.cairo"),
                entry("GdkPixbuf",                 "org.gnome.gdkpixbuf"),
                entry("Gdk",                       "org.gnome.gdk"),
                entry("Graphene",                  "org.gnome.graphene"),
                entry("Gsk",                       "org.gnome.gsk"),
                entry("Gtk",                       "org.gnome.gtk"),
                entry("GtkSource",                 "org.gnome.gtksourceview"),
                entry("Adw",                       "org.gnome.adw"),
                entry("Soup",                      "org.gnome.soup"),
                entry("JavaScriptCore",            "org.gnome.webkit.jsc"),
                entry("WebKit",                    "org.gnome.webkit"),
                entry("WebKitWebProcessExtension", "org.gnome.webkit.wpe"),
                entry("Gst",                       "org.freedesktop.gstreamer"),
                entry("GstBase",                   "org.freedesktop.gstreamer.base"),
                entry("GstAudio",                  "org.freedesktop.gstreamer.audio"),
                entry("GstPbutils",                "org.freedesktop.gstreamer.pbutils"),
                entry("GstVideo",                  "org.freedesktop.gstreamer.video")
        );
    }
}
