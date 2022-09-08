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
        } else if (type.cType != null && type.cType.endsWith("**")) {
            generateArrayInterop(writer);
        } else if (type.qualifiedJavaType.equals("java.lang.String")) {
            writer.write("Interop.allocateNativeString(" + name + ").handle()");
        } else if (type.isBitfield()) {
            writer.write(name);
        } else if (type.isEnum()
                || type.simpleJavaType.equals("Type")
                || (type.isAlias() && (! ((Alias) type.girElementInstance).inherits()))) {
            writer.write(name + ".getValue()");
        } else if (type.isCallback()) {
            ((Callback) type.girElementInstance).generateInterop(writer, name);
        } else if (type.isRecord()) {
            writer.write(name + ".handle()");
        } else if (type.isClass()
                || type.isInterface()
                || type.isAlias()
                || type.isUnion()) {
            writer.write(name + (transferOwnership() ? ".getReference().unowned().handle()" : ".handle()"));
        } else if (type.name.equals("gboolean") && type.cType != null && (! type.cType.equals("_Bool"))) {
            writer.write(name + " ? 1 : 0");
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
        } else if (type.isBitfield()) {
            writer.write(name);
        } else if (type.isEnum()
                || type.simpleJavaType.equals("Type")
                || (type.isAlias() && (! ((Alias) type.girElementInstance).inherits()))) {
            writer.write(type.qualifiedJavaType + ".fromValue(" + name + ")");
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
