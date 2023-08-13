/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2023 Jan-Willem Harmannij
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

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

    public void writeType(SourceWriter writer, boolean writeAnnotations) throws IOException {
        writer.write(getType(writeAnnotations));
    }

    public void writeName(SourceWriter writer) throws IOException {
        writer.write(getName());
    }

    public void writeTypeAndName(SourceWriter writer) throws IOException {
        writeType(writer, true);
        writer.write(" ");
        writeName(writer);
    }

    public void marshalJavaToNative(SourceWriter writer, String identifier) throws IOException {
        writer.write(marshalJavaToNative(identifier));
    }

    public void marshalNativeToJava(SourceWriter writer, String identifier, boolean upcall) throws IOException {
        writer.write(marshalNativeToJava(identifier, upcall));
    }

    private String getAnnotations(Type type, boolean writeAnnotations) {
        if (writeAnnotations && (! type.isPrimitive) && (!type.isVoid()) && (this instanceof Parameter p))
            return p.nullable ? "@Nullable " : p.notnull ? "@NotNull " : "";

        return "";
    }

    private String getType(boolean writeAnnotations) {

        if (type != null && type.isActuallyAnArray())
            return getAnnotations(type, writeAnnotations) + getType(type) + "[]";

        if (type != null)
            return getAnnotations(type, writeAnnotations) + getType(type);

        if (array != null && array.array != null && "gchar***".equals(array.cType))
            return "java.lang.String[][]";

        if (array != null && array.type != null)
            return getAnnotations(array.type, writeAnnotations)
                    + getArrayType(array.type);

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

        if (type.isPrimitive && type.isPointer())
            return "java.lang.foreign.MemorySegment";

        if (type.isBitfield() && type.isPointer())
            return "java.lang.foreign.MemorySegment";

        if (type.isEnum() && type.isPointer())
            return "java.lang.foreign.MemorySegment";

        return type.qualifiedJavaType;
    }

    private String getArrayType(Type type) {
        if (this instanceof Parameter p && p.isOutParameter())
            return "Out<" + type.qualifiedJavaType + "[]>";

        return type.qualifiedJavaType + "[]";
    }

    private String getName() {
        return "...".equals(name) ? "varargs" : name;
    }

    private String marshalJavaToNative(String identifier) {
        if (type != null)
            return marshalJavaToNative(type, identifier);

        if (array != null && array.array != null)
            return "MemorySegment.NULL /* unsupported */";

        if (array != null && array.type != null)
            return marshalJavaArrayToNative(array, identifier);

        if (this instanceof Field f && f.callback != null)
            return identifier + ".toCallback()";

        return "MemorySegment.NULL /* unsupported */";
    }

    protected String marshalJavaToNative(Type type, String identifier) {
        if (type.isActuallyAnArray())
            return "Interop.allocateNativeArray(" + identifier + ", false, _arena)";

        if (type.qualifiedJavaType.equals("java.lang.String"))
            return "Interop.allocateNativeString(" + identifier + ", _arena)";

        if (! (this instanceof Parameter p && p.isOutParameter()))
            if (type.cType != null && type.cType.endsWith("**"))
                return identifier;

        if (type.qualifiedJavaType.equals("java.lang.foreign.MemorySegment"))
            return identifier;

        if (type.isPointer() && (type.isPrimitive || type.isBitfield() || type.isEnum()))
            return identifier;

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

    private String marshalNativeToJava(String identifier, boolean upcall) {
        if (type != null) {
            if (type.isActuallyAnArray())
                return marshalNativeToJavaArray(type, null, identifier);

            if (type.isPointer() && (type.isPrimitive || type.isBitfield() || type.isEnum()))
                return identifier;

            return marshalNativeToJava(type, identifier, upcall);
        }

        if (array != null && array.array != null)
            return "null /* unsupported */";

        if (array != null && array.type != null)
            return marshalNativeToJavaArray(array.type, array.size(upcall), identifier);

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
            String cache = upcall ? "false" : "true";
            return "(" + type.qualifiedJavaType + ") " + cacheFunction + "(" + identifier + ", " + type.constructorName + ", " + cache + ")";
        }

        if (type.isBoolean())
            return identifier + " != 0";

        return identifier;
    }

    private String marshalNativeToJavaArray(Type type, String size, String identifier) {
        String free = (this instanceof Parameter p && "full".equals(p.transferOwnership)) ? "true" : "false";

        // GArray stores the length in the len field
        if (size == null && array != null && ("GLib.Array".equals(array.name))) {
            size = "new org.gnome.glib.Array(" + identifier + ").readLen()";
        } else if (size == null && array != null && ("GLib.PtrArray".equals(array.name))) {
            size = "new org.gnome.glib.PtrArray(" + identifier + ").readLen()";
        } else if (size == null && array != null && ("GLib.ByteArray".equals(array.name))) {
            size = "new org.gnome.glib.ByteArray(" + identifier + ").readLen()";
        }

        // Null-terminated array
        if (size == null) {
            if ("java.lang.String".equals(type.qualifiedJavaType))
                return "Interop.getStringArrayFrom(" + identifier + ", " + free + ")";

            if ("java.lang.foreign.MemorySegment".equals(type.qualifiedJavaType))
                return "Interop.getAddressArrayFrom(" + identifier + ", " + free + ")";

            if (type.isEnum() || type.isBitfield())
                return "Arrays.fromIntPointer(" + identifier + ", (int) "
                        + type.qualifiedJavaType + ".class, " + type.qualifiedJavaType + "::of)";

            if (type.isAliasForPrimitive())
                return type.qualifiedJavaType + ".fromNativeArray(" + identifier + ", " + free + ")";

            if (type.isPrimitive)
                return "Interop.get" + Conversions.primitiveClassName(array.type.simpleJavaType) + "ArrayFrom("
                        + identifier + ", " + identifier + ".scope(), " + free + ")";

            if (type.girElementInstance instanceof Record && (! type.isPointer()) &&
                    (! (array != null && "GLib.PtrArray".equals(array.name))))
                return "Arrays.fromStructPointer(" + identifier + ", " + type.qualifiedJavaType + ".class, "
                        + type.constructorName + ", " + type.qualifiedJavaType + ".getMemoryLayout())";

            return "Arrays.fromPointer(" + identifier + ", " + type.qualifiedJavaType + ".class, "
                    + type.constructorName + ")";
        }

        // Array with known size
        if ("java.lang.String".equals(type.qualifiedJavaType))
            return "Interop.getStringArrayFrom(" + identifier + ", " + size + ", " + free + ")";

        if ("java.lang.foreign.MemorySegment".equals(type.qualifiedJavaType))
            return "Interop.getAddressArrayFrom(" + identifier + ", " + size + ", " + free + ")";

        if (type.isEnum() || type.isBitfield())
            return "Arrays.fromIntPointer(" + identifier + ", (int) " + size + ", "
                    + type.qualifiedJavaType + ".class, " + type.qualifiedJavaType + "::of)";

        if (type.isAliasForPrimitive())
            return type.qualifiedJavaType + ".fromNativeArray(" + identifier + ", " + size + ", " + free + ")";

        if (type.isPrimitive)
            return "Interop.get" + Conversions.primitiveClassName(array.type.simpleJavaType) + "ArrayFrom("
                    + identifier + ", " + size + ", " + identifier + ".scope(), " + free + ")";

        if (type.girElementInstance instanceof Record && (! type.isPointer()) &&
                (! (array != null && "GLib.PtrArray".equals(array.name))))
            return "Arrays.fromStructPointer(" + identifier + ", (int) " + size + ", "
                    + type.qualifiedJavaType + ".class, " + type.constructorName + ", "
                    + type.qualifiedJavaType + ".getMemoryLayout())";

        return "Arrays.fromPointer(" + identifier + ", (int) " + size + ", "
                + type.qualifiedJavaType + ".class, " + type.constructorName + ")";
    }

    public String getGTypeDeclaration() {
        if (array != null) {
            if (array.type != null && "utf8".equals(array.type.name)) {
                return "Types.STRV";
            }
            // Other array types are not supported yet, but could be added here
            return "Types.BOXED";
        }
        if (type == null) {
            return "Types.BOXED";
        }
        if (type.isPrimitive) {
            return switch(type.cType) {
                case "gboolean" -> "Types.BOOLEAN";
                case "gchar", "gint8" -> "Types.CHAR";
                case "guchar", "guint8" -> "Types.UCHAR";
                case "gint", "gint32" -> "Types.INT";
                case "guint", "guint32", "gunichar" -> "Types.UINT";
                case "glong" -> "Types.LONG";
                case "gulong" -> "Types.ULONG";
                case "gint64" -> "Types.INT64";
                case "guint64" -> "Types.UINT64";
                case "gpointer", "gconstpointer", "gssize", "gsize",
                        "goffset", "gintptr", "guintptr" -> "Types.POINTER";
                case "gdouble" -> "Types.DOUBLE";
                case "gfloat" -> "Types.FLOAT";
                case "none" -> "Types.NONE";
                case "utf8", "filename" -> "Types.STRING";
                default -> "UNKNOWN: " + type.cType;
            };
        }
        if (type.qualifiedJavaType.equals("java.lang.String")) {
            return "Types.STRING";
        }
        if (type.qualifiedJavaType.equals("java.lang.foreign.MemorySegment")) {
            return "Types.POINTER";
        }
        if (type.qualifiedJavaType.equals("org.gnome.gobject.GObject")) {
            return "Types.OBJECT";
        }
        RegisteredType rt = (type.isAlias() && (! type.isAliasForPrimitive()))
                ? type.girElementInstance.type.girElementInstance
                : type.girElementInstance;
        if (rt != null) {
            if (rt.isInstanceOf("org.gnome.gobject.ParamSpec")) {
                return "Types.PARAM";
            }
            if (rt.isInstanceOf("org.gnome.glib.Variant")) {
                return "Types.VARIANT";
            }
            if (rt.getType != null) {
                return type.qualifiedJavaType + ".getType()";
            }
        }
        if (type.qualifiedJavaType.equals("org.gnome.glib.Type")) {
            return "org.gnome.gobject.GObjects.gtypeGetType()";
        }
        return "Types.BOXED";
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
            case "Types.BOOLEAN" -> "setBoolean";
            case "Types.CHAR" -> "setSchar";
            case "Types.DOUBLE" -> "setDouble";
            case "Types.FLOAT" -> "setFloat";
            case "Types.INT" -> "setInt";
            case "Types.UINT" -> "setUint";
            case "Types.LONG" -> "setLong";
            case "Types.ULONG" -> "setUlong";
            case "Types.INT64" -> "setInt64";
            case "Types.UINT64" -> "setUint64";
            case "Types.STRING" -> "setString";
            case "Types.POINTER" -> "setPointer";
            case "Types.PARAM" -> "setParam";
            case "Types.VARIANT" -> "setVariant";
            case "Types.BOXED", "Types.STRV" -> "setBoxed";
            case "org.gnome.gobject.GObjects.gtypeGetType()" -> "setGtype";
            default -> type == null ? "UNKNOWN"
                    : type.isEnum() ? "setEnum"
                    : type.isBitfield() ? "setFlags"
                    : type.isRecord() ? "setBoxed"
                    : "setObject";
        };
        return gvalueIdentifier + "." + switch(setValue) {
            case "setEnum", "setFlags" -> setValue + "(" + payloadIdentifier + ".getValue())";
            case "setBoxed" -> setValue + "(" + marshalJavaToNative(payloadIdentifier) + ")";
            case "setObject" -> setValue + "((org.gnome.gobject.GObject) " + payloadIdentifier + ")";
            default -> setValue + "(" + payloadIdentifier + ")";
        };
    }
}
