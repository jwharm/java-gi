package io.github.jwharm.javagi.model;

import java.io.IOException;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

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

    public void writeType(SourceWriter writer, boolean pointerForArray) throws IOException {
        writer.write(getType(pointerForArray));
    }

    public void writeName(SourceWriter writer) throws IOException {
        writer.write(getName());
    }

    public void writeTypeAndName(SourceWriter writer, boolean pointerForArray) throws IOException {
        writeType(writer, pointerForArray);
        writer.write(" ");
        writeName(writer);
    }

    public void marshalJavaToNative(SourceWriter writer, String identifier, boolean pointerForArray, boolean upcall) throws IOException {
        writer.write(marshalJavaToNative(identifier, pointerForArray, upcall));
    }

    public void marshalNativeToJava(SourceWriter writer, String identifier, boolean upcall) throws IOException {
        writer.write(marshalNativeToJava(identifier, upcall));
    }

    private String getAnnotations(Type type) {
        if ((! type.isPrimitive) && (!type.isVoid()) && (this instanceof Parameter p))
            return p.nullable ? "@Nullable " : p.notnull ? "@NotNull " : "";

        return "";
    }

    private String getType(boolean pointerForArray) {
        if (type != null)
            return getAnnotations(type) + getType(type);

        if (array != null && array.array != null && "gchar***".equals(array.cType))
            return "java.lang.String[][]";

        if (array != null && array.type != null)
            return getAnnotations(array.type)
                    + ((array.size(false) != null)
                    ? getArrayType(array.type)
                    : (pointerForArray ? getPointerType(array.type) : getArrayType(array.type)));

        if (this instanceof Parameter p && p.varargs)
            return "java.lang.Object...";

        if (this instanceof Field f && f.callback != null)
            return f.callbackType;

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

        if (type.isBitfield() && type.isPointer())
            return "PointerBitfield<" + type.qualifiedJavaType + ">";

        if (type.isEnum() && type.isPointer())
            return "PointerEnumeration<" + type.qualifiedJavaType + ">";

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

    private String marshalJavaToNative(String identifier, boolean pointerForArray, boolean upcall) {
        if (type != null)
            return marshalJavaToNative(type, identifier, upcall);

        if (array != null && array.array != null)
            return "(Addressable) MemoryAddress.NULL /* unsupported */";

        if (array != null && array.type != null)
            return (array.size(upcall) != null)
                    ? marshalJavaArrayToNative(array, identifier)
                    : (pointerForArray ? marshalJavaPointerToNative(array.type, identifier, upcall)
                    : marshalJavaArrayToNative(array, identifier));

        if (this instanceof Field f && f.callback != null)
            return identifier + ".toCallback()";

        return "(Addressable) MemoryAddress.NULL /* unsupported */";
    }

    protected String marshalJavaToNative(Type type, String identifier, boolean upcall) {
        if (type.cType != null && type.cType.endsWith("**"))
            return marshalJavaPointerToNative(type, identifier, upcall);

        if (type.qualifiedJavaType.equals("java.lang.String"))
            return "Marshal.stringToAddress.marshal(" + identifier + ", SCOPE)";

        if (type.qualifiedJavaType.equals("java.lang.foreign.MemoryAddress"))
            return "(Addressable) " + identifier;

        if (type.isPointer() && (type.isPrimitive || type.isBitfield() || type.isEnum()))
            return identifier + ".handle()";

        if (type.isBoolean())
            return "Marshal.booleanToInteger.marshal(" + identifier + ", null).intValue()";

        if (type.girElementInstance != null)
            return type.girElementInstance.getInteropString(identifier, type.isPointer());

        return identifier;
    }

    private String marshalJavaArrayToNative(Array array, String identifier) {
        Type type = array.type;

        // If zero-terminated is missing, there's no length, there's no fixed size,
        // and the name attribute is unset, then zero-terminated is true.
        String zeroTerminated =
                ((! "0".equals(array.zeroTerminated)) && array.size(false) == null && array.name == null)
                ? "true" : "false";

        if (type.isEnum() || type.isBitfield() || type.isAliasForPrimitive())
            return "Interop.allocateNativeArray("
                    + (type.isEnum() ? "Enumeration" : type.isBitfield() ? "Bitfield" : type.qualifiedJavaType) + ".get"
                    + (type.isAliasForPrimitive() ? Conversions.primitiveClassName(type.girElementInstance.type.qualifiedJavaType) : "")
                    + "Values(" + identifier + "), " + zeroTerminated + ", SCOPE)";

        if (type.isRecord())
            return "Interop.allocateNativeArray(" + identifier + ", " + type.qualifiedJavaType + ".getMemoryLayout(), " + zeroTerminated + ", SCOPE)";

        return "Interop.allocateNativeArray(" + identifier + ", " + zeroTerminated + ", SCOPE)";
    }

    private String marshalJavaPointerToNative(Type type, String identifier, boolean upcall) {
        if (upcall && "java.lang.String".equals(type.qualifiedJavaType))
            return "Marshal.stringToAddress.marshal(" + identifier + ", SCOPE)";

        return identifier + ".handle()";
    }

    private String marshalNativeToJava(String identifier, boolean upcall) {
        if (type != null) {
            if (type.cType != null && type.cType.endsWith("**"))
                return marshalNativetoJavaPointer(type, identifier);

            if (type.isPointer() && (type.isPrimitive || type.isBitfield() || type.isEnum()))
                return marshalNativetoJavaPointer(type, identifier);

            return marshalNativeToJava(type, identifier, upcall);
        }

        if (array != null && array.array != null)
            return "null /* unsupported */";

        if (array != null && array.type != null)
            return (array.size(upcall) == null)
                    ? marshalNativetoJavaPointer(array.type, identifier) : marshalNativeToJavaArray(array, identifier, upcall);

        return "null /* unsupported */";
    }

    protected String marshalNativeToJava(Type type, String identifier, boolean upcall) {
        if ("java.lang.String".equals(type.qualifiedJavaType))
            return "Marshal.addressToString.marshal(" + identifier + ", null)";

        if (type.isEnum())
            return type.qualifiedJavaType + ".of(" + identifier + ")";

        if (type.isBitfield() || type.isAliasForPrimitive())
            return "new " + type.qualifiedJavaType + "(" + identifier + ")";

        if (type.isCallback())
            return "null /* Unsupported parameter type */";

        if (type.isRecord() || type.isUnion())
            return type.qualifiedJavaType + ".fromAddress.marshal(" + identifier + ", null)";

        if (type.isClass() || type.isInterface() || type.isAlias())
            return "(" + type.qualifiedJavaType + ") Interop.register(" + identifier + ", " + type.qualifiedJavaType + ".fromAddress)"
                    + ".marshal(" + identifier + ", null)";

        if (type.isBoolean())
            return "Marshal.integerToBoolean.marshal(" + identifier + ", null).booleanValue()";

        return identifier;
    }

    private String marshalNativeToJavaArray(Array array, String identifier, boolean upcall) {
        Type type = array.type;

        if ("java.lang.String".equals(type.qualifiedJavaType))
            return "Interop.getStringArrayFrom(" + identifier + ", " + array.size(upcall) + ")";

        if ("java.lang.foreign.MemoryAddress".equals(type.qualifiedJavaType))
            return "Interop.getAddressArrayFrom(" + identifier + ", " + array.size(upcall) + ")";

        if (type.isEnum())
            return "new PointerEnumeration<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.qualifiedJavaType + "::of)"
                    + ".toArray((int) " + array.size(upcall) + ", " + type.qualifiedJavaType + ".class)";

        if (type.isBitfield())
            return "new PointerBitfield<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.qualifiedJavaType + "::new)"
                    + ".toArray((int) " + array.size(upcall) + ", " + type.qualifiedJavaType + ".class)";

        if (type.isAliasForPrimitive())
            return type.qualifiedJavaType + ".fromNativeArray(" + identifier + ", " + array.size(upcall) + ")";

        if (type.isPrimitive)
            return "MemorySegment.ofAddress(" + identifier + ", " + array.size(upcall)
                    + ", SCOPE).toArray(" + Conversions.getValueLayout(array.type) + ")";

        return "new PointerProxy<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.qualifiedJavaType + ".fromAddress)"
                + ".toArray((int) " + array.size(upcall) + ", " + type.qualifiedJavaType + ".class)";
    }

    private String marshalNativetoJavaPointer(Type type, String identifier) {
        if ("java.lang.String".equals(type.qualifiedJavaType))
            return "new PointerString(" + identifier + ")";

        if ("java.lang.foreign.MemoryAddress".equals(type.qualifiedJavaType))
            return "new PointerAddress(" + identifier + ")";

        if (type.isEnum())
            return "new PointerEnumeration<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.qualifiedJavaType + "::of)";

        if (type.isBitfield())
            return "new PointerBitfield<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.qualifiedJavaType + "::new)";

        if (type.isAliasForPrimitive())
            return "new Pointer" + Conversions.primitiveClassName(type.girElementInstance.type.qualifiedJavaType) + "(" + identifier + ")";

        if (type.isBoolean())
            return "new PointerBoolean(" + identifier + ")";

        if (type.isPrimitive)
            return "new Pointer" + Conversions.primitiveClassName(type.simpleJavaType) + "(" + identifier + ")";

        return "new PointerProxy<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.qualifiedJavaType + ".fromAddress)";
    }
}
