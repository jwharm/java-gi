package io.github.jwharm.javagi.generator;

import java.io.IOException;
import java.io.Writer;

/**
 * Helper class to generate additional methods in the org.gtk.glib.Value class
 */
public class GValueCreator {

    public static void generateGValueConstructors(Writer writer) throws IOException {
        generateGValueConstructor(writer, "boolean", "G_TYPE_BOOLEAN", "setBoolean(arg)");
        generateGValueConstructor(writer, "byte", "G_TYPE_CHAR", "setSchar(arg)");
        generateGValueConstructor(writer, "double", "G_TYPE_DOUBLE", "setDouble(arg)");
        generateGValueConstructor(writer, "float", "G_TYPE_FLOAT", "setFloat(arg)");
        generateGValueConstructor(writer, "int", "G_TYPE_INT", "setInt(arg)");
        generateGValueConstructor(writer, "long", "G_TYPE_LONG", "setLong(arg)");
        generateGValueConstructor(writer, "String", "G_TYPE_STRING", "setString(arg)");
        generateGValueConstructor(writer, "Enumeration", "G_TYPE_ENUM", "setEnum(arg.getValue())");
        generateGValueConstructor(writer, "Bitfield", "G_TYPE_FLAGS", "setFlags(arg.getValue())");
        generateGValueConstructor(writer, "org.gtk.gobject.Object", "G_TYPE_OBJECT", "setObject(arg)");
        generateGValueConstructor(writer, "MemoryAddress", "G_TYPE_POINTER", "setPointer(arg)");
        generateGValueConstructor(writer, "ParamSpec", "G_TYPE_PARAM", "setParam(arg)");
    }
    
    private static void generateGValueConstructor(Writer writer, String javatype, String gtype, String method) throws IOException {
        writer.write("    \n");
        writer.write("    /**\n");
        writer.write("     * Create a {@code " + gtype + "} {@link Value} of with the provided {@code " + javatype + "} value.\n");
        writer.write("     * @param  arg The initial value to set\n");
        writer.write("     * @return The new {@link Value}\n");
        writer.write("     */\n");
        writer.write("    public static Value create(" + javatype + " arg) {\n");
        writer.write("        Value v = allocate();\n");
        writer.write("        v.init(org.gtk.glib.Type." + gtype + ");\n");
        writer.write("        v." + method + ";\n");
        writer.write("        return v;\n");
        writer.write("    }\n");
    }

}
