package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

import java.io.IOException;
import java.io.Writer;

public class Parameter extends GirElement {

    public final String transferOwnership, nullable, allowNone, direction;
    public boolean varargs = false;

    public Parameter(GirElement parent, String name, String transferOwnership, String nullable, String allowNone, String direction) {
        super(parent);
        this.name = Conversions.toLowerCaseJavaName(name);
        this.transferOwnership = transferOwnership;
        this.nullable = nullable;
        this.allowNone = allowNone;
        this.direction = direction;
    }

    public boolean transferOwnership() {
        return "full".equals(transferOwnership);
    }

    public boolean isInstanceParameter() {
        return (this instanceof InstanceParameter);
    }

    public boolean isCallbackParameter() {
        return (type != null) && type.isCallback();
    }

    public boolean isUserDataParameter() {
        return (type != null) && name.toLowerCase().endsWith("data")
                && ("gpointer".equals(type.cType) || "gconstpointer".equals(type.cType));
    }

    public boolean isDestroyNotify() {
        return isCallbackParameter() && "DestroyNotify".equals(type.simpleJavaType);
    }

    public boolean isErrorParameter() {
        return (type != null) && "GError**".equals(type.cType);
    }

    public void generateTypeAndName(Writer writer) throws IOException {
        if (array != null) {
            writer.write(array.type.qualifiedJavaType + "[]");
        } else if (type.cType != null && type.cType.endsWith("**")) {
            writer.write(type.qualifiedJavaType + "[]");
        } else if (type.isPrimitive && type.isPointer()) {
            writer.write("Pointer" + Conversions.primitiveClassName(type.simpleJavaType));
        } else {
            writer.write(type.qualifiedJavaType);
        }
        writer.write(" " + name);
    }

    public void generateInterop(Writer writer) throws IOException {
        if (array != null) {
            generateArrayInterop(writer);
        } else if (type == null) {
            writer.write(name);
        } else if (type.cType != null && type.cType.endsWith("**")) {
            generateArrayInterop(writer);
        } else if (type.qualifiedJavaType.equals("java.lang.String")) {
            writer.write("Interop.allocateNativeString(" + name + ").handle()");
        } else if (type.isPrimitive && type.isPointer()) {
            writer.write(name + ".handle()");
        } else if (type.name.equals("gboolean") && type.cType != null && (! type.cType.equals("_Bool"))) {
            writer.write(name + " ? 1 : 0");
        } else if (type.girElementInstance instanceof RegisteredType rt) {
            writer.write(rt.getInteropString(name, type.isPointer(), transferOwnership()));
        } else {
            writer.write(name);
        }
    }

    private void generateArrayInterop(Writer writer) throws IOException {
        if (array == null && type != null && type.cType.endsWith("**")) {
            writer.write("Interop.allocateNativeArray(" + name + ").handle()");
        } else if (array != null) {
            if (array.type == null) {
                writer.write("MemoryAddress.NULL");
            } else if (array.type.simpleJavaType.equals("boolean")) {
                writer.write("Interop.allocateNativeArray(" + name + ").handle()");
            } else if (array.type.isPrimitive) {
                String typeconst = "JAVA_" + array.type.simpleJavaType.toUpperCase();
                writer.write("new MemorySegmentReference(Interop.getAllocator().allocateArray(ValueLayout." + typeconst + ", " + name + ")).handle()");
            } else if (array.type.isAliasForPrimitive()) {
                writer.write("Interop.allocateNativeArray(" + array.type.qualifiedJavaType + ".getValues(" + name + ")).handle()");
            } else {
                writer.write("Interop.allocateNativeArray(" + name + ").handle()");
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
        } else if (type.isPrimitive && type.isPointer()) {
            writer.write("new Pointer" + Conversions.primitiveClassName(type.simpleJavaType) + "(" + name + ")");
        } else if (type.isBitfield() || type.isEnum() || type.isAliasForPrimitive()) {
            writer.write("new " + type.qualifiedJavaType + "(" + name + ")");
        } else if (type.isCallback()) {
            writer.write("null"); // I don't think this situation exists
        } else if (type.name.equals("gboolean") && type.cType != null && (! type.cType.equals("_Bool"))) {
            writer.write(name); // Seems jextract already maps this to a boolean, even though I expected an int
        } else if (type.isPrimitive) {
            writer.write(name);
        } else if (type.isInterface()) {
            writer.write("new " + type.qualifiedJavaType + "." + type.simpleJavaType + "Impl(References.get(" + name + ", " + (transferOwnership() ? "true" : "false") + "))");
        } else if (type.isClass()
                || type.isAlias()
                || type.isUnion()) {
            writer.write("new " + type.qualifiedJavaType + "(References.get(" + name + ", " + (transferOwnership() ? "true" : "false") + "))");
        } else {
            writer.write(name);
        }
    }
}
