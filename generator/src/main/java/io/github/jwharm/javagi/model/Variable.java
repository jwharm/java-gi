package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

import io.github.jwharm.javagi.generator.Conversions;

public class Variable extends GirElement {

    public Variable(GirElement parent) {
        super(parent);
    }

    /**
     * We cannot null-check primitive values.
     * @return true if this parameter is not a primitive value
     */
    public boolean checkNull() {
        return ! (type != null && type.isPrimitive && (! type.isPointer()));
    }

    public void writeType(Writer writer, boolean pointerForArray) throws IOException {
        writer.write(getType(pointerForArray));
    }

    public void writeName(Writer writer) throws IOException {
        writer.write(getName());
    }

    public void writeTypeAndName(Writer writer, boolean pointerForArray) throws IOException {
        writeType(writer, pointerForArray);
        writer.write(" ");
        writeName(writer);
    }

    public void marshalJavaToNative(Writer writer, String identifier, boolean pointerForArray) throws IOException {
        writer.write(marshalJavaToNative(identifier, pointerForArray));
    }

    public void marshalNativeToJava(Writer writer, String identifier, boolean upcall) throws IOException {
        writer.write(marshalNativeToJava(identifier, upcall));
    }

    private String getAnnotations(Type type) {
        if ((! type.isPrimitive) && (this instanceof Parameter p))
            return p.nullable ? "@Nullable " : "@NotNull ";

        return "";
    }

    private String getType(boolean pointerForArray) {
        if (type != null)
            return getAnnotations(type) + getType(type);

        if (array != null && array.array != null && "gchar***".equals(array.cType))
            return "java.lang.String[][]";

        if (array != null && array.type != null)
            return getAnnotations(array.type)
                    + ((array.size() != null)
                    ? getArrayType(array.type)
                    : (pointerForArray ? getPointerType(array.type) : getArrayType(array.type)));

        if (this instanceof Parameter p && p.varargs)
            return "java.lang.Object...";

        return "java.lang.Object /* unsupported */";
    }

    private String getType(Type type) {
        if ("void".equals(type.simpleJavaType))
            return "void";

        if (this instanceof Parameter p && p.varargs)
            return "java.lang.Object...";

        if (this instanceof Parameter p && p.isOutParameter())
            return "Out<" + (type.isPrimitive ? Conversions.primitiveClassName(type.simpleJavaType) : type.qualifiedJavaType) + ">";

        if (type.cType != null && type.cType.endsWith("**"))
            return getPointerType(type);

        if (type.isPrimitive && type.isPointer())
            return "Pointer" + Conversions.primitiveClassName(type.simpleJavaType);

        return type.qualifiedJavaType;
    }

    private String getArrayType(Type type) {
        if (this instanceof Parameter p && p.isOutParameter())
            return "Out<" + type.qualifiedJavaType + "[]>";

        return type.qualifiedJavaType + "[]";
    }

    private String getPointerType(Type type) {
        if (type.isEnum())
            return "PointerEnumeration";

        if (type.isBitfield())
            return "PointerBitfield";

        if (type.isAliasForPrimitive())
            return "Pointer" + Conversions.primitiveClassName(type.girElementInstance.type.qualifiedJavaType);

        if (type.isPrimitive)
            return "Pointer" + Conversions.primitiveClassName(type.qualifiedJavaType);

        if (type.qualifiedJavaType.equals("java.lang.String"))
            return "PointerString";

        if (type.qualifiedJavaType.equals("java.lang.foreign.MemoryAddress"))
            return "PointerAddress";

        return "PointerProxy<" + type.qualifiedJavaType + ">";
    }

    private String getName() {
        return "...".equals(name) ? "varargs" : name;
    }

    private String marshalJavaToNative(String identifier, boolean pointerForArray) {
        if (type != null)
            return marshalJavaToNative(type, identifier);

        if (array != null && array.array != null)
            return "(Addressable) MemoryAddress.NULL /* unsupported */";

        if (array != null && array.type != null)
            return (array.size() != null)
                    ? marshalJavaArrayToNative(array, identifier)
                    : (pointerForArray ? marshalJavaPointerToNative(identifier) : marshalJavaArrayToNative(array, identifier));

        return "(Addressable) MemoryAddress.NULL /* unsupported */";
    }

    private String marshalJavaToNative(Type type, String identifier) {
        if (this instanceof Parameter p && p.isOutParameter())
            return "(Addressable) " + identifier + "POINTER.address()";

        if (type.cType != null && type.cType.endsWith("**"))
            return marshalJavaPointerToNative(identifier);

        if (type.qualifiedJavaType.equals("java.lang.String"))
            return "Marshal.stringToAddress.marshal(" + identifier + ", null)";

        if (type.qualifiedJavaType.equals("java.lang.foreign.MemoryAddress"))
            return "(Addressable) " + identifier;

        if (type.isPrimitive && type.isPointer())
            return identifier + ".handle()";

        if (type.isBoolean())
            return "Marshal.booleanToInteger.marshal(" + identifier + ", null).intValue()";

        if (type.girElementInstance != null)
            return type.girElementInstance.getInteropString(
                    identifier,
                    type.isPointer(),
                    (this instanceof Parameter p) ? p.transferOwnership() : "Ownership.UNKNOWN");

        return identifier;
    }

    private String marshalJavaArrayToNative(Array array, String identifier) {
        Type type = array.type;
        String zeroTerminated = "1".equals(array.zeroTerminated) ? "true" : "false";

        if (this instanceof Parameter p && p.isOutParameter())
            return "(Addressable) " + identifier + "POINTER.address()";

        if (type.isEnum() || type.isBitfield() || type.isAliasForPrimitive())
            return "Interop.allocateNativeArray("
                    + (type.isEnum() ? "Enumeration" : type.isBitfield() ? "Bitfield" : type.qualifiedJavaType) + ".get"
                    + (type.isAliasForPrimitive() ? Conversions.primitiveClassName(type.girElementInstance.type.qualifiedJavaType) : "")
                    + "Values(" + identifier + "), " + zeroTerminated + ")";

        if (type.isRecord())
            return "Interop.allocateNativeArray(" + identifier + ", " + type.qualifiedJavaType + ".getMemoryLayout(), " + zeroTerminated + ")";

        return "Interop.allocateNativeArray(" + identifier + ", " + zeroTerminated + ")";
    }

    private String marshalJavaPointerToNative(String identifier) {
        return identifier + ".handle()";
    }

    private String marshalNativeToJava(String identifier, boolean upcall) {
        if (type != null) {
            if (type.cType != null && type.cType.endsWith("**"))
                return marshalNativetoJavaPointer(type, identifier);

            if (type.isPrimitive && type.isPointer())
                return marshalNativetoJavaPointer(type, identifier);

            return marshalNativeToJava(type, identifier, upcall);
        }

        if (array != null && array.array != null)
            return "null /* unsupported */";

        if (array != null && array.type != null)
            return (array.size() == null)
                    ? marshalNativetoJavaPointer(array.type, identifier) : marshalNativeToJavaArray(array, identifier);

        return "null /* unsupported */";
    }

    protected String marshalNativeToJava(Type type, String identifier, boolean upcall) {
        if ("java.lang.String".equals(type.qualifiedJavaType))
            return "Marshal.addressToString.marshal(" + identifier + ", null)";

        if (type.isEnum())
            return type.qualifiedJavaType + ".of(" + identifier + ")";

        if (upcall && type.isAliasForPrimitive() && type.isPointer())
            return identifier + "ALIAS";

        if (type.isBitfield() || type.isAliasForPrimitive())
            return "new " + type.qualifiedJavaType + "(" + identifier + ")";

        if (type.isCallback())
            return "null /* Unsupported parameter type */";

        if (type.isClass() || type.isAlias() || type.isUnion() || type.isInterface())
            return type.qualifiedJavaType + ".fromAddress.marshal(" + identifier + ", "
                    + (this instanceof Parameter p ? p.transferOwnership() : "Ownership.UNKNOWN") + ")";

        if (type.isBoolean())
            return "Marshal.integerToBoolean.marshal(" + identifier + ", null).booleanValue()";

        return identifier;
    }

    private String marshalNativeToJavaArray(Array array, String identifier) {
        Type type = array.type;

        if ("java.lang.String".equals(type.qualifiedJavaType))
            return "Interop.getStringArrayFrom(" + identifier + ", " + array.size() + ")";

        if ("java.lang.foreign.MemoryAddress".equals(type.qualifiedJavaType))
            return "Interop.getAddressArrayFrom(" + identifier + ", " + array.size() + ")";

        if (type.isEnum())
            return "null /* unsupported */";

        if (type.isBitfield())
            return "null /* unsupported */";

        if (type.isAliasForPrimitive())
            return "null /* unsupported */";

        if (type.isPrimitive)
            return "Interop.get" + Conversions.primitiveClassName(type.simpleJavaType) + "ArrayFrom(" + identifier + ", " + array.size() + ")";

        return type.qualifiedJavaType + ".fromNativeArray(" + identifier + ", " + array.size() + ", "
                + (this instanceof Parameter p ? p.transferOwnership() : "Ownership.UNKNOWN") + ")";
    }

    private String marshalNativetoJavaPointer(Type type, String identifier) {
        if ("java.lang.String".equals(type.qualifiedJavaType))
            return "new PointerString(" + identifier + ")";

        if ("java.lang.foreign.MemoryAddress".equals(type.qualifiedJavaType))
            return "new PointerAddress(" + identifier + ")";

        if (type.isEnum())
            return "new PointerEnumeration<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.qualifiedJavaType + "::new)";

        if (type.isBitfield())
            return "new PointerBitfield<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.qualifiedJavaType + "::new)";

        if (type.isAliasForPrimitive())
            return "new Pointer" + Conversions.primitiveClassName(type.girElementInstance.type.qualifiedJavaType) + "(" + identifier + ")";

        if (type.isBoolean())
            return "new Pointer" + Conversions.primitiveClassName(type.simpleJavaType) + "(" + identifier + " == 1)";

        if (type.isPrimitive)
            return "new Pointer" + Conversions.primitiveClassName(type.simpleJavaType) + "(" + identifier + ")";

        return "new PointerProxy<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.qualifiedJavaType + ".fromAddress)";
    }
}
