package girparser.model;

import girparser.generator.Conversions;

import java.io.IOException;
import java.io.Writer;

public class Parameter extends GirElement {

    public String transferOwnership, nullable, allowNone, direction;
    public boolean varargs = false;

    public Parameter(GirElement parent, String name, String transferOwnership, String nullable, String allowNone, String direction) {
        super(parent);
        this.name = Conversions.toLowerCaseJavaName(name);
        this.transferOwnership = transferOwnership;
        this.nullable = nullable;
        this.allowNone = allowNone;
        this.direction = direction;
    }

    public void generateTypeAndName(Writer writer) throws IOException {
        if (array != null) {
            writer.write(array.type.qualifiedJavaType + "[] " + name);
        } else if (type != null) {
            if (type.cType != null && type.cType.endsWith("**")) {
                writer.write(type.qualifiedJavaType + "[] " + name);
            } else if (type.isBitfield()) {
                writer.write("int " + name);
            } else {
                if (type.namespacePath != null) {
                    writer.write(type.qualifiedJavaType + " " + name);
                } else {
                    writer.write(type.simpleJavaType + " " + name);
                }
            }
        }
    }

    public void generateInterop(Writer writer) throws IOException {
        if (array != null) {
            generateArrayInterop(writer);
        } else if (type == null) {
            writer.write(name);
        } else if (type.cType.endsWith("**")) {
            //System.out.println("Generate array interop for parameter " + name + " with type: " + type.cType);
            generateArrayInterop(writer);
        } else if (type.qualifiedJavaType.equals("java.lang.String")) {
            writer.write("Interop.getAllocator().allocateUtf8String(" + name + ")");
        } else if (type.isBitfield()) {
            writer.write(name);
        } else if (type.isEnum()
                || type.simpleJavaType.equals("GType")
                || (type.isAlias() && (! ((Alias) type.girElementInstance).inherits()))) {
            writer.write(name + ".getValue()");
        } else if (type.isCallback()) {
            ((Callback) type.girElementInstance).generateInterop(writer, name);
        } else if (type.isClass()
                || type.isInterface()
                || type.isAlias()
                || type.isUnion()
                || type.qualifiedJavaType.startsWith("org.gtk.gobject.")) {
            writer.write(name + ".HANDLE()");
        } else if (type.name.equals("gboolean") && (! type.cType.equals("_Bool"))) {
            writer.write(name + " ? 1 : 0");
        } else {
            writer.write(name);
        }
    }

    private void generateArrayInterop(Writer writer) throws IOException {
        if (array == null && type != null && type.cType.endsWith("**")) {
            writer.write("Interop.allocateNativeArray(" + name + ")");
        } else if (array != null) {
            if (array.type == null) {
                writer.write("MemoryAddress.NULL");
            } else if (array.type.simpleJavaType.equals("boolean")) {
                writer.write("Interop.allocateNativeArray(" + name + ")");
            } else if (array.type.isPrimitive) {
                String typeconst = "JAVA_" + array.type.simpleJavaType.toUpperCase();
                writer.write("Interop.getAllocator().allocateArray(ValueLayout." + typeconst + ", " + name + ")");
            } else {
                writer.write("Interop.allocateNativeArray(" + name + ")");
            }
        }
    }

    public void generateCallbackInterop(Writer writer) throws IOException {
        if (array != null) {
            writer.write("null"); // TODO: Implement Arrays
        } else if (type == null) {
            writer.write(name);
        } else if (type.qualifiedJavaType.equals("java.lang.String")) {
            writer.write(name + ".getUtf8String(0)");
        } else if (type.isBitfield()) {
            writer.write(name);
        } else if (type.isEnum()
                || type.simpleJavaType.equals("GType")
                || (type.isAlias() && (! ((Alias) type.girElementInstance).inherits()))) {
            writer.write(type.qualifiedJavaType + ".fromValue(" + name + ")");
        } else if (type.isCallback()) {
            writer.write("null"); // I don't think this situation exists
        } else if (type.name.equals("gboolean") && (! type.cType.equals("_Bool"))) {
            writer.write(name); // Seems Panama already maps this as a boolean, even though I expected an int
        } else if (type.isPrimitive) {
            writer.write(name);
        } else if (type.isClass()
                || type.isInterface()
                || type.isAlias()
                || type.isUnion()
                || type.qualifiedJavaType.startsWith("org.gtk.gobject.")) {
            writer.write("new " + type.qualifiedJavaType + "(" + name + ")");
        } else {
            writer.write(name);
        }
    }
}
