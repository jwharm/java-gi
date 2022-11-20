package io.github.jwharm.javagi.generator;

import java.io.IOException;
import java.io.Writer;

/**
 * Helper class to generate additional {@code create()} methods in the org.gtk.glib.Value class
 */
public class GValueCreator {

    public static void generateGValueConstructors(Writer writer) throws IOException {
        generateGValueConstructor(writer, "boolean", "org.gtk.glib.Type.G_TYPE_BOOLEAN", "setBoolean(arg)");
        generateGValueConstructor(writer, "byte", "org.gtk.glib.Type.G_TYPE_CHAR", "setSchar(arg)");
        generateGValueConstructor(writer, "double", "org.gtk.glib.Type.G_TYPE_DOUBLE", "setDouble(arg)");
        generateGValueConstructor(writer, "float", "org.gtk.glib.Type.G_TYPE_FLOAT", "setFloat(arg)");
        generateGValueConstructor(writer, "int", "org.gtk.glib.Type.G_TYPE_INT", "setInt(arg)");
        generateGValueConstructor(writer, "long", "org.gtk.glib.Type.G_TYPE_LONG", "setLong(arg)");
        generateGValueConstructor(writer, "String", "org.gtk.glib.Type.G_TYPE_STRING", "setString(arg)");
        generateGValueConstructor(writer, "Enumeration", "org.gtk.glib.Type.G_TYPE_ENUM", "setEnum(arg.getValue())");
        generateGValueConstructor(writer, "Bitfield", "org.gtk.glib.Type.G_TYPE_FLAGS", "setFlags(arg.getValue())");
        generateGValueConstructor(writer, "org.gtk.gobject.Object", "org.gtk.glib.Type.G_TYPE_OBJECT", "setObject(arg)");
        generateGValueConstructor(writer, "org.gtk.glib.Type", "org.gtk.gobject.GObject.gtypeGetType()", "setGtype(arg)");
        generateGValueConstructor(writer, "Struct", "org.gtk.glib.Type.G_TYPE_BOXED", "setBoxed((MemoryAddress) arg.handle())");
        generateGValueConstructor(writer, "MemoryAddress", "org.gtk.glib.Type.G_TYPE_POINTER", "setPointer(arg)");
        generateGValueConstructor(writer, "ParamSpec", "org.gtk.glib.Type.G_TYPE_PARAM", "setParam(arg)");
        generateGValueConstructor(writer, "Proxy", "org.gtk.glib.Type.G_TYPE_OBJECT", "setObject((org.gtk.gobject.Object) arg)");
    }
    
    private static void generateGValueConstructor(Writer writer, String javatype, String gtype, String method) throws IOException {
        writer.write("    \n");
        writer.write("    /**\n");
        writer.write("     * Create a {@link Value} of with the provided {@code " + javatype + "} value.\n");
        writer.write("     * @param  arg The initial value to set\n");
        writer.write("     * @return The new {@link Value}\n");
        writer.write("     */\n");
        writer.write("    public static Value create(" + javatype + " arg) {\n");
        writer.write("        Value v = allocate();\n");
        writer.write("        v.init(" + gtype + ");\n");
        writer.write("        v." + method + ";\n");
        writer.write("        return v;\n");
        writer.write("    }\n");
    }

}
