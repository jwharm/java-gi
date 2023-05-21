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
        return ! (type != null && (! type.isPointer()) && (type.isPrimitive ||
                type.isAliasForPrimitive() ||
                type.isBitfield() ||
                type.isEnum()));
    }

    public void writeType(SourceWriter writer, boolean pointerForArray, boolean writeAnnotations) throws IOException {
        writer.write(getType(pointerForArray, writeAnnotations));
    }

    public void writeName(SourceWriter writer) throws IOException {
        writer.write(getName());
    }

    public void writeTypeAndName(SourceWriter writer, boolean pointerForArray) throws IOException {
        writeType(writer, pointerForArray, true);
        writer.write(" ");
        writeName(writer);
    }

    public void marshalJavaToNative(SourceWriter writer, String identifier, boolean pointerForArray, boolean upcall) throws IOException {
        writer.write(marshalJavaToNative(identifier, pointerForArray, upcall));
    }

    public void marshalNativeToJava(SourceWriter writer, String identifier, boolean upcall) throws IOException {
        writer.write(marshalNativeToJava(identifier, upcall));
    }

    private String getAnnotations(Type type, boolean writeAnnotations) {
        if (writeAnnotations && (! type.isPrimitive) && (!type.isVoid()) && (this instanceof Parameter p))
            return p.nullable ? "@Nullable " : p.notnull ? "@NotNull " : "";

        return "";
    }

    private String getType(boolean pointerForArray, boolean writeAnnotations) {

        if (type != null)
            return getAnnotations(type, writeAnnotations) + getType(type);

        if (array != null && array.array != null && "gchar***".equals(array.cType))
            return "java.lang.String[][]";

        if (array != null && array.type != null)
            return getAnnotations(array.type, writeAnnotations)
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

        if (type.qualifiedJavaType.equals("java.lang.foreign.MemorySegment"))
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
            return "MemorySegment.NULL /* unsupported */";

        if (array != null && array.type != null)
            return (array.size(upcall) != null)
                    ? marshalJavaArrayToNative(array, identifier)
                    : (pointerForArray ? marshalJavaPointerToNative(array.type, identifier, upcall)
                    : marshalJavaArrayToNative(array, identifier));

        if (this instanceof Field f && f.callback != null)
            return identifier + ".toCallback()";

        return "MemorySegment.NULL /* unsupported */";
    }

    protected String marshalJavaToNative(Type type, String identifier, boolean upcall) {
        // When ownership is transferred, we must not free the allocated memory -> use global scope
        String allocator = (this instanceof Parameter p && "full".equals(p.transferOwnership))
                ? "SegmentAllocator.nativeAllocator(SegmentScope.global())" : "_arena";

        if (type.cType != null && type.cType.endsWith("**"))
            return marshalJavaPointerToNative(type, identifier, upcall);

        if (type.qualifiedJavaType.equals("java.lang.String"))
            return "Interop.allocateNativeString(" + identifier + ", _arena)";

        if (type.qualifiedJavaType.equals("java.lang.foreign.MemorySegment"))
            return identifier;

        if (type.isPointer() && (type.isPrimitive || type.isBitfield() || type.isEnum()))
            return identifier + ".handle()";

        if (type.isBoolean())
            return identifier + " ? 1 : 0";

        if (type.girElementInstance != null)
            return type.girElementInstance.getInteropString(identifier, type.isPointer());

        return identifier;
    }

    private String marshalJavaArrayToNative(Array array, String identifier) {
        // When ownership is transferred, we must not free the allocated memory -> use global scope
        String allocator = (this instanceof Parameter p && "full".equals(p.transferOwnership))
                ? "SegmentAllocator.nativeAllocator(SegmentScope.global())" : "_arena";

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
                    + "Values(" + identifier + "), " + zeroTerminated + ", " + allocator + ")";

        if (type.isRecord() && (!type.isPointer()))
            return "Interop.allocateNativeArray(" + identifier + ", " + type.qualifiedJavaType
                    + ".getMemoryLayout(), " + zeroTerminated + ", " + allocator + ")";

        return "Interop.allocateNativeArray(" + identifier + ", " + zeroTerminated + ", " + allocator + ")";
    }

    private String marshalJavaPointerToNative(Type type, String identifier, boolean upcall) {
        if (upcall && "java.lang.String".equals(type.qualifiedJavaType))
            return "Interop.allocateNativeString(" + identifier + ", _arena)";

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
        String free = (this instanceof Parameter p && "full".equals(p.transferOwnership)) ? "true" : "false";

        if ("java.lang.String".equals(type.qualifiedJavaType))
            return "Interop.getStringFrom(" + identifier + ", " + free + ")";

        if (type.isEnum())
            return type.qualifiedJavaType + ".of(" + identifier + ")";

        if (type.isBitfield() || type.isAliasForPrimitive())
            return "new " + type.qualifiedJavaType + "(" + identifier + ")";

        if (type.isCallback())
            return "null /* Unsupported parameter type */";

        if (type.isClass() || type.isInterface() || type.isAlias() || type.isRecord() || type.isUnion()) {
            String cacheFunction = "InstanceCache.get";
            if (type.hasGType())
                cacheFunction = "InstanceCache.getForType";
            else if (type.isTypeClass())
                cacheFunction = "InstanceCache.getForTypeClass";
            return "(" + type.qualifiedJavaType + ") " + cacheFunction + "(" + identifier + ", " + type.constructorName + ")";
        }

        if (type.isBoolean())
            return identifier + " != 0";

        return identifier;
    }

    private String marshalNativeToJavaArray(Array array, String identifier, boolean upcall) {
        String free = (this instanceof Parameter p && "full".equals(p.transferOwnership)) ? "true" : "false";
        Type type = array.type;

        if ("java.lang.String".equals(type.qualifiedJavaType))
            return "Interop.getStringArrayFrom(" + identifier + ", " + array.size(upcall) + ", " + free + ")";

        if ("java.lang.foreign.MemorySegment".equals(type.qualifiedJavaType))
            return "Interop.getAddressArrayFrom(" + identifier + ", " + array.size(upcall) + ", " + free + ")";

        if (type.isEnum())
            return "new PointerEnumeration<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.qualifiedJavaType + "::of)"
                    + ".toArray((int) " + array.size(upcall) + ", " + type.qualifiedJavaType + ".class, " + free + ")";

        if (type.isBitfield())
            return "new PointerBitfield<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.qualifiedJavaType + "::new)"
                    + ".toArray((int) " + array.size(upcall) + ", " + type.qualifiedJavaType + ".class, " + free + ")";

        if (type.isAliasForPrimitive())
            return type.qualifiedJavaType + ".fromNativeArray(" + identifier + ", " + array.size(upcall) + ", " + free + ")";

        if (type.isPrimitive)
            return "Interop.get" + Conversions.primitiveClassName(array.type.simpleJavaType) + "ArrayFrom("
                    + identifier + ", " + array.size(upcall) + ", " + identifier + ".scope(), " + free + ")";

        if (type.girElementInstance instanceof Record && (! type.isPointer()))
            return "new PointerProxy<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.constructorName + ")"
                    + ".toArrayOfStructs((int) " + array.size(upcall) + ", " + type.qualifiedJavaType + ".class, "
                    + type.qualifiedJavaType + ".getMemoryLayout())";

        return "new PointerProxy<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.constructorName + ")"
                + ".toArray((int) " + array.size(upcall) + ", " + type.qualifiedJavaType + ".class, false)";
    }

    private String marshalNativetoJavaPointer(Type type, String identifier) {
        if ("java.lang.String".equals(type.qualifiedJavaType))
            return "new PointerString(" + identifier + ")";

        if ("java.lang.foreign.MemorySegment".equals(type.qualifiedJavaType))
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

        return "new PointerProxy<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.constructorName + ")";
    }
    
    public String getGTypeDeclaration() {
        if (array != null) {
            if (array.type != null && "utf8".equals(array.type.name)) {
                return "io.github.jwharm.javagi.types.Types.G_TYPE_STRV";
            }
            // Other array types are not supported yet, but could be added here
            return "io.github.jwharm.javagi.types.Types.G_TYPE_BOXED";
        }
        if (type == null) {
            return "io.github.jwharm.javagi.types.Types.G_TYPE_BOXED";
        }
        if (type.isPrimitive) {
            return switch(type.cType) {
                case "gboolean" -> "io.github.jwharm.javagi.types.Types.G_TYPE_BOOLEAN";
                case "gchar", "gint8" -> "io.github.jwharm.javagi.types.Types.G_TYPE_CHAR";
                case "guchar", "guint8" -> "io.github.jwharm.javagi.types.Types.G_TYPE_UCHAR";
                case "gint", "gint32" -> "io.github.jwharm.javagi.types.Types.G_TYPE_INT";
                case "guint", "guint32", "gunichar" -> "io.github.jwharm.javagi.types.Types.G_TYPE_UINT";
                case "glong" -> "io.github.jwharm.javagi.types.Types.G_TYPE_LONG";
                case "gulong" -> "io.github.jwharm.javagi.types.Types.G_TYPE_ULONG";
                case "gint64" -> "io.github.jwharm.javagi.types.Types.G_TYPE_INT64";
                case "guint64" -> "io.github.jwharm.javagi.types.Types.G_TYPE_UINT64";
                case "gpointer", "gconstpointer", "gssize", "gsize",
                        "goffset", "gintptr", "guintptr" -> "io.github.jwharm.javagi.types.Types.G_TYPE_POINTER";
                case "gdouble" -> "io.github.jwharm.javagi.types.Types.G_TYPE_DOUBLE";
                case "gfloat" -> "io.github.jwharm.javagi.types.Types.G_TYPE_FLOAT";
                case "none" -> "io.github.jwharm.javagi.types.Types.G_TYPE_NONE";
                case "utf8", "filename" -> "io.github.jwharm.javagi.types.Types.G_TYPE_STRING";
                default -> "UNKNOWN: " + type.cType;
            };
        }
        if (type.qualifiedJavaType.equals("java.lang.String")) {
            return "io.github.jwharm.javagi.types.Types.G_TYPE_STRING";
        }
        if (type.qualifiedJavaType.equals("java.lang.foreign.MemorySegment")) {
            return "io.github.jwharm.javagi.types.Types.G_TYPE_POINTER";
        }
        if (type.qualifiedJavaType.equals("org.gnome.gobject.GObject")) {
            return "io.github.jwharm.javagi.types.Types.G_TYPE_OBJECT";
        }
        RegisteredType rt = (type.isAlias() && (! type.isAliasForPrimitive()))
                ? type.girElementInstance.type.girElementInstance
                : type.girElementInstance;
        if (rt != null) {
            if (rt.isInstanceOf("org.gnome.gobject.ParamSpec")) {
                return "io.github.jwharm.javagi.types.Types.G_TYPE_PARAM";
            }
            if (rt.isInstanceOf("org.gnome.glib.Variant")) {
                return "io.github.jwharm.javagi.types.Types.G_TYPE_VARIANT";
            }
            if (rt.getType != null) {
                return type.qualifiedJavaType + ".gtype";
            }
        }
        if (type.qualifiedJavaType.equals("org.gnome.glib.Type")) {
            return "org.gnome.gobject.GObjects.gtypeGetType()";
        }
        return "io.github.jwharm.javagi.types.Types.G_TYPE_BOXED";
    }
    
    public String getValueSetter(String gvalueIdentifier, String gTypeDeclaration, String payloadIdentifier) {
        // First, check for fundamental classes with their own GValue setters
        if (type != null) {
            RegisteredType rt = 
                    (type.isAlias() && (type.girElementInstance != null && type.girElementInstance.type != null)) 
                    ? type.girElementInstance.type.girElementInstance : type.girElementInstance;
            if (rt instanceof Class cls && cls.setValueFunc != null) {
                GirElement setter = module().cIdentifierLookupTable.get(cls.setValueFunc);
                if (setter instanceof Function function) {
                    String setValueFunc = Conversions.toLowerCaseJavaName(function.name);
                    String clsName = Conversions.convertToJavaType(rt.getNamespace().globalClassName, false, rt.getNamespace());
                    return rt.getNamespace().globalClassPackage + "." + clsName 
                            + "." + setValueFunc + "(" + gvalueIdentifier + ", " + payloadIdentifier + ")";
                }
            }
        }
        // Other, known types
        String setValue = switch (gTypeDeclaration) {
            case "io.github.jwharm.javagi.types.Types.G_TYPE_BOOLEAN" -> "setBoolean";
            case "io.github.jwharm.javagi.types.Types.G_TYPE_CHAR" -> "setSchar";
            case "io.github.jwharm.javagi.types.Types.G_TYPE_DOUBLE" -> "setDouble";
            case "io.github.jwharm.javagi.types.Types.G_TYPE_FLOAT" -> "setFloat";
            case "io.github.jwharm.javagi.types.Types.G_TYPE_INT" -> "setInt";
            case "io.github.jwharm.javagi.types.Types.G_TYPE_UINT" -> "setUint";
            case "io.github.jwharm.javagi.types.Types.G_TYPE_LONG" -> "setLong";
            case "io.github.jwharm.javagi.types.Types.G_TYPE_ULONG" -> "setUlong";
            case "io.github.jwharm.javagi.types.Types.G_TYPE_INT64" -> "setInt64";
            case "io.github.jwharm.javagi.types.Types.G_TYPE_UINT64" -> "setUint64";
            case "io.github.jwharm.javagi.types.Types.G_TYPE_STRING" -> "setString";
            case "io.github.jwharm.javagi.types.Types.G_TYPE_POINTER" -> "setPointer";
            case "io.github.jwharm.javagi.types.Types.G_TYPE_PARAM" -> "setParam";
            case "io.github.jwharm.javagi.types.Types.G_TYPE_VARIANT" -> "setVariant";
            case "io.github.jwharm.javagi.types.Types.G_TYPE_BOXED", "io.github.jwharm.javagi.types.Types.G_TYPE_STRV" -> "setBoxed";
            case "org.gnome.gobject.GObjects.gtypeGetType()" -> "setGtype";
            default -> type.isEnum() ? "setEnum" : type.isBitfield() ? "setFlags" : type.isRecord() ? "setBoxed" : "setObject";
        };
        return gvalueIdentifier + "." + switch(setValue) {
            case "setEnum", "setFlags" -> setValue + "(" + payloadIdentifier + ".getValue())";
            case "setBoxed" -> setValue + "(" + marshalJavaToNative(payloadIdentifier, false, false) + ")";
            case "setObject" -> setValue + "((org.gnome.gobject.GObject) " + payloadIdentifier + ")";
            default -> setValue + "(" + payloadIdentifier + ")";
        };
    }
}
