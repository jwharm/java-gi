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

package io.github.jwharm.javagi.gobject.types;

import io.github.jwharm.javagi.base.Proxy;
import io.github.jwharm.javagi.gobject.annotations.*;
import io.github.jwharm.javagi.gobject.InstanceCache;
import io.github.jwharm.javagi.interop.Interop;
import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.glib.Type;
import org.gnome.gobject.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemoryLayout;
import java.lang.reflect.*;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.github.jwharm.javagi.Constants.LOG_DOMAIN;

/**
 * The Types class contains GType constants, a series of static methods to
 * check gtype characteristics, and static methods to register a Java class as
 * a new GObject-derived GType.
 */
@SuppressWarnings("unused")
public class Types {

    // GLib fundamental types, adapted from <gobject/gtype.h>

    private static final long FUNDAMENTAL_SHIFT = 2;

    /**
     * An integer constant that represents the number of identifiers reserved
     * for types that are assigned at compile-time.
     */
    private static final long FUNDAMENTAL_MAX = (255 << FUNDAMENTAL_SHIFT);

    /**
     * An invalid {@code GType} used as error return value in some functions
     * which return a {@code GType}.
     */
    public static final Type INVALID = new Type(0L /* << FUNDAMENTAL_SHIFT */);

    /**
     * A fundamental type which is used as a replacement for the C
     * void return type.
     */
    public static final Type NONE = new Type(1L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type from which all interfaces are derived.
     */
    public static final Type INTERFACE = new Type(2L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type corresponding to {@code gchar}.
     * <p>
     * The type designated by {@code CHAR} is unconditionally an 8-bit signed
     * integer. This may or may not be the same type a the C type "gchar".
     */
    public static final Type CHAR = new Type(3L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type corresponding to {@code guchar}.
     */
    public static final Type UCHAR = new Type(4L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type corresponding to {@code gboolean}.
     */
    public static final Type BOOLEAN = new Type(5L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type corresponding to {@code gint}.
     */
    public static final Type INT = new Type(6L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type corresponding to {@code guint}.
     */
    public static final Type UINT = new Type(7L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type corresponding to {@code glong}.
     */
    public static final Type LONG = new Type(8L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type corresponding to {@code gulong}.
     */
    public static final Type ULONG = new Type(9L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type corresponding to {@code gint64}.
     */
    public static final Type INT64 = new Type(10L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type corresponding to {@code guint64}.
     */
    public static final Type UINT64 = new Type(11L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type from which all enumeration types are derived.
     */
    public static final Type ENUM = new Type(12L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type from which all flags types are derived.
     */
    public static final Type FLAGS = new Type(13L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type corresponding to {@code gfloat}.
     */
    public static final Type FLOAT = new Type(14L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type corresponding to {@code gdouble}.
     */
    public static final Type DOUBLE = new Type(15L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type corresponding to nul-terminated C strings.
     */
    public static final Type STRING = new Type(16L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type corresponding to {@code gpointer}.
     */
    public static final Type POINTER = new Type(17L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type from which all boxed types are derived.
     */
    public static final Type BOXED = new Type(18L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type from which all {@code GParamSpec} types are
     * derived.
     */
    public static final Type PARAM = new Type(19L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type for {@code GObject}.
     */
    public static final Type OBJECT = new Type(20L << FUNDAMENTAL_SHIFT);

    /**
     * The fundamental type corresponding to {@code G_TYPE_VARIANT}.
     * <p>
     * All floating {@code G_TYPE_VARIANT} instances passed through the
     * {@code GType} system are consumed.
     * <p>
     * Note that callbacks in closures, and signal handlers
     * for signals of return type {@code G_TYPE_VARIANT}, must never return
     * floating variants.
     * <p>
     * Note: GLib 2.24 did include a boxed type with this name. It was replaced
     * with this fundamental type in 2.26.
     * <p>
     *
     * @since 2.26
     */
    public static final Type VARIANT = new Type(21L << FUNDAMENTAL_SHIFT);

    // GLib boxed types, adapted from <gobject/glib-types.h>

    /**
     * The {@code GType} for {@code GDate}.
     */
    public static final Type DATE = Interop.getType("g_date_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code null}-terminated
     * array of strings.
     *
     * @since 2.4
     */
    public static final Type STRV = Interop.getType("g_strv_get_type");

    /**
     * The {@code GType} for {@code GString}.
     */
    public static final Type GSTRING = Interop.getType("g_gstring_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GHashTable}
     * reference.
     *
     * @since 2.10
     */
    public static final Type HASH_TABLE = Interop.getType("g_hash_table_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GRegex} reference.
     *
     * @since 2.14
     */
    public static final Type REGEX = Interop.getType("g_regex_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GMatchInfo}
     * reference.
     *
     * @since 2.30
     */
    public static final Type MATCH_INFO = Interop.getType("g_match_info_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GArray} reference.
     *
     * @since 2.22
     */
    public static final Type ARRAY = Interop.getType("g_array_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GByteArray}
     * reference.
     *
     * @since 2.22
     */
    public static final Type BYTE_ARRAY = Interop.getType("g_byte_array_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GPtrArray}
     * reference.
     *
     * @since 2.22
     */
    public static final Type PTR_ARRAY = Interop.getType("g_ptr_array_get_type");

    /**
     * The {@code GType} for {@code GBytes}.
     *
     * @since 2.32
     */
    public static final Type BYTES = Interop.getType("g_bytes_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GVariantType}.
     *
     * @since 2.24
     */
    public static final Type VARIANT_TYPE = Interop.getType("g_variant_type_get_gtype");

    /**
     * The {@code GType} for a boxed type holding a {@code GError}.
     *
     * @since 2.26
     */
    public static final Type ERROR = Interop.getType("g_error_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GDateTime}.
     *
     * @since 2.26
     */
    public static final Type DATE_TIME = Interop.getType("g_date_time_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GTimeZone}.
     *
     * @since 2.34
     */
    public static final Type TIME_ZONE = Interop.getType("g_time_zone_get_type");

    /**
     * The {@code GType} for {@code GIOChannel}.
     */
    public static final Type IO_CHANNEL = Interop.getType("g_io_channel_get_type");

    /**
     * The {@code GType} for {@code GIOCondition}.
     */
    public static final Type IO_CONDITION = Interop.getType("g_io_condition_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GVariantBuilder}.
     *
     * @since 2.30
     */
    public static final Type VARIANT_BUILDER = Interop.getType("g_variant_builder_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GVariantDict}.
     *
     * @since 2.40
     */
    public static final Type VARIANT_DICT = Interop.getType("g_variant_dict_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GMainLoop}.
     *
     * @since 2.30
     */
    public static final Type MAIN_LOOP = Interop.getType("g_main_loop_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GMainContext}.
     *
     * @since 2.30
     */
    public static final Type MAIN_CONTEXT = Interop.getType("g_main_context_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GSource}.
     *
     * @since 2.30
     */
    public static final Type SOURCE = Interop.getType("g_source_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GPollFD}.
     *
     * @since 2.36
     */
    public static final Type POLLFD = Interop.getType("g_pollfd_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GMarkupParseContext}.
     *
     * @since 2.36
     */
    public static final Type MARKUP_PARSE_CONTEXT = Interop.getType("g_markup_parse_context_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GKeyFile}.
     *
     * @since 2.32
     */
    public static final Type KEY_FILE = Interop.getType("g_key_file_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GMappedFile}.
     *
     * @since 2.40
     */
    public static final Type MAPPED_FILE = Interop.getType("g_mapped_file_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GThread}.
     *
     * @since 2.36
     */
    public static final Type THREAD = Interop.getType("g_thread_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GChecksum}.
     *
     * @since 2.36
     */
    public static final Type CHECKSUM = Interop.getType("g_checksum_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GOptionGroup}.
     *
     * @since 2.44
     */
    public static final Type OPTION_GROUP = Interop.getType("g_option_group_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GUri}.
     *
     * @since 2.66
     */
    public static final Type URI = Interop.getType("g_uri_get_type");

    /**
     * The {@code GType} for {@code GTree}.
     *
     * @since 2.68
     */
    public static final Type TREE = Interop.getType("g_tree_get_type");

    /**
     * The {@code GType} for {@code GPatternSpec}.
     *
     * @since 2.70
     */
    public static final Type PATTERN_SPEC = Interop.getType("g_pattern_spec_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GBookmarkFile}.
     *
     * @since 2.76
     */
    public static final Type BOOKMARK_FILE = Interop.getType("g_bookmark_file_get_type");

    /**
     * First fundamental type number to create a new fundamental type id with
     * G_TYPE_MAKE_FUNDAMENTAL() reserved for GLib.
     */
    public static final long RESERVED_GLIB_FIRST	= 22L;

    /**
     * Last fundamental type number reserved for GLib.
     */
    public static final long RESERVED_GLIB_LAST = 31L;

    /**
     * First fundamental type number to create a new fundamental type id with
     * G_TYPE_MAKE_FUNDAMENTAL() reserved for BSE.
     */
    public static final long RESERVED_BSE_FIRST = 32L;

    /**
     * Last fundamental type number reserved for BSE.
     */
    public static final long RESERVED_BSE_LAST = 48L;

    /**
     * First available fundamental type number to create new fundamental
     * type id with G_TYPE_MAKE_FUNDAMENTAL().
     */
    public static final long RESERVED_USER_FIRST	= 49L;

    // Type Checking Macros

    /**
     * Checks if {@code type} is a fundamental type.
     *
     * @param  type A {@link org.gnome.glib.Type} value
     * @return {@code true} if {@code type} is fundamental
     */
    public static boolean IS_FUNDAMENTAL(Type type) {
        return type.getValue() <= FUNDAMENTAL_MAX;
    }

    /**
     * Checks if {@code type} is derived (or in object-oriented terminology:
     * inherited) from another type (this holds true for all non-fundamental
     * types).
     *
     * @param  type A {@link org.gnome.glib.Type} value
     * @return {@code true} if {@code type} is derived
     */
    public static boolean IS_DERIVED(Type type) {
        return type.getValue() > FUNDAMENTAL_MAX;
    }

    /**
     * Checks if {@code type} is an interface type.
     * <p>
     * An interface type provides a pure API, the implementation
     * of which is provided by another type (which is then said to conform
     * to the interface).  GLib interfaces are somewhat analogous to Java
     * interfaces and C++ classes containing only pure virtual functions,
     * with the difference that GType interfaces are not derivable (but see
     * g_type_interface_add_prerequisite() for an alternative).
     *
     * @param  type A {@link org.gnome.glib.Type} value
     * @return {@code true} if {@code type} is an interface
     */
    public static boolean IS_INTERFACE(Type type) {
        return GObjects.typeFundamental(type).equals(INTERFACE);
    }

    /**
     * Checks if {@code type} is a classed type.
     * <p>
     * A classed type has an associated {@link org.gnome.gobject.TypeClass}
     * which can be derived to store class-wide virtual function pointers and
     * data for all instances of the type. This allows for subclassing. All
     * {@link org.gnome.gobject.GObject}s are classed; none of the scalar
     * fundamental types built into GLib are classed.
     * <p>
     * Interfaces are not classed: while their
     * {@link org.gnome.gobject.TypeInterface} struct could be considered
     * similar to {@link org.gnome.gobject.TypeClass}, and classes can derive
     * interfaces, {@link org.gnome.gobject.TypeInterface} doesn’t allow for
     * subclassing.
     *
     * @param  type A {@link org.gnome.glib.Type} value
     * @return {@code true} if {@code type} is classed
     */
    public static boolean IS_CLASSED(Type type) {
        return GObjects.typeTestFlags(type, TypeFundamentalFlags.CLASSED.getValue());
    }

    /**
     * Checks if {@code type} can be instantiated. Instantiation is the
     * process of creating an instance (object) of this type.
     *
     * @param  type A {@link org.gnome.glib.Type} value
     * @return {@code true} if {@code type} is instantiable
     */
    public static boolean IS_INSTANTIATABLE(Type type) {
        return GObjects.typeTestFlags(type, TypeFundamentalFlags.INSTANTIATABLE.getValue());
    }

    /**
     * Checks if {@code type} is a derivable type.  A derivable type can
     * be used as the base class of a flat (single-level) class hierarchy.
     *
     * @param  type A {@link org.gnome.glib.Type} value
     * @return {@code true} if {@code type} is derivable
     */
    public static boolean IS_DERIVABLE(Type type) {
        return GObjects.typeTestFlags(type, TypeFundamentalFlags.DERIVABLE.getValue());
    }

    /**
     * Checks if {@code type} is a deep derivable type.  A deep derivable type
     * can be used as the base class of a deep (multi-level) class hierarchy.
     *
     * @param  type A {@link org.gnome.glib.Type} value
     * @return {@code true} if {@code type} is deep derivable
     */
    public static boolean IS_DEEP_DERIVABLE(Type type) {
        return GObjects.typeTestFlags(type, TypeFundamentalFlags.DEEP_DERIVABLE.getValue());
    }

    /**
     * Checks if {@code type} is an abstract type.  An abstract type cannot be
     * instantiated and is normally used as an abstract base class for
     * derived classes.
     *
     * @param  type A {@link org.gnome.glib.Type} value
     * @return {@code true} if {@code type} is abstract
     */
    public static boolean IS_ABSTRACT(Type type) {
        return GObjects.typeTestFlags(type, TypeFlags.ABSTRACT.getValue());
    }

    /**
     * Checks if {@code type} is an abstract value type.  An abstract value
     * type introduces a value table, but can't be used for g_value_init() and
     * is normally used as an abstract base type for derived value types.
     *
     * @param  type A {@link org.gnome.glib.Type} value
     * @return {@code true} if {@code type} is an abstract value type
     */
    public static boolean IS_VALUE_ABSTRACT(Type type) {
        return GObjects.typeTestFlags(type, TypeFlags.VALUE_ABSTRACT.getValue());
    }

    /**
     * Checks if {@code type} is a value type and can be used with
     * g_value_init().
     *
     * @param  type A {@link org.gnome.glib.Type} value
     * @return {@code true} if {@code type} is a value type
     */
    public static boolean IS_VALUE_TYPE(Type type) {
        return GObjects.typeCheckIsValueType(type);
    }

    /**
     * Checks if {@code type} has a {@link org.gnome.gobject.TypeValueTable}.
     *
     * @param  type A {@link org.gnome.glib.Type} value
     * @return {@code true} if {@code type} has a value table
     */
    public static boolean HAS_VALUE_TABLE(Type type) {
        return TypeValueTable.peek(type) != null;
    }

    /**
     * Checks if {@code type} is a final type. A final type cannot be derived
     * any further.
     *
     * @param  type A {@link org.gnome.glib.Type} value
     * @return {@code true} if {@code type} is final
     * @since  2.70
     */
    public static boolean IS_FINAL(Type type) {
        return GObjects.typeTestFlags(type, TypeFlags.FINAL.getValue());
    }

    /**
     * Checks if {@code type} is deprecated. Instantiating a deprecated type
     * will trigger a warning if running with {@code G_ENABLE_DIAGNOSTIC=1}.
     *
     * @param  type A {@link org.gnome.glib.Type} value
     * @return {@code true} if the type is deprecated
     * @since  2.76
     */
    public static boolean IS_DEPRECATED(Type type) {
        return GObjects.typeTestFlags(type, TypeFlags.DEPRECATED.getValue());
    }

    /**
     * Convert the name of a class to the name for a new GType. When the
     * {@link RegisteredType} annotation is present, and the name parameter of
     * the annotation is not empty, it will be returned. Otherwise, the
     * package and class name will be used as the new GType name (with all
     * characters except a-z and A-Z converted to underscores).
     *
     * @param  cls the class for which a GType name is returned
     * @return the GType name
     */
    public static String getName(Class<?> cls) {
        // Default type name: fully qualified Java class name
        String typeNameInput = cls.getName();

        // Check for an annotation that overrides the type name
        if (cls.isAnnotationPresent(RegisteredType.class)) {
            var annotation = cls.getAnnotation(RegisteredType.class);
            if (! "".equals(annotation.name())) {
                typeNameInput = annotation.name();
            }
        }

        // Replace all characters except a-z or A-Z with underscores
        return typeNameInput.replaceAll("[^a-zA-Z]", "_");
    }

    /**
     * Return the memory layout defined in the provided class, or if not found,
     * a new {@code MemoryLayout.structLayout} with one field that points to
     * the memory layout defined in the direct superclass.
     *
     * @param  cls      the class to provide a memory layout for
     * @param  typeName the name given tot the generated memory layout
     * @param  <T>      the class must extend {@link org.gnome.gobject.GObject}
     * @return the declared memory layout, or if not found, a generated memory
     *         layout that copies the memory layout declared in the direct
     *         superclass.
     */
    public static <T extends GObject> MemoryLayout getInstanceLayout(Class<T> cls, String typeName) {
            // Get instance-memorylayout of this class
            MemoryLayout instanceLayout = getLayout(cls);
            if (instanceLayout != null)
                return instanceLayout;

            // If no memory layout was defined, create a default memory layout
            // that only has a pointer to the parent class' memory layout.
            MemoryLayout parentLayout = getLayout(cls.getSuperclass());

            if (parentLayout == null) {
                GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                        "Cannot find memory layout definition for class %s\n", cls.getName());
                return null;
            }

            return MemoryLayout.structLayout(
                    parentLayout.withName("parent_instance")
            ).withName(typeName);
    }

    /**
     * Return the inner TypeClass class, or the inner TypeClass from the
     * superclass, or null if not found.
     *
     * @param  cls  the class that contains (or whose superclass contains) an
     *              inner TypeClass class
     * @param  <T>  the parameter must extend {@link org.gnome.gobject.TypeInstance}
     * @param  <TC> the returned class extends {@link org.gnome.gobject.TypeClass}
     * @return the TypeClass class, or null if not found
     */
    @SuppressWarnings("unchecked")
    public static <T extends TypeInstance, TC extends TypeClass> Class<TC> getTypeClass(Class<T> cls) {
        // Get the type-struct. This is an inner class that extends ObjectClass.
        for (Class<?> gclass : cls.getDeclaredClasses()) {
            if (TypeClass.class.isAssignableFrom(gclass)) {
                return (Class<TC>) gclass;
            }
        }
        // If the type-struct is unavailable, get it from the parent class.
        for (Class<?> gclass : cls.getSuperclass().getDeclaredClasses()) {
            if (TypeClass.class.isAssignableFrom(gclass)) {
                return (Class<TC>) gclass;
            }
        }
        return null;
    }

    /**
     * Return the inner TypeInterface class, or null if not found.
     *
     * @param  iface the interface that contains an inner TypeInterface class
     * @param  <TI>  the returned class extends {@link org.gnome.gobject.TypeInterface}
     * @return the TypeInterface class, or null if not found
     */
    @SuppressWarnings("unchecked")
    public static <TI extends TypeInterface> Class<TI> getTypeInterface(Class<?> iface) {
        // Get the type-struct. This is an inner class that extends TypeInterface.
        for (Class<?> giface : iface.getClasses()) {
            if (TypeInterface.class.isAssignableFrom(giface)) {
                return (Class<TI>) giface;
            }
        }
        return null;
    }

    /**
     * Generate a MemoryLayout struct with one member: the memorylayout of the
     * parent TypeClass
     *
     * @param  cls      the class to get a memory layout for
     * @param  typeName the name of the new memory layout
     * @param  <T>      the class must extend {@link org.gnome.gobject.GObject}
     * @return the requested memory layout
     */
    public static <T extends GObject> MemoryLayout getClassLayout(Class<T> cls, String typeName) {
        // Get the type-struct. This is an inner class that extends GObject.ObjectClass.
        // If the type-struct is unavailable, get it from the parent class.
        Class<?> typeClass = getTypeClass(cls);
        if (typeClass == null) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find TypeClass for class %s\n", cls.getName());
            return null;
        }

        // Get class-memorylayout
        MemoryLayout parentLayout = getLayout(typeClass);

        if (parentLayout == null) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find class memory layout definition for class %s\n", cls.getName());
            return null;
        }

        return MemoryLayout.structLayout(
                parentLayout.withName("parent_class")
        ).withName(typeName + "Class");
    }

    /**
     * Return the {@link org.gnome.glib.Type} that is returned by a static
     * method with {@code @GType} annotation, or if that annotation is not
     * found, by searching for a method with return type
     * {@code org.gnome.glib.Type}, or else, return null.
     *
     * @param  cls the class for which to return the declared GType
     * @return the declared GType
     */
    public static Type getGType(Class<?> cls) {
        Method gtypeMethod = getGTypeMethod(cls);

        if (gtypeMethod == null) {
            // No gtype method found
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find static method that returns org.gnome.glib.Type in class %s\n",
                    cls.getName());
            return null;
        }

        try {
            return (Type) gtypeMethod.invoke(null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            // Method is not public, or throws an exception
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Exception while trying to read %s.%s\n",
                    cls.getName(), gtypeMethod.getName());
            return null;
        }
    }

    // Find a static method that returns the GType of this class
    private static Method getGTypeMethod(Class<?> cls) {
        Method gtypeMethod = null;

        // Find a static method that is annotated with @GType and read its value
        for (Method method : cls.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())
                    && method.isAnnotationPresent(GType.class)) {
                gtypeMethod = method;
            }
        }

        // Find a static method with return type org.gnome.glib.Type.class
        for (Method method : cls.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())
                    && method.getReturnType().equals(Type.class)) {
                gtypeMethod = method;
            }
        }
        return gtypeMethod;
    }

    /**
     * Return the MemoryLayout that is returned by a method with
     * {@code @MemoryLayout} annotation, or if that annotation is not found, by
     * invoking {@code cls.getMemoryLayout()} if such method exists, or else,
     * return null.
     *
     * @param  cls the class for which to return the declared MemoryLayout
     * @return the declared MemoryLayout
     */
    public static MemoryLayout getLayout(Class<?> cls) {
        // Find a method that is annotated with @MemoryLayout and execute it
        for (Method method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Layout.class)) {
                // Check method signature
                if ((method.getParameterTypes().length != 0)
                        || (! method.getReturnType().equals(MemoryLayout.class))) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Method %s.%s does not have expected signature () -> MemoryLayout\n",
                            cls.getName(), method.getName());
                    return null;
                }
                // Invoke the @MemoryLayout-annotated method and return the result
                try {
                    return (MemoryLayout) method.invoke(null);
                } catch (IllegalAccessException e) {
                    // Method is not public
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "IllegalAccessException when calling %s.%s\n",
                            cls.getName(), method.getName());
                    return null;
                } catch (InvocationTargetException e) {
                    // Method throws an exception
                    Throwable t = e.getTargetException();
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Exception when calling %s.%s: %s\n",
                            cls.getName(), method.getName(), t.toString());
                    return null;
                }
            }
        }

        // Find a method {@code public static MemoryLayout getMemoryLayout()}
        // and execute it
        try {
            // invoke getMemoryLayout() on the class
            Method getLayoutMethod = cls.getDeclaredMethod("getMemoryLayout");
            return (MemoryLayout) getLayoutMethod.invoke(null);

        } catch (Exception notfound) {
            return null;
        }
    }

    /**
     * Return the memory address constructor for the provided class. This is a
     * constructor for a new Proxy instance for a native memory address.
     *
     * @param  cls the class that declares a constructor with a single
     *             {@link MemorySegment} parameter
     * @param  <T> the class must implement the {@link Proxy} interface
     * @return the memory address constructor for this class, or null if not
     *         found
     */
    public static <T extends Proxy> Function<MemorySegment, T> getAddressConstructor(Class<T> cls) {
        Constructor<T> ctor;
        try {
            // Get memory address constructor
            ctor = cls.getConstructor(MemorySegment.class);
        } catch (NoSuchMethodException e) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find memory-address constructor definition for class %s: %s\n",
                    cls.getName(), e.toString());
            return null;
        }

        // Create a wrapper function that will run the constructor and catch
        // exceptions
        return (addr) -> {
            try {
                return ctor.newInstance(addr);
            } catch (InvocationTargetException ite) {
                GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                        "Exception in constructor for class %s: %s\n",
                        cls.getName(), ite.getCause().toString());
                return null;
            } catch (Exception e) {
                GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                        "Exception in constructor for class %s: %s\n",
                        cls.getName(), e.toString());
                return null;
            }
        };
    }

    /**
     * Return a lambda that invokes the instance initializer, with is a method
     * that is annotated with {@link InstanceInit} and takes a single parameter
     * of type {@link GObject}.
     *
     * @param  cls the class that declares the instance init method
     * @param  <T> the class must extend {@link org.gnome.gobject.GObject}
     * @return the instance initializer, or null if not found
     */
    public static <T extends GObject> Consumer<T> getInstanceInit(Class<T> cls) {
        // Find instance initializer function
        for (Method method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(InstanceInit.class)) {
                // Create a wrapper function that calls the instance
                // initializer and logs exceptions
                return (inst) -> {
                    try {
                        method.invoke(inst);
                    } catch (Exception e) {
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "Exception in %s instance init: %s\n", cls.getName(), e.toString());
                    }
                };
            }
        }
        return null;
    }

    /**
     * Return a lambda that invokes the class initializer, with is a method
     * that is annotated with {@link ClassInit} and takes a single parameter
     * of type {@link GObject.ObjectClass}.
     *
     * @param  cls  the class that declares the class init method
     * @param  <T>  the class must extend {@link org.gnome.gobject.GObject}
     * @param  <TC> the class initializer must accept a
     *              {@link GObject.ObjectClass} parameter
     * @return the class initializer, or null if not found
     */
    public static <T extends GObject, TC extends GObject.ObjectClass> Consumer<TC> getClassInit(Class<T> cls) {
        // Find class initializer function
        for (Method method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ClassInit.class)) {
                // Create a wrapper function that calls the class initializer and logs exceptions
                return (gclass) -> {
                    try {
                        method.invoke(null, gclass);
                    } catch (Exception e) {
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "Exception in %s class init: %s\n", cls.getName(), e.toString());
                    }
                };
            }
        }
        return null;
    }

    /**
     * Return a lambda that invokes the interface initializer, with is a method
     * that is annotated with {@link InterfaceInit} and takes a single
     * parameter of the type that is specified with {@code iface}.
     *
     * @param cls  the class that declares the interface init method
     * @param <T>  the class must extend {@link org.gnome.gobject.GObject}
     * @param <TI> the iface parameter must extend {@link TypeInterface}
     * @return the interface initializer, or null if not found
     */
    public static <T extends GObject, TI extends TypeInterface> Consumer<TI> getInterfaceInit(Class<T> cls, Class<?> iface) {
        // Find all overridden methods
        Class<TI> typeStruct = getTypeInterface(iface);
        if (typeStruct == null) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find TypeInterface class for interface %s\n", iface);
            return null;
        }
        var constructor = getAddressConstructor(typeStruct);
        if (constructor == null) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find constructor in TypeInterface %s\n", typeStruct);
            return null;
        }

        // Find interface initializer function
        for (Method method : cls.getDeclaredMethods()) {
            if (! method.isAnnotationPresent(InterfaceInit.class)) {
                continue;
            }
            if (! (method.getParameterTypes().length == 1)) {
                continue;
            }
            if (! method.getParameterTypes()[0].equals(typeStruct)) {
                continue;
            }
            // Create a wrapper function that calls the interface initializer
            // and logs exceptions
            return (giface) -> {
                try {
                    TI ifaceInstance = constructor.apply(giface.handle());
                    method.invoke(null, ifaceInstance);
                } catch (Exception e) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Exception in %s interface init: %s\n", cls.getName(), e.toString());
                }
            };
        }
        return null;
    }

    /**
     * Construct a {@link TypeFlags} bitfield that specifies whether the
     * provided class is abstract and/or final.
     *
     * @param  cls the class for which to generate typeflags
     * @return the generated typeflags
     */
    public static Set<TypeFlags> getTypeFlags(Class<?> cls) {
        // Set type flags
        Set<TypeFlags> flags = EnumSet.noneOf(TypeFlags.class);
        if (Modifier.isAbstract(cls.getModifiers()))
            flags.add(TypeFlags.ABSTRACT);
        if (Modifier.isFinal(cls.getModifiers()))
            flags.add(TypeFlags.FINAL);
        return flags;
    }

    /**
     * Register a new GType for a Java class. The GType will inherit from the
     * GType of the Java superclass (using {@link Class#getSuperclass()},
     * reading a {@link GType} annotated field and executing
     * {@code getMemoryLayout()} using reflection).
     * <p>
     * The name of the new GType will be the simple name of the Java class, but
     * can also be specified with the {@link RegisteredType} annotation. (All
     * invalid characters, including '.', are replaced with underscores.)
     * <p>
     * Use {@link ClassInit} and {@link InstanceInit} annotations on static
     * methods in the Java class to indicate that these are to be called during
     * GObject class- and instance initialization.
     * <p>
     * The {@link TypeFlags#ABSTRACT} and {@link TypeFlags#FINAL} flags are set
     * for abstract and final Java classes.
     *
     * @param  <T> The class must be derived from GObject
     * @return the new registered GType
     */
    public static <T extends GObject, TC extends GObject.ObjectClass> Type register(Class<T> cls) {
        if (cls == null) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Class is null\n");
            return null;
        }

        try {
            Class<?> parentClass = cls.getSuperclass();
            Type parentType = getGType(parentClass);
            String typeName = getName(cls);
            MemoryLayout classLayout = getClassLayout(cls, typeName);
            Consumer<TC> overridesInit = Overrides.overrideClassMethods(cls);
            Consumer<TC> propertiesInit = Properties.installProperties(cls);
            Consumer<TC> signalsInit = Signals.installSignals(cls);
            Consumer<TC> classInit = getClassInit(cls);
            MemoryLayout instanceLayout = getInstanceLayout(cls, typeName);
            Consumer<T> instanceInit = getInstanceInit(cls);
            Function<MemorySegment, T> constructor = getAddressConstructor(cls);
            Set<TypeFlags> flags = getTypeFlags(cls);

            if (parentType == null
                    || classLayout == null
                    || instanceLayout == null
                    || constructor == null) {
                GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                        "Cannot register type %s\n", cls.getName());
                return null;
            }

            // Generate default init function
            if (instanceInit == null)
                instanceInit = $ -> {};

            /*
             * Override virtual methods and install properties and signals before
             * running a user-defined class init. We chain the generated
             * initializers (if not null) and default to an empty method _ -> {}.
             */
            Consumer<TC> init = chain(overridesInit, propertiesInit);
            init = chain(init, signalsInit);
            init = chain(init, classInit);
            classInit = init != null ? init : $ -> {};

            // Register the GType
            Type type = register(
                    parentType,
                    typeName,
                    classLayout,
                    classInit,
                    instanceLayout,
                    instanceInit,
                    constructor,
                    flags
            );

            // Add interfaces
            try (var arena = Arena.ofConfined()) {
                for (Class<?> iface : cls.getInterfaces()) {
                    if (Proxy.class.isAssignableFrom(iface)) {
                        Type ifaceType = getGType(iface);
                        if (ifaceType == null) {
                            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                    "Cannot implement interface %s on class %s: No GType\n",
                                    iface.getName(), cls.getName());
                            continue;
                        }

                        InterfaceInfo interfaceInfo = new InterfaceInfo(arena);
                        Consumer<TypeInterface> ifaceOverridesInit = Overrides.overrideInterfaceMethods(cls, iface);
                        Consumer<TypeInterface> ifaceInit = getInterfaceInit(cls, iface);

                        // Override virtual methods before running a user-defined
                        // interface init
                        ifaceInit = chain(ifaceOverridesInit, ifaceInit);
                        if (ifaceInit == null) {
                            ifaceInit = $ -> {};
                        }

                        Consumer<TypeInterface> finalIfaceInit = ifaceInit;
                        interfaceInfo.writeInterfaceInit((ti, data) -> finalIfaceInit.accept(ti), Arena.global());
                        GObjects.typeAddInterfaceStatic(type, ifaceType, interfaceInfo);
                    }
                }
            }
            return type;

        } catch (Exception e) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot register type %s: %s\n", cls.getName(), e.toString());
            return null;
        }
    }

    /**
     * Register a new GType.
     *
     * @param  parentType     parent GType
     * @param  typeName       name of the GType
     * @param  classLayout    memory layout of the typeclass
     * @param  classInit      static class initializer function
     * @param  instanceLayout memory layout of the typeinstance
     * @param  instanceInit   static instance initializer function
     * @param  constructor    memory-address constructor
     * @param  flags          type flags
     * @param  <T>            the instance initializer function must accept the
     *                        result of the memory address constructor
     * @param  <TC>           the class initializer function must accept a
     *                        parameter that is a subclass of TypeClass
     * @return the new GType
     */
    public static <T extends GObject, TC extends GObject.ObjectClass> Type register(
            org.gnome.glib.Type parentType,
            String typeName,
            MemoryLayout classLayout,
            Consumer<TC> classInit,
            MemoryLayout instanceLayout,
            Consumer<T> instanceInit,
            Function<MemorySegment, T> constructor,
            Set<TypeFlags> flags
    ) {
        @SuppressWarnings("unchecked")
        Type type = GObjects.typeRegisterStaticSimple(
                parentType,
                typeName,
                (short) classLayout.byteSize(),
                // The data parameter is not used.
                (typeClass, data) -> classInit.accept((TC) typeClass),
                (short) instanceLayout.byteSize(),
                // The instance parameter is a type-instance of T, so construct a T proxy instance.
                // The typeClass parameter is not used.
                (instance, typeClass) -> {
                    // The instance is initially cached as TypeInstance.
                    // Overwrite it with a new T instance, and run init().
                    T newInstance = constructor.apply(instance.handle());
                    InstanceCache.put(newInstance.handle(), newInstance);
                    instanceInit.accept(newInstance);
                },
                flags
        );
        // Register the type and constructor in the cache
        TypeCache.register(type, constructor);
        return type;
    }

    /**
     * Null-safe run {@code first.andThen(second)}.
     * <ul>
     *   <li>When both lambdas are not null: return first.andThen(second)
     *   <li>When only first is not null:    return first
     *   <li>When only second is not null:   return second
     *   <li>When both lambdas are null:     return null
     * </ul>
     *
     * @param  first  the first Consumer to run
     * @param  second the Consumer to run after the first
     * @param  <Z>    both Consumers must have the same type signature
     * @return a Consumer that runs the first and second lambdas, or
     *         {@code null} if both arguments are {@code null}
     */
    public static <Z> Consumer<Z> chain(Consumer<Z> first, Consumer<Z> second) {
        if (first != null && second != null) {
            return first.andThen(second);
        }
        return first != null ? first : second;
    }
}
