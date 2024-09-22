/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 the Java-GI developers
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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeVariableName;

import static com.squareup.javapoet.ClassName.get;

public final class ClassNames {
    private static final String PKG_TOPLEVEL        = "io.github.jwharm.javagi";
    private static final String PKG_BASE            = "io.github.jwharm.javagi.base";
    private static final String PKG_INTEROP         = "io.github.jwharm.javagi.interop";
    private static final String PKG_GIO             = "io.github.jwharm.javagi.gio";
    private static final String PKG_GOBJECT         = "io.github.jwharm.javagi.gobject";
    private static final String PKG_GOBJECT_TYPES   = "io.github.jwharm.javagi.gobject.types";

    public static final ClassName CONSTANTS = get(PKG_TOPLEVEL, "Constants");

    public static final ClassName ALIAS = get(PKG_BASE, "Alias");
    public static final ClassName ENUMERATION = get(PKG_BASE, "Enumeration");
    public static final ClassName FLOATING = get(PKG_BASE, "Floating");
    public static final ClassName GERROR_EXCEPTION = get(PKG_BASE, "GErrorException");
    public static final ClassName GLIB_LOGGER = get(PKG_BASE, "GLibLogger");
    public static final ClassName OUT = get(PKG_BASE, "Out");
    public static final ClassName PROXY = get(PKG_BASE, "Proxy");
    public static final ClassName PROXY_INSTANCE = get(PKG_BASE, "ProxyInstance");
    public static final ClassName UNSUPPORTED_PLATFORM_EXCEPTION = get(PKG_BASE, "UnsupportedPlatformException");

    public static final ClassName ARENAS = get(PKG_INTEROP, "Arenas");
    public static final ClassName ARENA_CLOSE_ACTION = get(PKG_INTEROP, "ArenaCloseAction");
    public static final ClassName MEMORY_CLEANER = get(PKG_INTEROP, "MemoryCleaner");
    public static final ClassName INTEROP = get(PKG_INTEROP, "Interop");
    public static final ClassName PLATFORM = get(PKG_INTEROP, "Platform");

    public static final ClassName AUTO_CLOSEABLE = get(PKG_GIO, "AutoCloseable");
    public static final ClassName LIST_MODEL_JAVA_LIST = get(PKG_GIO, "ListModelJavaList");

    public static final ClassName BUILDER = get(PKG_GOBJECT, "Builder");
    public static final ClassName BUILDER_INTERFACE = get(PKG_GOBJECT, "BuilderInterface");
    public static final ClassName INSTANCE_CACHE = get(PKG_GOBJECT, "InstanceCache");
    public static final ClassName JAVA_CLOSURE = get(PKG_GOBJECT, "JavaClosure");
    public static final ClassName SIGNAL_CONNECTION = get(PKG_GOBJECT, "SignalConnection");

    public static final ClassName OVERRIDES = get(PKG_GOBJECT_TYPES, "Overrides");
    public static final ClassName PROPERTIES = get(PKG_GOBJECT_TYPES, "Properties");
    public static final ClassName SIGNALS = get(PKG_GOBJECT_TYPES, "Signals");
    public static final ClassName TYPE_CACHE = get(PKG_GOBJECT_TYPES, "TypeCache");
    public static final ClassName TYPES = get(PKG_GOBJECT_TYPES, "Types");

    // Some frequently used class names
    public final static ClassName DESTROY_NOTIFY = get("org.gnome.glib", "DestroyNotify");
    public final static ClassName GERROR = get("org.gnome.glib", "GError");
    public final static ClassName GLIB = get("org.gnome.glib", "GLib");
    public final static ClassName GTYPE = get("org.gnome.glib", "Type");
    public final static ClassName LOG_LEVEL_FLAGS = get("org.gnome.glib", "LogLevelFlags");

    public final static ClassName GOBJECT = get("org.gnome.gobject", "GObject");
    public final static ClassName GOBJECTS = get("org.gnome.gobject", "GObjects");
    public final static ClassName GVALUE = get("org.gnome.gobject", "Value");
    public final static ClassName TYPE_CLASS = get("org.gnome.gobject", "TypeClass");
    public final static ClassName TYPE_INTERFACE = get("org.gnome.gobject", "TypeInterface");
    public final static ClassName TYPE_INSTANCE = get("org.gnome.gobject", "TypeInstance");

    // The type variable used for <T extends GObject>
    public final static TypeVariableName GENERIC_T = TypeVariableName.get("T", GOBJECT);
}
