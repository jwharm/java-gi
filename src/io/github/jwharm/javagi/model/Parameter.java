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

    public void generateTypeAndName(Writer writer, boolean pointerForArray) throws IOException {
        // Arrays
        if (array != null) {
            generateArrayTypeAndName(writer, array.type, pointerForArray);
        
        // Also arrays
        } else if (type.cType != null && type.cType.endsWith("**")) {
            generateArrayTypeAndName(writer, type, pointerForArray);
        
        // Pointer to primitive type
        } else if (type.isPrimitive && type.isPointer()) {
            writer.write("Pointer" + Conversions.primitiveClassName(type.simpleJavaType));
        
        // Everything else
        } else {
            writer.write(type.qualifiedJavaType);
        }
        writer.write(" " + name);
    }
    
    private void generateArrayTypeAndName(Writer writer, Type type, boolean pointerForArray) throws IOException {
        if (pointerForArray) {
            String typename = type.qualifiedJavaType;
            if (type.isAliasForPrimitive()) {
                typename = Conversions.primitiveClassName(type.girElementInstance.type.qualifiedJavaType);
            } else if (type.isPrimitive) {
                typename = Conversions.primitiveClassName(type.qualifiedJavaType);
            }
            writer.write("PointerIterator<" + typename + ">");
        } else {
            writer.write(type.qualifiedJavaType + "[]");
        }
    }

    public void generateInterop(Writer writer) throws IOException {
        // Arrays
        if (array != null) {
            generateArrayInterop(writer, array.type);
        
        // This should not happen
        } else if (type == null) {
            writer.write(name);
        
        // Also arrays
        } else if (type.cType != null && type.cType.endsWith("**")) {
            generateArrayInterop(writer, type);
        
        // Strings: allocate utf8 string
        } else if (type.qualifiedJavaType.equals("java.lang.String")) {
            writer.write("Interop.allocateNativeString(" + name + ").handle()");
        
        // Pointer to primitive type: get memory address
        } else if (type.isPrimitive && type.isPointer()) {
            writer.write(name + ".handle()");
        
        // Convert boolean to int
        } else if (type.isBoolean()) {
            writer.write(name + " ? 1 : 0");
        
        // Objects and ValueWrappers
        } else if (type.girElementInstance instanceof RegisteredType rt) {
            writer.write(rt.getInteropString(name, type.isPointer(), transferOwnership()));
        
        // Primitive types
        } else {
            writer.write(name);
        }
    }

    private void generateArrayInterop(Writer writer, Type type) throws IOException {
        // This should not happen
        if (type == null) {
            writer.write("MemoryAddress.NULL");

        // Convert array of ValueWrapper types to an array of the wrapped values
        } else if (type.isEnum() || type.isBitfield() || type.isAliasForPrimitive()) {
            String typename = "";
            if (type.isAliasForPrimitive()) {
                typename = Conversions.primitiveClassName(type.girElementInstance.type.qualifiedJavaType);
            }
            writer.write("Interop.allocateNativeArray(" + type.qualifiedJavaType + ".get" + typename + "Values(" + name + ")).handle()");

        // Automatically use the right allocateNativeArray() method for this type
        } else {
            writer.write("Interop.allocateNativeArray(" + name + ").handle()");
        }
    }

    public void generateReverseInterop(Writer writer, String identifier) throws IOException {
        // Arrays
        if (array != null) {
            generateReverseArrayInterop(writer, identifier);
        
        // Also arrays, but in this case it's always a pointer to an object
        } else if (type.cType != null && type.cType.endsWith("**")) {
            writer.write("new PointerProxy<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.qualifiedJavaType + ".class)");
        
        // This should not happen
        } else if (type == null) {
            writer.write(identifier);
        
        // Create Java String from UTF8 memorysegment
        } else if (type.qualifiedJavaType.equals("java.lang.String")) {
            writer.write(identifier + ".getUtf8String(0)");
        
        // Create Pointer object
        } else if (type.isPrimitive && type.isPointer()) {
            writer.write("new Pointer" + Conversions.primitiveClassName(type.simpleJavaType) + "(" + identifier + ")");
        
        // Create ValueWrapper object
        } else if (type.isBitfield() || type.isEnum() || type.isAliasForPrimitive()) {
            writer.write("new " + type.qualifiedJavaType + "(" + identifier + ")");
        
        // I don't think this situation exists
        } else if (type.isCallback()) {
            writer.write("null /* Unsupported parameter type */");
        
        // Convert int back to boolean
        } else if (type.isBoolean()) {
            writer.write(identifier + " != 0");
        
        // Primitive values remain as-is
        } else if (type.isPrimitive) {
            writer.write(identifier);
        
        // Create an Impl object when we only know the interface but not the class
        } else if (type.isInterface()) {
            writer.write("new " + type.qualifiedJavaType + "." + type.simpleJavaType + "Impl(References.get(" + identifier + ", " + (transferOwnership() ? "true" : "false") + "))");
        
        // Objects
        } else if (type.isClass() || type.isAlias() || type.isUnion()) {
            writer.write("new " + type.qualifiedJavaType + "(References.get(" + identifier + ", " + (transferOwnership() ? "true" : "false") + "))");
        
        // Anything else
        } else {
            writer.write(identifier);
        }
    }
    
    public void generateReverseArrayInterop(Writer writer, String identifier) throws IOException {
        // Array of arrays - this is not supported yet
        if (array.array != null) {
            writer.write("");
            
        // This should not happen
        } else if (array.type == null) {
            writer.write("null");
        
        // Pointer to enumeration
        } else if (array.type.isEnum()) {
            writer.write("new PointerEnumeration<" + array.type.qualifiedJavaType + ">(" + identifier + ", " + array.type.qualifiedJavaType + ".class).iterator()");
        
        // Pointer to bitfield
        } else if (array.type.isBitfield()) {
            writer.write("new PointerBitfield<" + array.type.qualifiedJavaType + ">(" + identifier + ", " + array.type.qualifiedJavaType + ".class).iterator()");
        
        // Pointer to wrapped primitive value
        } else if (array.type.isAliasForPrimitive()) {
            writer.write("new Pointer" + Conversions.primitiveClassName(array.type.girElementInstance.type.qualifiedJavaType) + "(" + identifier + ").iterator()");
            
        // Pointer to primitive value
        } else if (array.type.isPrimitive) {
            writer.write("new Pointer" + Conversions.primitiveClassName(array.type.qualifiedJavaType) + "(" + identifier + ").iterator()");
        
        // Pointer to UTF8 memorysegment
        } else if (array.type.qualifiedJavaType.equals("java.lang.String")) {
            writer.write("new PointerString(" + identifier + ").iterator()");
        
        // Pointer to memorysegment
        } else if (array.type.qualifiedJavaType.equals("java.lang.foreign.MemoryAddress")) {
            writer.write("new PointerAddress(" + identifier + ").iterator()");
            
        // Pointer to object
        } else {
            writer.write("new PointerProxy<" + array.type.qualifiedJavaType + ">(" + identifier + ", " + array.type.qualifiedJavaType + ".class).iterator()");
        }
    }
    
    public String getReturnType() {
        // Arrays
        if (array != null) {
            return getArrayReturnType();
        
        // Also arrays, but in this case it's always a pointer to an object
        } else if (type.cType != null && type.cType.endsWith("**")) {
            return "PointerProxy<" + type.qualifiedJavaType + ">";
        
        // This should not happen
        } else if (type == null) {
            return "";
        
        // Create Pointer object
        } else if (type.isPrimitive && type.isPointer()) {
            return "Pointer" + Conversions.primitiveClassName(type.simpleJavaType);
        
        // Anything else
        } else {
            return type.qualifiedJavaType;
        }
    }
    
    public String getArrayReturnType() {
        // This should not happen
        if (array.type == null) {
            return "void";
        
        // Pointer to enumeration or bitfield
        } else if (array.type.isEnum() || array.type.isBitfield()) {
            return "PointerIterator<" + array.type.qualifiedJavaType + ">";
        
        // Pointer to wrapped primitive value
        } else if (array.type.isAliasForPrimitive()) {
            return "PointerIterator<" + Conversions.primitiveClassName(array.type.girElementInstance.type.qualifiedJavaType) + ">";
            
        // Pointer to primitive value
        } else if (array.type.isPrimitive) {
            return "PointerIterator<" + Conversions.primitiveClassName(array.type.qualifiedJavaType) + ">";
        
        // Pointer to UTF8 memorysegment
        } else if (array.type.qualifiedJavaType.equals("java.lang.String")) {
            return "PointerIterator<java.lang.String>";
        
        // Pointer to UTF8 memorysegment
        } else if (array.type.qualifiedJavaType.equals("java.lang.foreign.MemoryAddress")) {
            return "PointerIterator<java.lang.foreign.MemoryAddress>";
            
        // Pointer to object
        } else {
            return "PointerIterator<" + array.type.qualifiedJavaType + ">";
        }
    }
}
