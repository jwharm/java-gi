package io.github.jwharm.javagi.types;

import org.gnome.glib.Variant;
import org.gnome.glib.VariantType;

/**
 * This class contains G_VARIANT_TYPE_... declarations and conversion functions from
 * Java classes to GVariant types and vice versa.
 */
public class VariantTypes {

    // GVariant types, adapted from <glib/gvarianttype.h>

    /**
     * The type of a value that can be either {@code true} or {@code false}.
     */
    public static final VariantType BOOLEAN              = new VariantType("b");

    /**
     * The type of an integer value that can range from 0 to 255.
     **/
    public static final VariantType BYTE                 = new VariantType("y");

    /**
     * The type of an integer value that can range from -32768 to 32767.
     **/
    public static final VariantType INT16                = new VariantType("n");

    /**
     * The type of an integer value that can range from 0 to 65535.
     * There were about this many people living in Toronto in the 1870s.
     **/
    public static final VariantType UINT16               = new VariantType("q");

    /**
     * The type of an integer value that can range from -2147483648 to
     * 2147483647.
     **/
    public static final VariantType INT32                = new VariantType("i");

    /**
     * The type of an integer value that can range from 0 to 4294967295.
     * That's one number for everyone who was around in the late 1970s.
     **/
    public static final VariantType UINT32               = new VariantType("u");

    /**
     * The type of an integer value that can range from
     * -9223372036854775808 to 9223372036854775807.
     **/
    public static final VariantType INT64                = new VariantType("x");

    /**
     * The type of an integer value that can range from 0
     * to 18446744073709551615 (inclusive).  That's a really big number,
     * but a Rubik's cube can have a bit more than twice as many possible
     * positions.
     **/
    public static final VariantType UINT64               = new VariantType("t");

    /**
     * The type of a double precision IEEE754 floating point number.
     * These guys go up to about 1.80e308 (plus and minus) but miss out on
     * some numbers in between.  In any case, that's far greater than the
     * estimated number of fundamental particles in the observable
     * universe.
     **/
    public static final VariantType DOUBLE               = new VariantType("d");

    /**
     * The type of a string.  "" is a string.  {@code null} is not a string.
     **/
    public static final VariantType STRING               = new VariantType("s");

    /**
     * The type of a D-Bus object reference.  These are strings of a
     * specific format used to identify objects at a given destination on
     * the bus.
     *
     * If you are not interacting with D-Bus, then there is no reason to make
     * use of this type.  If you are, then the D-Bus specification contains a
     * precise description of valid object paths.
     **/
    public static final VariantType OBJECT_PATH          = new VariantType("o");

    /**
     * The type of a D-Bus type signature.  These are strings of a specific
     * format used as type signatures for D-Bus methods and messages.
     *
     * If you are not interacting with D-Bus, then there is no reason to make
     * use of this type.  If you are, then the D-Bus specification contains a
     * precise description of valid signature strings.
     **/
    public static final VariantType SIGNATURE            = new VariantType("g");

    /**
     * The type of a box that contains any other value (including another
     * variant).
     **/
    public static final VariantType VARIANT              = new VariantType("v");

    /**
     * The type of a 32bit signed integer value, that by convention, is used
     * as an index into an array of file descriptors that are sent alongside
     * a D-Bus message.
     *
     * If you are not interacting with D-Bus, then there is no reason to make
     * use of this type.
     **/
    public static final VariantType HANDLE               = new VariantType("h");

    /**
     * The empty tuple type.  Has only one instance.  Known also as "triv"
     * or "void".
     **/
    public static final VariantType UNIT                 = new VariantType("()");

    /**
     * An indefinite type that is a supertype of every type (including
     * itself).
     **/
    public static final VariantType ANY                  = new VariantType("*");

    /**
     * An indefinite type that is a supertype of every basic (ie:
     * non-container) type.
     **/
    public static final VariantType BASIC                = new VariantType("?");

    /**
     * An indefinite type that is a supertype of every maybe type.
     **/
    public static final VariantType MAYBE                = new VariantType("m*");

    /**
     * An indefinite type that is a supertype of every array type.
     **/
    public static final VariantType ARRAY                = new VariantType("a*");

    /**
     * An indefinite type that is a supertype of every tuple type,
     * regardless of the number of items in the tuple.
     **/
    public static final VariantType TUPLE                = new VariantType("r");

    /**
     * An indefinite type that is a supertype of every dictionary entry
     * type.
     **/
    public static final VariantType DICT_ENTRY           = new VariantType("{?*}");

    /**
     * An indefinite type that is a supertype of every dictionary type --
     * that is, any array type that has an element type equal to any
     * dictionary entry type.
     **/
    public static final VariantType DICTIONARY           = new VariantType("a{?*}");

    /**
     * The type of an array of strings.
     **/
    public static final VariantType STRING_ARRAY         = new VariantType("as");

    /**
     * The type of an array of object paths.
     **/
    public static final VariantType OBJECT_PATH_ARRAY    = new VariantType("ao");

    /**
     * The type of an array of bytes.  This type is commonly used to pass
     * around strings that may not be valid utf8.  In that case, the
     * convention is that the nul terminator character should be included as
     * the last character in the array.
     **/
    public static final VariantType BYTESTRING           = new VariantType("ay");

    /**
     * The type of an array of byte strings (an array of arrays of bytes).
     **/
    public static final VariantType BYTESTRING_ARRAY     = new VariantType("aay");

    /**
     * The type of a dictionary mapping strings to variants (the ubiquitous
     * {@code "a{sv}"} type).
     *
     * @since 2.30
     **/
    public static final VariantType VARDICT              = new VariantType("a{sv}");

    /**
     * Return a {@link VariantType} that is suitable for the Java class of this object
     * @param object a Java object instance
     * @return the VariantType
     */
    public static VariantType objectToVariantType(Object object) {
        return object instanceof Boolean ? BOOLEAN
                : object instanceof Byte ? BYTE
                : object instanceof Double ? DOUBLE
                : object instanceof Integer ? INT32
                : object instanceof Long ? INT64
                : object instanceof Short ? INT16
                : object instanceof String ? STRING
                : null;
    }

    /**
     * Return a Java class that is suitable for the {@link VariantType} of this GVariant
     * @param variant a {@link Variant} instance
     * @return the Java class
     */
    public static Class<?> variantToClass(Variant variant) {
        return variant.isOfType(BOOLEAN) ? Boolean.class
                : variant.isOfType(BYTE) ? Byte.class
                : variant.isOfType(DOUBLE) ? Double.class
                : variant.isOfType(INT32) ? Integer.class
                : variant.isOfType(INT64) ? Long.class
                : variant.isOfType(INT16) ? Short.class
                : variant.isOfType(STRING) ? String.class
                : null;
    }
}
