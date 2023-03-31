package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.util.stream.Stream;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

public class Field extends Variable {

    public final String readable, isPrivate;
    public Callback callback;
    public final String fieldName, callbackType;

    public Field(GirElement parent, String name, String readable, String isPrivate) {
        super(parent);
        this.fieldName = name;
        this.name = Conversions.toLowerCaseJavaName(name);
        this.readable = readable;
        this.isPrivate = isPrivate;
        this.callbackType = Conversions.toCamelCase(this.name, true) + "Callback";
    }
    
    /*
     * Generates a getter and setter method for a field in (the MemoryLayout of) a C struct.
     */
    public void generate(SourceWriter writer) throws IOException {
        // Don't generate a getter/setter for a "disguised" record (often private data)
        if (type != null
                && type.girElementInstance != null
                && type.girElementInstance instanceof Record r 
                && ("1".equals(r.disguised) || r.name.endsWith("Private"))) {
            return;
        }
        // Don't generate a getter/setter for padding/reserved space
        if ("padding".equals(name) || "reserved".equals(name)) {
            return;
        }

        String camelName = Conversions.toCamelCase(this.name, true);
        String upcallName = this.name + "Upcall";
        String setter = (callback != null ? "override" : "write") + camelName;
        String getter = "read" + camelName;

        for (Method method : Stream.concat(parent.methodList.stream(), parent.functionList.stream()).toList()) {
            String n = Conversions.toLowerCaseJavaName(method.name);
            if (n.equals(getter)) getter += "_";
            if (n.equals(setter)) setter += "_";
        }

        // Generate a inner callback class declarations for callback fields
        if (callback != null) {
            writer.write("\n");
            callback.generateFunctionalInterface(writer, callbackType);
        }

        // Generate getter method
        if (callback == null) {
            writer.write("\n");
            writer.write("/**\n");
            writer.write(" * Read the value of the field {@code " + this.fieldName + "}\n");
            writer.write(" * @return The value of the field {@code " + this.fieldName + "}\n");
            writer.write(" */\n");
            writer.write("public ");
            writeType(writer, true, true);
            writer.write(" " + getter + "() {\n");

            if (isApi()) {
                writer.write("    throw Interop.apiError();\n");
            
            // Read the memory segment of an embedded field from the struct (not a pointer)
            } else if ((type != null) && (!type.isPointer()) && (type.isClass() || type.isInterface())) {
                writer.write("    long _offset = getMemoryLayout().byteOffset(MemoryLayout.PathElement.groupElement(\"" + this.fieldName + "\"));\n");
                writer.write("    return ");
                marshalNativeToJava(writer, "((MemoryAddress) handle()).addOffset(_offset)", false);
                writer.write(";\n");
             
            // Read a pointer or primitive value from the struct
            } else {
                String memoryType = getMemoryType();
                if ("ARRAY".equals(memoryType)) memoryType = "MemoryAddress";
                writer.write("    var _scope = getSegmentScope();\n");
                writer.write("    var _result = (" + memoryType + ") getMemoryLayout()\n");
                writer.write("        .varHandle(MemoryLayout.PathElement.groupElement(\"" + this.fieldName + "\"))\n");
                writer.write("        .get(allocatedMemorySegment);\n");
                writer.write("    return ");
                marshalNativeToJava(writer, "_result", false);
                writer.write(";\n");
            }
            writer.write("}\n");
        }

        // Generate override/setter method
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Write a value in the field {@code " + this.fieldName + "}\n");
        writer.write(" * @param " + this.name + " The new value for the field {@code " + this.fieldName + "}\n");
        writer.write(" */\n");
        writer.write("public void " + setter + "(");
        writeTypeAndName(writer, false);
        writer.write(") {\n");
        
        if (isApi()) {
            writer.write("    throw Interop.apiError();\n");
        } else {
            writer.write("    var _scope = getSegmentScope();\n");
            writer.write("    getMemoryLayout()\n");
            writer.write("        .varHandle(MemoryLayout.PathElement.groupElement(\"" + this.fieldName + "\"))\n");
            writer.write("        .set(allocatedMemorySegment, ");
            // Check for null values
            if (checkNull()) {
                writer.write("(Addressable) (" + this.name + " == null ? MemoryAddress.NULL : ");
            }
            marshalJavaToNative(writer, this.name, false, false);
            if (checkNull()) {
                writer.write(")");
            }
            writer.write(");\n");
        }
        writer.write("}\n");

        // For callbacks, generate a second override method with java.lang.reflect.Method parameter
        if (callback != null && callback.parameters != null) {
            writer.write("\n");
            writer.write("private java.lang.reflect.Method _" + this.name + "Method;\n");
            writer.write("\n");
            writer.write("/**\n");
            writer.write(" * Override virtual method {@code " + this.fieldName + "}\n");
            writer.write(" * @param method The method to invoke\n");
            writer.write(" */\n");
            writer.write("public void " + setter + "(java.lang.reflect.Method method) {\n");
            writer.increaseIndent();
            
            if (isApi()) {
                writer.write("throw Interop.apiError();\n");                
            } else {
                writer.write("this._" + this.name + "Method = method;\n");
                writer.write("var _scope = getSegmentScope();\n");

                // Generate function descriptor
                writer.write("FunctionDescriptor _fdesc = ");
                callback.generateFunctionDescriptor(writer);
                writer.write(";\n");

                // Generate method handle
                String className = ((Record) parent).javaName;
                writer.write("MethodHandle _handle = Interop.upcallHandle(MethodHandles.lookup(), " + className + ".class, \"" + upcallName + "\", _fdesc);\n");
                writer.write("MemoryAddress _address = Linker.nativeLinker().upcallStub(_handle.bindTo(this), _fdesc, MemorySession.global()).address();\n");
                writer.write("getMemoryLayout()\n");
                writer.write("    .varHandle(MemoryLayout.PathElement.groupElement(\"" + this.fieldName + "\"))\n");
                writer.write("    .set(allocatedMemorySegment, ");
                writer.write("(Addressable) (method == null ? MemoryAddress.NULL : _address));\n");
            }
            
            writer.decreaseIndent();
            writer.write("}\n");
            writer.write("\n");

            // Generate upcall method
            String methodToInvoke = "this._" + this.name + "Method.invoke";
            callback.generateUpcallMethod(writer, upcallName, methodToInvoke);
        }
    }

    /**
     * Generates a String containing the MemoryLayout definition for this field.
     * @return A String containing the MemoryLayout definition for this field
     */
    public String getMemoryLayoutString() {
        
        // Regular types (not arrays or callbacks)
        if (type != null) {
            
            // Pointers, strings and callbacks are memory addresses
            if (type.isPointer()
                    || "java.lang.String".equals(type.qualifiedJavaType)
                    || "java.lang.foreign.MemoryAddress".equals(type.qualifiedJavaType)
                    || type.isCallback()) {
                return "ValueLayout.ADDRESS.withName(\"" + this.fieldName + "\")";
            }
            
            // Bitfields and enumerations are integers
            if (type.isBitfield() || type.isEnum()) {
                return "ValueLayout.JAVA_INT.withName(\"" + this.fieldName + "\")";
            }
            
            // Primitive types and aliases
            if (type.isPrimitive 
                    || type.isAliasForPrimitive()) {
                return Conversions.getValueLayout(type) + ".withName(\"" + this.fieldName + "\")";
            }
            
            // For Proxy objects we recursively get the memory layout
            return type.qualifiedJavaType + ".getMemoryLayout()" + ".withName(\"" + this.fieldName + "\")";
        }
        
        // Arrays with a fixed size
        if (array != null && array.fixedSize != null) {
            String valueLayout;
            
            // Array of primitive values or pointers
            if (array.type.isPrimitive 
                    || array.type.isBitfield() 
                    || array.type.isEnum() 
                    || array.type.isAliasForPrimitive() 
                    || array.type.isPointer()
                    || array.type.isCallback()
                    || "java.lang.String".equals(array.type.qualifiedJavaType)
                    || "java.lang.foreign.MemoryAddress".equals(array.type.qualifiedJavaType)) {
                
                valueLayout = Conversions.getValueLayout(array.type);
                
            // Proxy objects
            } else {
                valueLayout = array.type.qualifiedJavaType + ".getMemoryLayout()";
            }
            
            return "MemoryLayout.sequenceLayout(" + array.fixedSize + ", " + valueLayout + ").withName(\"" + this.fieldName + "\")";
        }
        
        // Arrays with non-fixed size
        if (array != null) {
            return "ValueLayout.ADDRESS.withName(\"" + this.fieldName + "\")";
        }
        
        // Callbacks
        if (callback != null) {
            return "ValueLayout.ADDRESS.withName(\"" + this.fieldName + "\")";
        }
        
        System.out.printf("Error: Field %s.%s has unknown type\n", parent.name, fieldName);
        return "ValueLayout.ADDRESS.withName(\"" + this.fieldName + "\")";
    }
    
    /**
     * Get the native type of this field. For example "int", "char".
     * An array with fixed size is returned as "ARRAY", a pointer is returned as "MemoryAddress".
     * @return the native type of this field
     */
    public String getMemoryType() {
        if (type != null) {
            return getMemoryType(type);
        } else if (array != null && array.fixedSize != null) {
            return "ARRAY";
        } else {
            return "MemoryAddress";
        }
    }
    
    /**
     * Get the native type of a non-array type
     * @param type the type
     * @return the native type
     */
    public String getMemoryType(Type type) {
        if (type.isPointer() && (type.isPrimitive || type.isBitfield() || type.isEnum())) {
            return "MemoryAddress";
        }
        if (type.isBoolean() || type.isBitfield() || type.isEnum()) {
            return "int";
        }
        if (type.isPrimitive) {
            return type.simpleJavaType;
        }
        if (type.isAliasForPrimitive()) {
            return type.girElementInstance.type.simpleJavaType;
        }
        return "MemoryAddress";
    }
    
    /**
     * Get the byte-size of the types.
     * @param memoryType The result of getMemoryType(). For example: int, byte, ...
     * @return the native byte-size of the provided type
     */
    public int getSize(String memoryType) {
        return switch(memoryType) {
            case "boolean" -> 32; // treated as an integer
            case "byte" -> 8;
            case "char" -> 8;
            case "short" -> 16;
            case "int" -> 32;
            case "long" -> 64; // java long is 64-bits
            case "float" -> 32;
            case "double" -> 64;
            case "MemoryAddress" -> 64; // 64-bits pointer
            case "ARRAY" -> Integer.parseInt(array.fixedSize) * getSize(getMemoryType(array.type));
            default -> 64;
        };
    }
}
