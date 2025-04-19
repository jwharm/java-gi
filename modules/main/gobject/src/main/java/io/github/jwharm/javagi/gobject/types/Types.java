/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 Jan-Willem Harmannij
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
import io.github.jwharm.javagi.interop.Interop;
import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.glib.Type;
import org.gnome.gobject.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.github.jwharm.javagi.Constants.LOG_DOMAIN;
import static java.util.Objects.requireNonNull;
import static org.gnome.gobject.GObjects.typeTestFlags;

/**
 * The Types class contains GType constants, a series of static methods to
 * check gtype characteristics, and static methods to register a Java class as
 * a new GObject-derived GType.
 */
@SuppressWarnings("unused")
public class Types {

    static {
        GObjects.javagi$ensureInitialized();
    }

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
    public static final Type HASH_TABLE =
            Interop.getType("g_hash_table_get_type");

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
    public static final Type MATCH_INFO =
            Interop.getType("g_match_info_get_type");

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
    public static final Type BYTE_ARRAY =
            Interop.getType("g_byte_array_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GPtrArray}
     * reference.
     *
     * @since 2.22
     */
    public static final Type PTR_ARRAY =
            Interop.getType("g_ptr_array_get_type");

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
    public static final Type VARIANT_TYPE =
            Interop.getType("g_variant_type_get_gtype");

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
    public static final Type DATE_TIME =
            Interop.getType("g_date_time_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GTimeZone}.
     *
     * @since 2.34
     */
    public static final Type TIME_ZONE =
            Interop.getType("g_time_zone_get_type");

    /**
     * The {@code GType} for {@code GIOChannel}.
     */
    public static final Type IO_CHANNEL =
            Interop.getType("g_io_channel_get_type");

    /**
     * The {@code GType} for {@code GIOCondition}.
     */
    public static final Type IO_CONDITION =
            Interop.getType("g_io_condition_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GVariantBuilder}.
     *
     * @since 2.30
     */
    public static final Type VARIANT_BUILDER =
            Interop.getType("g_variant_builder_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GVariantDict}.
     *
     * @since 2.40
     */
    public static final Type VARIANT_DICT =
            Interop.getType("g_variant_dict_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GMainLoop}.
     *
     * @since 2.30
     */
    public static final Type MAIN_LOOP =
            Interop.getType("g_main_loop_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GMainContext}.
     *
     * @since 2.30
     */
    public static final Type MAIN_CONTEXT =
            Interop.getType("g_main_context_get_type");

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
    public static final Type MARKUP_PARSE_CONTEXT =
            Interop.getType("g_markup_parse_context_get_type");

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
    public static final Type MAPPED_FILE =
            Interop.getType("g_mapped_file_get_type");

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
    public static final Type OPTION_GROUP =
            Interop.getType("g_option_group_get_type");

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
    public static final Type PATTERN_SPEC =
            Interop.getType("g_pattern_spec_get_type");

    /**
     * The {@code GType} for a boxed type holding a {@code GBookmarkFile}.
     *
     * @since 2.76
     */
    public static final Type BOOKMARK_FILE =
            Interop.getType("g_bookmark_file_get_type");

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
     * @param  type A {@link Type} value
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
     * @param  type A {@link Type} value
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
     * @param  type A {@link Type} value
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
     * @param  type A {@link Type} value
     * @return {@code true} if {@code type} is classed
     */
    public static boolean IS_CLASSED(Type type) {
        return typeTestFlags(type, TypeFundamentalFlags.CLASSED.getValue());
    }

    /**
     * Checks if {@code type} can be instantiated. Instantiation is the
     * process of creating an instance (object) of this type.
     *
     * @param  type A {@link Type} value
     * @return {@code true} if {@code type} is instantiable
     */
    public static boolean IS_INSTANTIATABLE(Type type) {
        return typeTestFlags(type,
                             TypeFundamentalFlags.INSTANTIATABLE.getValue());
    }

    /**
     * Checks if {@code type} is a derivable type.  A derivable type can
     * be used as the base class of a flat (single-level) class hierarchy.
     *
     * @param  type A {@link Type} value
     * @return {@code true} if {@code type} is derivable
     */
    public static boolean IS_DERIVABLE(Type type) {
        return typeTestFlags(type, TypeFundamentalFlags.DERIVABLE.getValue());
    }

    /**
     * Checks if {@code type} is a deep derivable type.  A deep derivable type
     * can be used as the base class of a deep (multi-level) class hierarchy.
     *
     * @param  type A {@link Type} value
     * @return {@code true} if {@code type} is deep derivable
     */
    public static boolean IS_DEEP_DERIVABLE(Type type) {
        return typeTestFlags(type,
                             TypeFundamentalFlags.DEEP_DERIVABLE.getValue());
    }

    /**
     * Checks if {@code type} is an abstract type.  An abstract type cannot be
     * instantiated and is normally used as an abstract base class for
     * derived classes.
     *
     * @param  type A {@link Type} value
     * @return {@code true} if {@code type} is abstract
     */
    public static boolean IS_ABSTRACT(Type type) {
        return typeTestFlags(type, TypeFlags.ABSTRACT.getValue());
    }

    /**
     * Checks if {@code type} is an abstract value type.  An abstract value
     * type introduces a value table, but can't be used for g_value_init() and
     * is normally used as an abstract base type for derived value types.
     *
     * @param  type A {@link Type} value
     * @return {@code true} if {@code type} is an abstract value type
     */
    public static boolean IS_VALUE_ABSTRACT(Type type) {
        return typeTestFlags(type, TypeFlags.VALUE_ABSTRACT.getValue());
    }

    /**
     * Checks if {@code type} is a value type and can be used with
     * g_value_init().
     *
     * @param  type A {@link Type} value
     * @return {@code true} if {@code type} is a value type
     */
    public static boolean IS_VALUE_TYPE(Type type) {
        return GObjects.typeCheckIsValueType(type);
    }

    /**
     * Checks if {@code type} has a {@link TypeValueTable}.
     *
     * @param  type A {@link Type} value
     * @return {@code true} if {@code type} has a value table
     */
    public static boolean HAS_VALUE_TABLE(Type type) {
        return TypeValueTable.peek(type) != null;
    }

    /**
     * Checks if {@code type} is a final type. A final type cannot be derived
     * any further.
     *
     * @param  type A {@link Type} value
     * @return {@code true} if {@code type} is final
     * @since  2.70
     */
    public static boolean IS_FINAL(Type type) {
        return typeTestFlags(type, TypeFlags.FINAL.getValue());
    }

    /**
     * Checks if {@code type} is deprecated. Instantiating a deprecated type
     * will trigger a warning if running with {@code G_ENABLE_DIAGNOSTIC=1}.
     *
     * @param  type A {@link Type} value
     * @return {@code true} if the type is deprecated
     * @since  2.76
     */
    public static boolean IS_DEPRECATED(Type type) {
        return typeTestFlags(type, TypeFlags.DEPRECATED.getValue());
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
    private static String getName(Class<?> cls) {
        // Default type name: fully qualified Java class name
        String typeNameInput = cls.getName();
        String namespace = "";

        // Check for a Namespace annotation on the package
        if (cls.getPackage().isAnnotationPresent(Namespace.class)) {
            var annotation = cls.getPackage().getAnnotation(Namespace.class);
            namespace = annotation.name();
            typeNameInput = namespace + cls.getSimpleName();
        }

        // Check for an annotation that overrides the type name
        if (cls.isAnnotationPresent(RegisteredType.class)) {
            var annotation = cls.getAnnotation(RegisteredType.class);
            if (! "".equals(annotation.name())) {
                typeNameInput = namespace + annotation.name();
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
     * @param  <T>      the class must extend {@link TypeInstance}
     * @return the declared memory layout, or if not found, a generated memory
     *         layout that copies the memory layout declared in the direct
     *         superclass.
     */
    public static <T extends TypeInstance>
    MemoryLayout getInstanceLayout(Class<T> cls, String typeName) {

            // Get instance-memorylayout of this class
            MemoryLayout instanceLayout = getLayout(cls);
            if (instanceLayout != null)
                return instanceLayout;

            // If no memory layout was defined, create a default memory layout
            // that only has a pointer to the parent class' memory layout.
            MemoryLayout parentLayout = getLayout(cls.getSuperclass());

            if (parentLayout == null) {
                GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                        "Cannot find memory layout definition for class %s\n",
                        cls.getSimpleName());
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
     * @return the TypeClass class, or null if not found
     */
    @SuppressWarnings("unchecked")
    public static <TC extends TypeClass> Class<TC> getTypeClass(Class<?> cls) {

        // Get the type-struct. This is an inner class that extends ObjectClass.
        for (Class<?> gclass : cls.getDeclaredClasses()) {
            if (TypeClass.class.isAssignableFrom(gclass)) {
                return (Class<TC>) gclass;
            }
        }
        // If the type-struct is unavailable, get it from the parent class.
        if (cls.getSuperclass() != null) {
            for (Class<?> gclass : cls.getSuperclass().getDeclaredClasses()) {
                if (TypeClass.class.isAssignableFrom(gclass)) {
                    return (Class<TC>) gclass;
                }
            }
        }
        return null;
    }

    /**
     * Return the inner TypeInterface class, or null if not found.
     *
     * @param  iface the interface that contains an inner TypeInterface class
     * @param  <TI>  the returned class extends {@link TypeInterface}
     * @return the TypeInterface class, or TypeInterface.class if not found
     */
    @SuppressWarnings("unchecked")
    public static <TI extends TypeInterface> Class<TI> getTypeInterface(Class<?> iface) {
        // Get the type-struct. This is an inner class that extends TypeInterface.
        for (Class<?> giface : iface.getClasses()) {
            if (TypeInterface.class.isAssignableFrom(giface)) {
                return (Class<TI>) giface;
            }
        }
        return (Class<TI>) TypeInterface.class;
    }

    /**
     * Generate a MemoryLayout struct with one member: the memory layout of the
     * parent TypeClass
     *
     * @param  cls      the class to generate a memory layout for
     * @param  typeName the name of the new memory layout
     * @return the requested memory layout
     */
    public static MemoryLayout generateClassLayout(Class<?> cls, String typeName) {
        // Get the type-struct. This is an inner class that extends TypeClass.
        // If the type-struct is unavailable, get it from the parent class.
        Class<?> typeClass = getTypeClass(cls);
        requireNonNull(typeClass,
                "No TypeClass for class " + cls.getSimpleName());

        // Get memory layout of the type-struct
        MemoryLayout parentLayout = getLayout(typeClass);
        requireNonNull(parentLayout,
                "No memory layout for class " + typeClass.getSimpleName());

        return MemoryLayout.structLayout(
                parentLayout.withName("parent_class")
        ).withName(typeName + "Class");
    }

    /**
     * Generate a MemoryLayout struct with one member: the memory layout of
     * GTypeInterface
     *
     * @param  cls      the interface to generate a memory layout for
     * @param  typeName the name of the new memory layout
     * @return the requested memory layout
     */
    public static MemoryLayout generateIfaceLayout(Class<?> cls, String typeName) {
        // Get the type-struct. This is an inner class that extends TypeInterface.
        // If the type-struct is unavailable, get it from the parent class.
        Class<? extends TypeInterface> typeIface = getTypeInterface(cls);
        requireNonNull(typeIface,
                "No TypeInterface for interface " + cls.getSimpleName());

        // Get memory layout of the type-struct
        MemoryLayout parentLayout = getLayout(typeIface);
        requireNonNull(parentLayout,
                "No memory layout for interface " + typeIface.getSimpleName());

        return MemoryLayout.structLayout(
                parentLayout.withName("g_iface")
        ).withName(typeName + "Interface");
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
        for (Method m : cls.getDeclaredMethods()) {
            if (m.isAnnotationPresent(Layout.class)) {
                // Check method signature
                if ((m.getParameterTypes().length != 0)
                        || (! m.getReturnType().equals(MemoryLayout.class))) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Method %s.%s does not have expected signature () -> MemoryLayout\n",
                            cls.getSimpleName(), m.getName());
                    return null;
                }
                // Invoke the @MemoryLayout-annotated method and return the
                // result
                try {
                    return (MemoryLayout) m.invoke(null);
                } catch (IllegalAccessException e) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "IllegalAccessException when calling %s.%s\n",
                            cls.getSimpleName(), m.getName());
                    return null;
                } catch (InvocationTargetException e) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Exception when calling %s.%s: %s\n",
                            cls.getSimpleName(),
                            m.getName(),
                            e.getTargetException().toString());
                    return null;
                }
            }
        }

        // Find a method {@code public static MemoryLayout getMemoryLayout()}
        // and execute it
        try {
            // invoke getMemoryLayout() on the class
            Method method = cls.getDeclaredMethod("getMemoryLayout");
            return (MemoryLayout) method.invoke(null);

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
    public static <T extends Proxy>
    Function<MemorySegment, T> getAddressConstructor(Class<T> cls) {

        Constructor<T> ctor;
        try {
            // Get memory address constructor
            ctor = cls.getConstructor(MemorySegment.class);
        } catch (NoSuchMethodException e) {
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
                        cls.getSimpleName(), ite.getTargetException().toString());
                return null;
            } catch (Exception e) {
                GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                        "Exception in constructor for class %s: %s\n",
                        cls.getSimpleName(), e.toString());
                return null;
            }
        };
    }

    /**
     * Return a lambda that invokes the instance initializer, with is a method
     * that is annotated with {@link InstanceInit} and takes a single parameter
     * of type {@link TypeInstance}.
     *
     * @param  cls the class that declares the instance init method
     * @param  <T> the class must extend {@link TypeInstance}
     * @return the instance initializer, or a no-op ({@code $ -> {}}) if not
     *         found
     */
    public static <T extends TypeInstance>
    Consumer<T> getInstanceInit(Class<T> cls) {

        // Find instance initializer function
        for (Method method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(InstanceInit.class)) {
                // Create a wrapper function that calls the instance
                // initializer and logs exceptions
                return (inst) -> {
                    try {
                        method.invoke(inst);
                    } catch (InvocationTargetException ite) {
                        Throwable t = ite.getTargetException();
                        if (t instanceof ExceptionInInitializerError eiie) {
                            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                    "ExceptionInInitializerError in %s instance init: %s\n",
                                    cls.getSimpleName(),
                                    eiie.getCause().toString());
                        } else {
                            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                    "InvocationTargetException in %s instance init: %s\n",
                                    cls.getSimpleName(),
                                    ite.getTargetException().toString());
                        }
                    } catch (Exception e) {
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "Exception in %s instance init: %s\n",
                                cls.getSimpleName(),
                                e.toString());
                    }
                };
            }
        }
        return $ -> {};
    }

    /**
     * Return a lambda that invokes the class initializer, with is a method
     * that is annotated with {@link ClassInit} and takes a single parameter
     * of type {@link TypeClass}.
     *
     * @param  cls  the class that declares the class init method
     * @param  <T>  the class must extend {@link TypeInstance}
     * @param  <TC> the class initializer must accept a {@link TypeClass}
     *              parameter
     * @return the class initializer, or null if not found
     */
    public static <T extends TypeInstance, TC extends TypeClass>
    Consumer<TC> getClassInit(Class<T> cls) {
        // Find class initializer function
        for (Method method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ClassInit.class)) {
                // Create a wrapper function that calls the class initializer
                // and logs exceptions
                return (gclass) -> {
                    try {
                        method.invoke(null, gclass);
                    } catch (InvocationTargetException ite) {
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "Exception in %s class init: %s\n",
                                cls.getSimpleName(),
                                ite.getTargetException().toString());
                    } catch (Exception e) {
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "Exception in %s class init: %s\n",
                                cls.getSimpleName(),
                                e.toString());
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
     * @param <T>  the class must extend {@link TypeInstance}
     * @param <TI> the iface parameter must extend {@link TypeInterface}
     * @return the interface initializer, or null if not found
     */
    public static <T extends TypeInstance, TI extends TypeInterface>
    Consumer<TI> getInterfaceInit(Class<T> cls, Class<?> iface) {
        // Find all overridden methods
        Class<TI> typeStruct = getTypeInterface(iface);
        if (typeStruct == null) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find TypeInterface class for interface %s\n",
                    iface);
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
                    method.invoke(null, giface);
                } catch (Exception e) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Exception in %s interface init: %s\n",
                            cls.getSimpleName(), e.toString());
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
     * Get a list of interface prerequisites. These are all interfaces directly
     * extended by this interface, and all classes listed in the prerequisites
     * argument of the RegisteredType annotation.
     * <p>
     * When the interface declares properties and/or signals, and GObject or a
     * GObject-derived class is not a prerequisite, it is added automatically.
     *
     * @param  iface the interface for which to list the prerequisites
     * @return the list of prerequisites
     */
    private static List<Class<?>> getPrerequisites(Class<?> iface) {
        var list = new ArrayList<Class<?>>();
        for (var prerequisite : iface.getInterfaces()) {
            if (! prerequisite.equals(Proxy.class)) // Skip Proxy interface
                list.add(prerequisite);
        }
        var gobjectBased = false;

        if (iface.isAnnotationPresent(RegisteredType.class)) {
            var registeredType = iface.getAnnotation(RegisteredType.class);
            for (var prereq : registeredType.prerequisites()) {
                if (GObject.class.isAssignableFrom(prereq))
                    gobjectBased = true;
                list.add(prereq);
            }
        }

        // If the interface declares properties or signals, automatically add
        // GObject as a prerequisite.
        if (!gobjectBased) {
            // Find property declarations
            for (var method : iface.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Property.class)) {
                    list.add(GObject.class);
                    return list;
                }
            }

            // Find signal declarations
            for (var cls : iface.getDeclaredClasses()) {
                if (cls.isInterface() && cls.isAnnotationPresent(Signal.class)) {
                    list.add(GObject.class);
                    return list;
                }
            }
        }

        return list;
    }

    /**
     * Check if this class (or in case of an interface, any class that
     * implements it) is a GObject.
     *
     * @param  cls the class or interface to check
     * @return whether this is a class that extends GObject or an interface
     *         that has GObject as a prerequisite
     */
    public static boolean isGObjectBased(Class<?> cls) {
        // Class that extends GObject
        if (GObject.class.isAssignableFrom(cls))
            return true;

        // Interface with GObject as prerequisite
        if (cls.isInterface() && cls.isAnnotationPresent(RegisteredType.class)) {
            var annotation = cls.getAnnotation(RegisteredType.class);
            for (var prerequisite : annotation.prerequisites())
                if (GObject.class.isAssignableFrom(prerequisite))
                    return true;
        }
        return false;
    }

    /*
     * Perform sanity checks on the class that will be registered
     */
    private static void checkClassDefinition(Class<?> cls) {
        if (cls == null)
            throw new IllegalArgumentException("Class is null");

        if (cls.isAnnotationPresent(RegisteredType.class)) {
            var annotation = cls.getAnnotation(RegisteredType.class);
            if (annotation.prerequisites().length > 0 && !cls.isInterface())
                throw new IllegalArgumentException("Prerequisites can only be applied on interfaces");
        }

        if (cls.isAnnotationPresent(Flags.class) && !cls.isEnum())
            throw new IllegalArgumentException("Only enums can be a flags type");
    }

    /**
     * Register a new GType for a Java class, interface or enum.
     * <ul>
     *     <li>For classes, the GType will inherit from the GType of the Java
     *     superclass, and will implement all interfaces that are implemented
     *     by the Java class.
     *     <li>Interfaces will be registered as a GType that inherits from
     *     {@link TypeInterface}. It is possible to specify prerequisite types
     *     using the {@link RegisteredType} annotation. When no prerequisites
     *     are set, GObject is by default set as a prerequisite.
     *     <li>Enums will be registered as a GObject enum type. When a flags
     *     type is preferred, add the {@link Flags} annotation on the enum.
     * </ul>
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
     * @param  cls the class, interface or enumeration to register
     * @return the GType of the registered Java type
     */
    public static Type register(Class<?> cls) {
        if (TypeCache.contains(cls))
            return TypeCache.getType(cls);

        checkClassDefinition(cls);

        if (Enum.class.isAssignableFrom(cls)) {
            @SuppressWarnings("unchecked") // checked by isAssignableFrom()
            var enumClass = (Class<? extends Enum<?>>) cls;
            if (enumClass.isAnnotationPresent(Flags.class))
                return registerFlags(enumClass);
            else
                return registerEnum(enumClass);
        }

        // Assert that `cls` is a Proxy class
        if (! Proxy.class.isAssignableFrom(cls)) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Class does not implement Proxy interface\n");
            return null;
        }
        @SuppressWarnings("unchecked") // checked by isAssignableFrom()
        var proxy = (Class<? extends Proxy>) cls;

        try {
            Class<?> parentClass = cls.getSuperclass();
            Type parentType = cls.isInterface() ? INTERFACE
                    : TypeCache.getType(parentClass);
            String typeName = getName(cls);

            // Generate memory layout
            MemoryLayout memoryLayout = cls.isInterface()
                    ? generateIfaceLayout(cls, typeName)
                    : generateClassLayout(cls, typeName);

            // Create initialization function that registers method overrides
            var overridesInit = Overrides.overrideClassMethods(cls);

            // GObject class initializers for properties and signals
            Consumer<TypeClass> propertiesInit;
            Consumer<TypeClass> signalsInit;
            if (isGObjectBased(cls)) {
                signalsInit = Signals.installSignals(cls);
                propertiesInit = new Properties().installProperties(cls);
            } else {
                signalsInit = null;
                propertiesInit = null;
            }

            // Initialization methods that are only applicable to classes,
            // and `null` for interfaces
            Consumer<TypeClass> customClassInit;
            MemoryLayout instanceLayout;
            Consumer<TypeInstance> instanceInit;
            if (TypeInstance.class.isAssignableFrom(cls)) {
                @SuppressWarnings("unchecked") // checked by isAssignableFrom()
                var typeInstance = (Class<TypeInstance>) cls;
                customClassInit = getClassInit(typeInstance);
                instanceLayout = getInstanceLayout(typeInstance, typeName);
                instanceInit = getInstanceInit(typeInstance);
            } else {
                customClassInit = null;
                instanceLayout = null;
                instanceInit = $ -> {};
            }

            var constructor = getAddressConstructor(proxy);
            if (constructor == null && cls.isInterface())
                constructor = getAddressConstructor(TypeInterface.class);

            Set<TypeFlags> flags = getTypeFlags(cls);

            if (memoryLayout == null
                    || ((!cls.isInterface()) && instanceLayout == null)) {
                GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                        "Cannot register type %s\n", cls.getSimpleName());
                return null;
            }

            // Override virtual methods and install properties and signals
            // before running a user-defined class init.
            Consumer<TypeClass> classInit = typeClass -> {
                applyIfNotNull(overridesInit, typeClass);
                if (typeClass instanceof GObject.ObjectClass oc) {
                    applyIfNotNull(propertiesInit, oc);
                }
                applyIfNotNull(signalsInit, typeClass);
                applyIfNotNull(customClassInit, typeClass);
            };

            // Get parent TypeClass constructor
            var typeClassConstructor = TypeCache.getTypeClassConstructor(parentType);

            // Register the GType
            Type type;
            if (cls.isInterface()) {
                type = registerInterface(cls, typeName, memoryLayout, classInit,
                        constructor, typeClassConstructor, flags);
            } else {
                type = register(parentType, cls, typeName, memoryLayout, classInit,
                        instanceLayout, instanceInit, constructor,
                        typeClassConstructor, flags);

                try {
                    @SuppressWarnings("unchecked") // ClassCastException handled below
                    Class<TypeInstance> typeInstanceClass = (Class<TypeInstance>) cls;

                    // Add interfaces
                    try (var arena = Arena.ofConfined()) {
                        for (Class<?> iface : cls.getInterfaces()) {
                            if (Proxy.class.isAssignableFrom(iface)) {
                                Type ifaceType;
                                try {
                                    ifaceType = TypeCache.getType(iface);
                                } catch (IllegalArgumentException iae) {
                                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                            "Cannot implement interface %s on class %s: No GType\n",
                                            iface.getSimpleName(), cls.getSimpleName());
                                    continue;
                                }

                                InterfaceInfo interfaceInfo = new InterfaceInfo(arena);
                                Consumer<TypeInterface> ifaceOverridesInit =
                                        Overrides.overrideInterfaceMethods(typeInstanceClass, iface);
                                Consumer<TypeInterface> customIfaceInit =
                                        getInterfaceInit(typeInstanceClass, iface);

                                // Override virtual methods before running a user-defined
                                // interface init
                                Consumer<TypeInterface> ifaceInit = typeIface -> {
                                    applyIfNotNull(ifaceOverridesInit, typeIface);
                                    applyIfNotNull(customIfaceInit, typeIface);
                                };

                                interfaceInfo.writeInterfaceInit((ti, data) ->
                                        ifaceInit.accept(ti), Arena.global());
                                GObjects.typeAddInterfaceStatic(
                                        type, ifaceType, interfaceInfo);
                            }
                        }
                    }
                } catch (ClassCastException cce) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Class %s does not derive from TypeInstance\n",
                            cls.getSimpleName());
                    return null;
                }
            }
            return type;

        } catch (Exception e) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot register type %s: %s\n",
                    cls.getSimpleName(), e.toString());
            return null;
        }
    }

    /**
     * Register a new interface type.
     *
     * @param  cls             the interface to register
     * @param  typeName        name of the GType
     * @param  interfaceLayout memory layout of the interface
     * @param  classInit       static class initializer function
     * @param  ctor            memory-address constructor
     * @param  flags           type flags
     * @return the GType of the registered interface
     */
    private static Type registerInterface(Class<?> cls,
                                          String typeName,
                                          MemoryLayout interfaceLayout,
                                          Consumer<TypeClass> classInit,
                                          Function<MemorySegment, ? extends Proxy> ctor,
                                          Function<MemorySegment, ? extends Proxy> typeClassCtor,
                                          Set<TypeFlags> flags) {
        TypeInfo typeInfo = new TypeInfo(
                (short) interfaceLayout.byteSize(),
                null,       // base_init
                null,       // base_finalize
                (typeClass, data) -> classInit.accept(typeClass),
                null,       // class_finalize
                null,       // class_data
                (short) 0,  // instance_size
                (short) 0,  // n_preallocs
                null,       // instance_init
                null,       // value_table
                Arena.global());
        Type type = GObjects.typeRegisterStatic(
                INTERFACE, typeName, typeInfo, flags);

        // Add prerequisites
        for (var prerequisite : getPrerequisites(cls)) {
            var ptype = TypeCache.getType(prerequisite);
            TypeInterface.addPrerequisite(type, ptype);
        }

        // Register the type and constructor in the cache
        TypeCache.register(cls, type, ctor, typeClassCtor);
        return type;
    }

    /**
     * Register a new enumeration type.
     *
     * @param  cls the class (must extend java.lang.Enum)
     * @return the GType of the registered enumeration
     */
    private static Type registerEnum(Class<? extends Enum<?>> cls) {
        var name = getName(cls);
        var constants = (Enum<?>[]) cls.getEnumConstants();
        var enumValues = new EnumValue[constants.length];
        int i = 0;
        for (var constant : constants) {
            enumValues[i++] = new EnumValue(
                    constant.ordinal(), constant.name(), constant.name(), Arena.global());
        }
        var type = enumRegisterStatic(name, enumValues);
        TypeCache.register(cls, type, null, TypeInterface::new);
        return type;
    }

    /**
     * Register a new flags type.
     *
     * @param  cls the class (must extend java.lang.Enum)
     * @return the GType of the registered enumeration
     */
    private static Type registerFlags(Class<? extends Enum<?>> cls) {
        var name = getName(cls);
        var constants = (Enum<?>[]) cls.getEnumConstants();
        var flagsValues = new FlagsValue[constants.length];
        int i = 0;
        for (var constant : constants) {
            flagsValues[i++] = new FlagsValue(
                    1 << constant.ordinal(), constant.name(), constant.name(), Arena.global());
        }
        var type = flagsRegisterStatic(name, flagsValues);
        TypeCache.register(cls, type, null, null);
        return type;
    }

    /**
     * Based on {@link GObjects#enumRegisterStatic}, but will allocate memory
     * in the global arena.
     *
     * @param name              the name of the new type
     * @param constStaticValues An array of {@code GEnumValue} structs for the
     *                          possible enumeration values. The array is
     *                          terminated by a struct with all members being 0.
     *                          GObject keeps a reference to the data, so it
     *                          must be allocated in the global arena.
     * @return The new type identifier
     */
    private static Type enumRegisterStatic(String name, EnumValue[] constStaticValues) {
        long _result;
        try {
            MemorySegment pName = Interop.allocateNativeString(name, Arena.global());
            MemorySegment pValues = Interop.allocateNativeArray(
                    constStaticValues, EnumValue.getMemoryLayout(), true, Arena.global());
            _result = (long) g_enum_register_static.invokeExact(pName, pValues);
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        return new Type(_result);
    }

    private static final MethodHandle g_enum_register_static = Interop.downcallHandle(
            "g_enum_register_static", FunctionDescriptor.of(ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS), false);

    /**
     * Based on {@link GObjects#flagsRegisterStatic}, but will allocate memory
     * in the global arena.
     *
     * @param name              the name of the new type
     * @param constStaticValues An array of {@code GFlagsValue} structs for the
     *                          possible flags values. The array is terminated
     *                          by a struct with all members being 0.
     *                          GObject keeps a reference to the data, so it
     *                          must be allocated in the global arena.
     * @return The new type identifier
     */
    private static Type flagsRegisterStatic(String name, FlagsValue[] constStaticValues) {
        long _result;
        try {
            MemorySegment pName = Interop.allocateNativeString(name, Arena.global());
            MemorySegment pValues = Interop.allocateNativeArray(
                    constStaticValues, FlagsValue.getMemoryLayout(), true, Arena.global());
            _result = (long) g_flags_register_static.invokeExact(pName, pValues);
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        return new Type(_result);
    }

    private static final MethodHandle g_flags_register_static = Interop.downcallHandle(
            "g_flags_register_static", FunctionDescriptor.of(ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS, ValueLayout.ADDRESS), false);

    /**
     * Register a new GType.
     *
     * @param  parentType     parent GType
     * @param  typeName       name of the GType
     * @param  classLayout    memory layout of the typeclass
     * @param  classInit      static class initializer function
     * @param  instanceLayout memory layout of the typeinstance
     * @param  instanceInit   static instance initializer function
     * @param  flags          type flags
     * @return the GType of the registered Java type
     */
    public static Type register(Type parentType,
                  Class<?> javaClass,
                  String typeName,
                  MemoryLayout classLayout,
                  Consumer<TypeClass> classInit,
                  MemoryLayout instanceLayout,
                  Consumer<TypeInstance> instanceInit,
                  Function<MemorySegment, ? extends Proxy> constructor,
                  Function<MemorySegment, ? extends Proxy> typeClassConstructor,
                  Set<TypeFlags> flags) {

        Type type = GObjects.typeRegisterStaticSimple(
                parentType,
                typeName,
                (short) classLayout.byteSize(),
                (typeClass, data) -> classInit.accept(typeClass),
                (short) instanceLayout.byteSize(),
                (instance, typeClass) -> instanceInit.accept(instance),
                flags
        );
        // Register the type and constructor in the cache
        TypeCache.register(javaClass, type, constructor, typeClassConstructor);
        return type;
    }

    /**
     * Apply {@code func} to {@code cls} if {@code func} is not {@code null}.
     *
     * @param func a nullable Consumer operation that will be applied to
     *             {@code cls}
     * @param cls  the Class that will be passed to {@code func}
     * @param <Z>  {@code func} must accept the type of {@code cls} as its
     *             parameter
     */
    public static <Z> void applyIfNotNull(@Nullable Consumer<? super Z> func, @NotNull Z cls) {
        if (func != null)
            func.accept(cls);
    }
}
