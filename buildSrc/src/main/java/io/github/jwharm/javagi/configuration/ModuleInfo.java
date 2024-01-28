/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 Jan-Willem Harmannij
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
public final class ModuleInfo {

    private record Module(String packageName, String docUrlPrefix) {}

    private static final Map<String, Module> MODULES = Map.ofEntries(
            entry("GLib",                      new Module("org.gnome.glib", "")),
            entry("GObject",                   new Module("org.gnome.gobject", "")),
            entry("GModule",                   new Module("org.gnome.gmodule", "")),
            entry("Gio",                       new Module("org.gnome.gio", "")),
            entry("cairo",                     new Module("org.freedesktop.cairo", "")),
            entry("freetype2",                 new Module("org.freedesktop.freetype", "")),
            entry("HarfBuzz",                  new Module("org.freedesktop.harfbuzz", "")),
            entry("Pango",                     new Module("org.gnome.pango", "")),
            entry("PangoCairo",                new Module("org.gnome.pango.cairo", "")),
            entry("GdkPixbuf",                 new Module("org.gnome.gdkpixbuf", "")),
            entry("Gdk",                       new Module("org.gnome.gdk", "")),
            entry("GdkWin32",                  new Module("org.gnome.gdk", "")),
            entry("GdkWayland",                new Module("org.gnome.gdk", "")),
            entry("GdkX11",                    new Module("org.gnome.gdk", "")),
            entry("Graphene",                  new Module("org.gnome.graphene", "")),
            entry("Gsk",                       new Module("org.gnome.gsk", "")),
            entry("Gtk",                       new Module("org.gnome.gtk", "")),
            entry("GtkSource",                 new Module("org.gnome.gtksourceview", "")),
            entry("Adw",                       new Module("org.gnome.adw", "")),
            entry("Soup",                      new Module("org.gnome.soup", "")),
            entry("JavaScriptCore",            new Module("org.gnome.webkit.jsc", "")),
            entry("WebKit",                    new Module("org.gnome.webkit", "")),
            entry("WebKitWebProcessExtension", new Module("org.gnome.webkit.wpe", "")),
            entry("Gst",                       new Module("org.freedesktop.gstreamer.gst", "")),
            entry("GstBase",                   new Module("org.freedesktop.gstreamer.base", "")),
            entry("GstAudio",                  new Module("org.freedesktop.gstreamer.audio", "")),
            entry("GstPbutils",                new Module("org.freedesktop.gstreamer.pbutils", "")),
            entry("GstVideo",                  new Module("org.freedesktop.gstreamer.video", "")),
            entry("xlib",                      new Module("org.freedesktop.xorg.xlib", ""))
    );

    public static String getPackageName(String namespace) {
        return MODULES.get(namespace).packageName();
    }

    public static String getDocUrlPrefix(String namespace) {
        return MODULES.get(namespace).docUrlPrefix();
    }
}
