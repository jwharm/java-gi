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

public class Parameter extends Variable {

    public String transferOwnership;
    public boolean nullable;
    public boolean notnull;
    public String direction;
    public String closure;
    public String destroy;
    public String callerAllocates;
    public Scope scope;

    public boolean varargs = false;

    /**
     * An array with a "length" attribute which refers to this parameter.
     * This field is initialized in {@link Module#link()}.
     */
    public Array linkedArray;

    /**
     * A parameter with a "destroy" attribute which refers to this parameter.
     * This field is initialized in {@link Module#link()}.
     */
    public Parameter linkedParameter;

    public Parameter(GirElement parent, String name, String transferOwnership, String nullable,
                     String allowNone, String optional, String direction, String closure,
                     String destroy, String callerAllocates, String scope) {

        super(parent);
        this.name = Conversions.toLowerCaseJavaName(name);
        this.transferOwnership = transferOwnership;
        this.nullable = "1".equals(nullable) || "1".equals(allowNone) || "1".equals(optional);
        this.notnull = "0".equals(nullable) || "0".equals(allowNone) || "0".equals(optional);
        this.direction = direction;
        this.closure = closure;
        this.destroy = destroy;
        this.callerAllocates = callerAllocates;
        this.scope = Scope.from(scope);

        // When scope="notified" without reference to a GDestroyNotify parameter,
        // fallback to global scope.
        if (this.scope == Scope.NOTIFIED && this.destroy == null) {
            this.scope = Scope.FOREVER;
        }
    }

    /**
     * Return the parameter that is referred to by index in a GIR attribute.
     * For example: array length="2" refers to the second parameter for the 
     * length of that particular array.
     * The index counts from zero and ignores the instance-parameter.
     */
    public Parameter getParameterAt(String indexAttr) {
        if (indexAttr == null) {
            return null;
        }
        // Parse the value of the 'length' attribute
        int index;
        try {
            index = Integer.parseInt(indexAttr);
        } catch (NumberFormatException nfe) {
            return null;
        }
        // Get the <parameters> node
        Parameters params;
        if (this instanceof ReturnValue) {
            params = ((CallableType) parent).getParameters();
        } else {
            params = (Parameters) parent;
        }
        // Find the parameter that was specified in the value of the 'length' attribute.
        // Ignore the instance parameter.
        Parameter p0 = params.parameterList.get(0);
        if (p0 instanceof InstanceParameter) {
            return params.parameterList.get(index + 1);
        } else {
            return params.parameterList.get(index);
        }
    }

    public boolean isProxy() {
        if (type == null) {
            return false;
        }
        if (type.isAlias()) {
            Alias a = (Alias) type.girElementInstance;
            return (a == null || a.getTargetType() == Alias.TargetType.CLASS) || (a.getTargetType() == Alias.TargetType.INTERFACE);
        }
        // A pointer to a proxy is not a proxy
        if (type.cType != null && type.cType.endsWith("**")) {
            return false;
        }
        return type.isClass() || type.isRecord() || type.isInterface() || type.isUnion();
    }

    public boolean isGObject() {
        if (! isProxy())
            return false;

        if (type.girElementInstance == null)
            return false;

        if (type.isClass() || type.isInterface())
            return type.girElementInstance.isInstanceOf("org.gnome.gobject.GObject");

        if (type.isAliasForPrimitive())
            return false;

        if (type.isAlias()) {
            Alias a = (Alias) type.girElementInstance;
            if (a.getTargetType() == Alias.TargetType.CLASS || a.getTargetType() == Alias.TargetType.INTERFACE) {
                return a.type.girElementInstance.isInstanceOf("org.gnome.gobject.GObject");
            }
        }

        return false;
    }

    public boolean isAliasForPrimitive() {
        return type != null && type.isAliasForPrimitive();
    }

    /**
     * Whether this parameter must receive special treatment as an out-parameter
     * @return True if the direction attribute exists and contains "out", AND the parameter type
     *         is NOT a Proxy object, a primitive alias, or an array with unknown size. (For Proxy
     *         objects, we can simply pass the memory address, and don't need to do anything
     *         special. For aliases, we can pass the alias object. Arrays with unknown size are
     *         already marshalled to Pointer objects.
     */
    public boolean isOutParameter() {
        if (array != null && array.size(false) == null)
            return false;

        return direction != null && direction.contains("out")
                && (type == null || type.isPointer() || (type.cType != null && type.cType.endsWith("gsize")))
                && (!isProxy())
                && (!isAliasForPrimitive());
    }

    public boolean isInstanceParameter() {
        return (this instanceof InstanceParameter);
    }

    public boolean isUserDataParameter() {
        // Closure parameters: the user_data parameter has attribute "closure" set
        if (parent.parent instanceof Closure) {
            return (closure != null);
        }

        // Method parameters that pass a user_data pointer to a closure
        else {
            // A user_data parameter must have a type (pointer)
            if (type == null)
                return false;

            // The c:type must be a gpointer or gconstpointer
            if (! ("gpointer".equals(type.cType) || "gconstpointer".equals(type.cType)))
                return false;

            // Loop through the callback parameter(s)
            Parameters parameters = (Parameters) parent;
            for (Parameter p : parameters.parameterList) {
                if (p.type != null && p.type.isCallback()) {

                    // The closure attribute points to the user_data parameter
                    if (p.closure != null) {
                        Parameter userData = getParameterAt(p.closure);
                        // Is this the user_data parameter that is referred to?
                        return userData.name.equals(this.name);
                    }

                }
            }
            return false;
        }
    }

    public boolean isDestroyNotifyParameter() {
        return (type != null) && "GDestroyNotify".equals(type.cType);
    }

    public boolean isErrorParameter() {
        return (type != null) && "GError**".equals(type.cType);
    }

    public boolean isArrayLengthParameter() {
        return linkedArray != null;
    }

    /**
     * We don't need to perform a null-check on parameters that are not nullable, or not user-specified (instance param,
     * gerror, user_data or array length), or primitive values.
     *
     * @return true iff this parameter is nullable, is user-specified, and is not a primitive value
     */
    @Override
    public boolean checkNull() {
        return (! notnull)
                && (! (isInstanceParameter() || isErrorParameter()
                || isUserDataParameter() || isDestroyNotifyParameter()
                || isArrayLengthParameter() || varargs))
                && super.checkNull();
    }

    /**
     * Whether this parameter needs a memory allocator (Arena) for marshaling
     *
     * @return true if an enclosing Arena scope must be generated
     */
    @Override
    public boolean allocatesMemory() {
         return super.allocatesMemory()
                 || isOutParameter()
                 || (isAliasForPrimitive() && type.isPointer())
                 || (type != null && type.isCallback() && (! scope.in(Scope.NOTIFIED, Scope.FOREVER)));
    }

    /**
     * Generate code to do pre-processing of the parameter before the function call. This will generate a null check for
     * NotNull parameters, and generate pointer allocation logic for pointer parameters.
     *
     * @param writer The source code file writer
     * @throws IOException Thrown when an error occurs while writing to the file
     */
    public void generatePreprocessing(SourceWriter writer) throws IOException {
        // Generate null-check
        // Don't null-check parameters that are hidden from the Java API, or primitive values
        if (! (isInstanceParameter() || isErrorParameter() || isUserDataParameter()
                || isDestroyNotifyParameter() || isArrayLengthParameter() || varargs
                || (type != null && type.isPrimitive && (! type.isPointer())))) {
            if (notnull) {
                writer.write("java.util.Objects.requireNonNull(" + name
                        + ", \"" + "Parameter '" + name + "' must not be null\");\n");
            }
        }

        // Generate pointer allocation
        if (isOutParameter() && array != null && array.size(false) != null && "1".equals(callerAllocates)) {
            writer.write("MemorySegment _" + name + "Pointer = (MemorySegment) ");
            marshalJavaToNative(writer, name + ".get()");
            writer.write(";\n");
        } else if (isOutParameter() || (isAliasForPrimitive() && type.isPointer())) {
            writer.write("MemorySegment _" + name + "Pointer = _arena.allocate(" + Conversions.getValueLayoutPlain(type) + ");\n");
        }

        // Array length parameter: generate local variable that contains the length
        // array length
        if (isArrayLengthParameter()) {
            writeTypeAndName(writer);
            writer.write (" = ");
            if (isOutParameter()) {
                writer.write("new Out<>();\n");
            } else {
                Parameter arrayParam = (Parameter) linkedArray.parent;
                String arrayLength = arrayParam.name + ".length";

                // Force lossy conversion if needed
                String cast = "";
                if (type != null) {
                    String javaType = type.qualifiedJavaType;
                    if ("byte".equals(javaType) || "short".equals(javaType)) {
                        cast = "(" + javaType + ") ";
                    }
                }

                // For out parameter arrays, generate "arg.get().length", with null-checks for both arg and arg.get()
                if (arrayParam.isOutParameter())
                    arrayLength = arrayParam.name + ".get() == null ? 0 : " + cast + arrayParam.name + ".get().length";

                writer.write(arrayParam.name + " == null ? 0 : " + cast + arrayLength + ";\n");
            }
        }

        // Callback functions with an arena that is closed from a DestroyNotify callback
        if (scope == Scope.NOTIFIED && destroy != null) {
            writer.write("final Arena _");
            writeName(writer);
            writer.write("Scope = Arena.ofConfined();\n");
        }
    }

    /**
     * Generate code to do post-processing of the parameter after the function call.
     *
     * @param writer The source code file writer
     * @throws IOException Thrown when an error occurs while writing to the file
     */
    public void generatePostprocessing(SourceWriter writer) throws IOException {
        if (isOutParameter() || (isAliasForPrimitive() && type.isPointer())) {
            if (array == null) {
                // First the regular (non-array) out-parameters. These could include an out-parameter with 
                // the length of an array out-parameter, so we have to process these first.
                if (checkNull()) {
                    writer.write("if (" + name + " != null) ");
                }
                writer.write(name + (isAliasForPrimitive() ? ".setValue(" : ".set("));
                String identifier = "_" + name + "Pointer.get(" + Conversions.getValueLayoutPlain(type) + ", 0)";
                if (isAliasForPrimitive() || (type.isPrimitive && type.isPointer())) {
                    writer.write(identifier);
                    if (type.isBoolean()) writer.write(" != 0");
                    writer.write(");\n");
                } else {
                    writer.write(marshalNativeToJava(type, identifier, false) + ");\n");
                }
            } else {
                // Secondly, process the array out parameters
                String len = array.size(false);
                String valuelayout = Conversions.getValueLayoutPlain(array.type);
                if (isOutParameter() && len != null && "1".equals(callerAllocates)) {
                    writer.write("if (" + name + " != null) " + name + ".set(");
                    // Out-parameter array
                    if (array.type.isPrimitive) {
                        writer.write("_" + name + "Pointer.toArray(" + valuelayout + "));\n");
                    } else {
                        marshalNativeToJava(writer, "_" + name + "Pointer", false);
                        writer.write(");\n");
                    }
                } else if (array.type.isPrimitive && (! array.type.isBoolean())) {
                    // Array of primitive values
                    writer.write("if (" + name + " != null) " + name + ".set(");
                    writer.write("_" + name + "Pointer.get(ValueLayout.ADDRESS, 0).reinterpret("
                            + len + " * " + valuelayout + ".byteSize(), _arena, null).toArray(" + valuelayout + "));\n");
                } else {
                    // Array of proxy objects
                    writer.write("if (" + name + " != null) {\n");
                    writer.increaseIndent();
                    writer.write(array.type.qualifiedJavaType + "[] _" + name + "Array = new " + array.type.qualifiedJavaType + "[" + len + "];\n");
                    writer.write("for (int _idx = 0; _idx < " + len + "; _idx++) {\n");
                    writer.write("    var _object = _" + name + "Pointer.get(" + valuelayout + ", _idx);\n");
                    writer.write("    _" + name + "Array[_idx] = ");
                    writer.write(marshalNativeToJava(array.type, "_object", false) + ";\n");
                    writer.write("    }\n");
                    writer.write(name + ".set(_" + name + "Array);\n");
                    writer.decreaseIndent();
                    writer.write("}\n");
                }
            }
        }

        // If the parameter has attribute transfer-ownership="full", we must register a reference, because
        // the native code is going to call un_ref() at some point while we still keep a pointer in the InstanceCache.
        // Only for GObjects where ownership is fully transferred away, unless it's an out parameter or a pointer.
        if (isGObject()
                && "full".equals(transferOwnership)
                && (! isOutParameter())
                && (type.cType == null || (! type.cType.endsWith("**")))) {
            String param = isInstanceParameter() ? "this" : name;
            if (checkNull()) writer.write("if (" + param + " != null) ");
            writer.write(param + ".ref();\n");
        }
        // Same, but for structs/unions: Disable the cleaner
        if (type != null && (type.isUnion() || type.isRecord())
                && "full".equals(transferOwnership)
                && (! isOutParameter())
                && (type.cType == null || (! type.cType.endsWith("**")))) {
            String param = isInstanceParameter() ? "this" : name;
            if (checkNull()) writer.write("if (" + param + " != null) ");
            writer.write("MemoryCleaner.yieldOwnership(" + param + ".handle());\n");
        }
    }

    /**
     * Generate code to do preprocessing of parameters before an upcall into a Java callback method
     *
     * @param writer The source code file writer
     * @throws IOException Thrown when an error occurs while writing to the file
     */
    public void generateUpcallPreprocessing(SourceWriter writer) throws IOException {
        if (isAliasForPrimitive() && type.isPointer()) {
            writer.write("MemorySegment %sParam = %s.reinterpret(%s.byteSize(), _arena, null);\n"
                    .formatted(name, name, Conversions.getValueLayoutPlain(type)));
            String typeStr = Conversions.getValueLayoutPlain(type.girElementInstance.type);
            writer.write(type.qualifiedJavaType + " _" + name + "Alias = new " + type.qualifiedJavaType + "(" + name + "Param.get(" + typeStr + ", 0));\n");
        } else if (isOutParameter()) {
            if (type != null) {
                writer.write("MemorySegment %sParam = %s.reinterpret(%s.byteSize(), _arena, null);\n"
                        .formatted(name, name, Conversions.getValueLayoutPlain(type)));
                String typeStr = type.qualifiedJavaType;
                if (type.isPrimitive) typeStr = Conversions.primitiveClassName(typeStr);
                writer.write("Out<" + typeStr + "> _" + name + "Out = new Out<>(");
                if (type.isPrimitive || type.isAliasForPrimitive()) {
                    String layout = Conversions.getValueLayoutPlain(type);
                    writer.write(name + "Param.get(" + layout + ", 0)");
                    if (type.isBoolean()) writer.write(" != 0");
                    writer.write(");\n");
                } else {
                    String identifier = name + "Param";
                    if (type.isEnum() || type.isBitfield()) {
                        identifier = name + "Param.get(ValueLayout.JAVA_INT, 0)";
                    }
                    writer.write(marshalNativeToJava(type, identifier, true) + ");\n");
                }
            }
            if (array != null) {
                writeType(writer, true);
                writer.write(" _" + name + "Out = new Out<>(");
                marshalNativeToJava(writer, name, true);
                writer.write(");\n");
            }
        }
    }

    /**
     * Generate code to do postprocessing of parameters after an upcall into a Java callback method
     *
     * @param writer The source code file writer
     * @throws IOException Thrown when an error occurs while writing to the file
     */
    public void generateUpcallPostprocessing(SourceWriter writer) throws IOException {
        if (type != null && type.isAliasForPrimitive() && type.isPointer()) {
            String typeStr = Conversions.getValueLayoutPlain(type.girElementInstance.type);
            writer.write(name + "Param.set(" + typeStr + ", 0, _" + name + "Alias.getValue());\n");
        } else if (isOutParameter()) {
            if (type != null) {
                String typeStr = Conversions.getValueLayoutPlain(type);
                String identifier = marshalJavaToNative(type, "_" + name + "Out.get()");
                if (type.isPrimitive || type.isAliasForPrimitive()) {
                    identifier = "_" + name + "Out.get()";
                    if (type.isBoolean()) identifier += " ? 1 : 0";
                }
                if (type.isEnum() || type.isBitfield()) {
                    identifier = "_" + name + "Out.get().getValue()";
                }
                writer.write(name + "Param.set(" + typeStr + ", 0, " + identifier + ");\n");
            }
            if (array != null) {
                // TODO: Copy the array from the Out<> parameter to the provided memory address.
            }
        }
    }
}
