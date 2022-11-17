package io.github.jwharm.javagi.generator;

import java.io.IOException;
import java.io.Writer;

/**
 * Helper class to add fundamental GType definitions in the org.gtk.glib.Type class.
 * The type definitions are not in the gir file.
 */
public class GTypeDefinitions {

    public static void generateFundamentalGTypes(Writer writer) throws IOException {
        writer.write("    \n");
        writer.write(
                  "    public static final long G_TYPE_FUNDAMENTAL_SHIFT = 2;\n"
                + "    public static final Type G_TYPE_INVALID = new Type(0L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_NONE = new Type(1L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_INTERFACE = new Type(2L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_CHAR = new Type(3L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_UCHAR = new Type(4L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_BOOLEAN = new Type(5L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_INT = new Type(6L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_UINT = new Type(7L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_LONG = new Type(8L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_ULONG = new Type(9L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_INT64 = new Type(10L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_UINT64 = new Type(11L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_ENUM = new Type(12L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_FLAGS = new Type(13L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_FLOAT = new Type(14L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_DOUBLE = new Type(15L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_STRING = new Type(16L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_POINTER = new Type(17L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_BOXED = new Type(18L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_PARAM = new Type(19L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_OBJECT = new Type(20L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
                + "    public static final Type G_TYPE_VARIANT = new Type(21L << G_TYPE_FUNDAMENTAL_SHIFT);\n"
        );
    }

}
