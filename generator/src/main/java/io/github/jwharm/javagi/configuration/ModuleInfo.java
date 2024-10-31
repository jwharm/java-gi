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

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static java.util.Map.entry;
import static java.util.Objects.requireNonNullElse;

/**
 * The ModuleInfo class defines the mapping between GIR namespaces and Java
 * package names, the base URL for image links, and a short description of the
 * package.
 */
public final class ModuleInfo {

    public record Module(String moduleName, String packageName, String docUrlPrefix, String description) {}

    public static final Map<String, Module> INCLUDED_MODULES = Map.ofEntries(
            entry("glib",                      new Module("GLib", "org.gnome.glib", "https://docs.gtk.org/glib/", "A general-purpose, portable utility library, which provides many useful data types, macros, type conversions, string utilities, file utilities, a mainloop abstraction, and so on")),
            entry("gobject",                   new Module("GObject", "org.gnome.gobject", "https://docs.gtk.org/glib/" ,"The base type system and object class")),
            entry("gmodule",                   new Module("GModule", "org.gnome.gmodule", "https://docs.gtk.org/glib/", "A portable API for dynamically loading modules")),
            entry("gio",                       new Module("GIO", "org.gnome.gio", "https://docs.gtk.org/glib/", "A library providing useful classes for general purpose I/O, networking, IPC, settings, and other high level application functionality")),
            entry("cairo",                     new Module("Cairo", "org.freedesktop.cairo", "", "")),
            entry("freetype2",                 new Module("FreeType2", "org.freedesktop.freetype", "", "")),
            entry("harfbuzz",                  new Module("HarfBuzz", "org.freedesktop.harfbuzz", "", "The HarfBuzz text shaping engine")),
            entry("pango",                     new Module("Pango", "org.gnome.pango", "https://docs.gtk.org/Pango/", "A library for layout and rendering of text, with an emphasis on internationalization")),
            entry("pangocairo",                new Module("PangoCairo", "org.gnome.pango.cairo", "https://docs.gtk.org/Pango/", "Cairo support for Pango")),
            entry("gdkpixbuf",                 new Module("GdkPixbuf", "org.gnome.gdkpixbuf", "https://docs.gtk.org/gdk-pixbuf/", "A library that loads image data in various formats and stores it as linear buffers in memory")),
            entry("gdk",                       new Module("GDK", "org.gnome.gdk", "https://docs.gtk.org/gdk4/", "The low-level library used by GTK to interact with the windowing system for graphics and input devices")),
            entry("gdkwin32",                  new Module("GdkWin32", "org.gnome.gdk", "", "")),
            entry("gdkwayland",                new Module("GdkWayland", "org.gnome.gdk", "", "")),
            entry("gdkx11",                    new Module("GdkX11", "org.gnome.gdk", "", "")),
            entry("graphene",                  new Module("Graphene", "org.gnome.graphene", "https://developer-old.gnome.org/graphene/stable/", "A thin layer of types for graphic libraries")),
            entry("gsk",                       new Module("GSK", "org.gnome.gsk", "", "The rendering and scene graph API for GTK")),
            entry("gtk",                       new Module("GTK", "org.gnome.gtk", "https://docs.gtk.org/gtk4/", "GTK is a multi-platform toolkit for creating graphical user interfaces")),
            entry("gtksource",                 new Module("GtkSourceView", "org.gnome.gtksourceview", "https://gnome.pages.gitlab.gnome.org/gtksourceview/gtksourceview5/", "A text editor widget for code editing")),
            entry("adw",                       new Module("LibAdwaita", "org.gnome.adw", "https://gnome.pages.gitlab.gnome.org/libadwaita/doc/1-latest/", "Building blocks for modern GNOME applications")),
            entry("soup",                      new Module("LibSoup", "org.gnome.soup", "https://libsoup.org/libsoup-3.0/", "An HTTP client/server library for GNOME")),
            entry("javascriptcore",            new Module("JavaScriptCore", "org.gnome.webkit.jsc", "https://webkitgtk.org/reference/jsc-glib/stable/", "The JavaScript engine used in WebKit")),
            entry("webkit",                    new Module("WebKitGTK", "org.gnome.webkit", "https://webkitgtk.org/reference/webkit2gtk/stable/", "WebKitGTK is a full-featured port of the WebKit rendering engine")),
            entry("webkitwebprocessextension", new Module("WebKitWebProcessExtension", "org.gnome.webkit.wpe", "https://webkitgtk.org/reference/webkit2gtk-web-extension/stable/", "The WebKit web extension and DOM library")),
            entry("gst",                       new Module("Gst", "org.freedesktop.gstreamer.gst", "", "Provides all the core GStreamer services, including initialization, plugin management and types, as well as the object hierarchy that defines elements and bins, along with some more specialized elements")),
            entry("gstbase",                   new Module("GstBase", "org.freedesktop.gstreamer.base", "", "Provides some GStreamer base classes to be extended by elements and utility classes that are most useful for plugin developers")),
            entry("gstaudio",                  new Module("GstAudio", "org.freedesktop.gstreamer.audio", "", "The GStreamer Audio Library")),
            entry("gstpbutils",                new Module("GstPbutils", "org.freedesktop.gstreamer.pbutils", "", "A general utility library for GStreamer plugins and applications")),
            entry("gstvideo",                  new Module("GstVideo", "org.freedesktop.gstreamer.video", "", "The GStreamer Video Library")),
            entry("xlib",                      new Module("XLib", "org.freedesktop.xorg.xlib", "", ""))
    );

    public static final Map<String, Module> ALL_MODULES = new HashMap<>(INCLUDED_MODULES);

    /**
     * Add information about a module.
     *
     * @param namespace    name of the GIR namespace
     * @param moduleName   official name of the module
     * @param packageName  name of the generated Java package
     * @param docUrlPrefix URL to be prefixed to hyperlinks in generated Javadoc
     * @param description  description of the generated Java package
     */
    public static void add(String namespace,
                            String moduleName,
                            String packageName,
                            String docUrlPrefix,
                            String description) {
        ALL_MODULES.put(namespace.toLowerCase(), new Module(
                requireNonNullElse(moduleName, ""),
                requireNonNullElse(packageName, ""),
                requireNonNullElse(docUrlPrefix, ""),
                requireNonNullElse(description, "")
        ));
    }

    private static Module get(String namespace) {
        Module info = ALL_MODULES.get(namespace.toLowerCase());
        if (info == null)
            throw new NoSuchElementException(namespace);
        return info;
    }

    /**
     * Get the official name for the specified GIR namespace
     */
    public static String moduleName(String namespace) {
        return get(namespace).moduleName();
    }

    /**
     * Get the generated Java package name for the specified GIR namespace
     */
    public static String packageName(String namespace) {
        return get(namespace).packageName();
    }

    /**
     * Get the URL prefix for hyperlinks in Javadoc generated for the specified
     * GIR namespace
     */
    public static String docUrlPrefix(String namespace) {
        return get(namespace).docUrlPrefix();
    }

    /**
     * Get the Java package description for the specified GIR namespace
     */
    public static String description(String namespace) {
        return get(namespace).description();
    }
}
