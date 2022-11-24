package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

import io.github.jwharm.javagi.generator.Conversions;

public class Field extends Variable {

    public final String readable, isPrivate;
    public Callback callback;

    public Field(GirElement parent, String name, String readable, String isPrivate) {
        super(parent);
        this.name = name;
        this.readable = readable;
        this.isPrivate = isPrivate;
    }
    
    /*
     * Generates a getter and setter method for a field in (the MemoryLayout of) a C struct.
     * Manipulating these fields directly isn't exactly a best practice, but there are some 
     * valid use cases IMO for at least some field getters.
     * The getters receive the name of the field followed by "$get()" or "$set(value)".
     * 
     * I didn't put too much effort into this, so any special cases (like arrays or callbacks) 
     * are not supported (i.e. will not generate a getter or setter).
     */
    public void generate(Writer writer) throws IOException {
        // Don't try to generate a getter for callback or array fields yet
        if (type == null) {
            return;
        }
        // Don't generate a getter for a "disguised" record (often private data)
        if (type.girElementInstance != null 
                && type.girElementInstance instanceof Record r 
                && "1".equals(r.disguised)) {
            return;
        }
        // Don't generate a getter for a field that is marked as not readable
        if ("0".equals(readable)) {
            return;
        }
        // Don't generate a getter for a private field
        if ("1".equals(isPrivate)) {
            return;
        }
        
        // Generate getter method
        writer.write("    \n");
        writer.write("    /**\n");
        writer.write("     * Get the value of the field {@code " + this.name + "}\n");
        writer.write("     * @return The value of the field {@code " + this.name + "}\n");
        writer.write("     */\n");
        writer.write("    public " + getReturnType() + " " + this.name + "$get() {\n");
        
        if ((! type.isPointer()) && (type.isClass() || type.isInterface())) {
            writer.write("        long OFFSET = getMemoryLayout().byteOffset(MemoryLayout.PathElement.groupElement(\"" + this.name + "\"));\n");
            writer.write("        return ");
            generateReverseInterop(writer, "((MemoryAddress) handle()).addOffset(OFFSET)", false);
            writer.write(";\n");
        } else {
            writer.write("        var RESULT = (" + getMemoryType() + ") getMemoryLayout()\n"
                    + "            .varHandle(MemoryLayout.PathElement.groupElement(\"" + this.name + "\"))\n"
                    + "            .get(MemorySegment.ofAddress((MemoryAddress) handle(), getMemoryLayout().byteSize(), Interop.getScope()));\n");
            writer.write("        return ");
            generateReverseInterop(writer, "RESULT", false);
            writer.write(";\n");
        }
        writer.write("    }\n");
        
        // Don't try to generate a setter for nested struct type definitions
        if ((! type.isPointer()) && (type.isClass() || type.isInterface())) {
            return;
        }
        
        // Don't try to generate a setter for callbacks
        if (type.isCallback()) {
            return;
        }
        
        // Generate setter method
        writer.write("    \n");
        writer.write("    /**\n");
        writer.write("     * Change the value of the field {@code " + this.name + "}\n");
        writer.write("     * @param " + this.name + " The new value of the field {@code " + this.name + "}\n");
        writer.write("     */\n");
        writer.write("    public void " + this.name + "$set(");
        generateTypeAndName(writer, true);
        writer.write(") {\n");
        writer.write("        getMemoryLayout()\n"
                + "            .varHandle(MemoryLayout.PathElement.groupElement(\"" + this.name + "\"))\n"
                + "            .set(MemorySegment.ofAddress((MemoryAddress) handle(), getMemoryLayout().byteSize(), Interop.getScope()), ");
        generateInterop(writer, this.name, false);
        writer.write(");\n");
        writer.write("    }\n");
    }
    
    public void generateStructField(Writer writer) throws IOException {
        writer.write("        \n");

        // Generate javadoc
        if (doc != null) {
            doc.generate(writer, 2);
        }

        writer.write("        public Build set" + Conversions.toCamelCase(this.name, true) + "(");

        // Write the parameter
        generateTypeAndName(writer, false);

        // Set the value in the struct using the generated memory layout
        writer.write(") {\n");
        writer.write("            getMemoryLayout()\n"
                + "                .varHandle(MemoryLayout.PathElement.groupElement(\"" + this.name + "\"))\n"
                + "                .set(MemorySegment.ofAddress((MemoryAddress) struct.handle(), getMemoryLayout().byteSize(), Interop.getScope()), ");
        // Check for null values
        if (checkNull()) {
            writer.write("(Addressable) (" + this.name + " == null ? MemoryAddress.NULL : ");
        }

        // Convert the parameter to the C function argument
        generateInterop(writer, this.name, false);

        if (checkNull()) {
            writer.write(")");
        }

        writer.write(");\n");
        writer.write("        return this;\n");
        writer.write("        }\n");
    }
    
    /**
     * Generates a String containing the MemoryLayout definition for this field.
     * @return A String containing the MemoryLayout definition for this field
     */
    public String getMemoryLayoutString() {
        
        // Regular types (not arrays or callbacks)
        if (type != null) {
            
            // Bitfields and enumerations are integers
            if (type.isBitfield() || type.isEnum()) {
                return "Interop.valueLayout.C_INT.withName(\"" + this.name + "\")";
            }
            
            // Pointers, strings and callbacks are memory addresses
            if (type.isPointer()
                    || "java.lang.String".equals(type.qualifiedJavaType)
                    || "java.lang.foreign.MemoryAddress".equals(type.qualifiedJavaType)
                    || type.isCallback()) {
                return "Interop.valueLayout.ADDRESS.withName(\"" + this.name + "\")";
            }
            
            // Primitive types and aliases
            if (type.isPrimitive 
                    || type.isAliasForPrimitive()) {
                return Conversions.getValueLayout(type) + ".withName(\"" + this.name + "\")";
            }
            
            // For Proxy objects we recursively get the memory layout
            return type.qualifiedJavaType + ".getMemoryLayout()" + ".withName(\"" + this.name + "\")";
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
                
                valueLayout = Conversions.toPanamaMemoryLayout(array.type);
                
            // Proxy objects
            } else {
                valueLayout = array.type.qualifiedJavaType + ".getMemoryLayout()";
            }
            
            return "MemoryLayout.sequenceLayout(" + array.fixedSize + ", " + valueLayout + ").withName(\"" + this.name + "\")";
        }
        
        // Arrays with non-fixed size
        if (array != null) {
            return "Interop.valueLayout.ADDRESS.withName(\"" + this.name + "\")";
        }
        
        // Callbacks
        if (callback != null) {
            return "Interop.valueLayout.ADDRESS.withName(\"" + this.name + "\")";
        }
        
        System.out.printf("Error: Field %s.%s has unknown type\n", parent.name, name);
        return "Interop.valueLayout.ADDRESS.withName(\"" + this.name + "\")";
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
        if (type.isPrimitive && type.isPointer()) {
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
     * Get the byte-size of the native types. This is obviously architecture- and platform-specific.
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
            case "long" -> 64; // On Windows this is 32, on Linux this is 64
            case "float" -> 32;
            case "double" -> 64;
            case "MemoryAddress" -> 64; // 64-bits pointer
            case "ARRAY" -> Integer.parseInt(array.fixedSize) * getSize(getMemoryType(array.type));
            default -> 64;
        };
    }
}
