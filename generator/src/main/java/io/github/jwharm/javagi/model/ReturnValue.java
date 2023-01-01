package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

import io.github.jwharm.javagi.generator.Conversions;

public class ReturnValue extends Parameter {

    public ReturnValue(GirElement parent, String transferOwnership, String nullable) {
        super(parent, null, transferOwnership, nullable, null, null, null);
    }

    /**
     * Generate code to process and return the function call result.
     * @param writer The source code file writer
     * @param panamaReturnType The type of the value that was returned by the downcall
     * @param indent How many tabs to indent
     * @throws IOException Thrown when an error occurs while writing
     */
    public void generate(Writer writer, String panamaReturnType, int indent) throws IOException {
        // If the return value is an array, try to convert it to a Java array
        if (array != null) {
            String len = array.size(false);
            if (len != null) {
                if (nullable) {
                    switch (panamaReturnType) {
                        case "MemoryAddress" -> writer.write(tab(indent) + "if (RESULT.equals(MemoryAddress.NULL)) return null;\n");
                        case "MemorySegment" -> writer.write(tab(indent) + "if (RESULT.address().equals(MemoryAddress.NULL)) return null;\n");
                        default -> System.err.println("Unexpected nullable return type: " + panamaReturnType);
                    }
                }
                String valuelayout = Conversions.getValueLayout(array.type);
                if (array.type.isPrimitive && (!array.type.isBoolean())) {
                    // Array of primitive values
                    writer.write(tab(indent) + "return MemorySegment.ofAddress(RESULT.get(Interop.valueLayout.ADDRESS, 0), " + len + " * " + valuelayout + ".byteSize(), Interop.getScope()).toArray(" + valuelayout + ");\n");
                } else {
                    // Array of proxy objects
                    writer.write(tab(indent) + array.type.qualifiedJavaType + "[] resultARRAY = new " + array.type.qualifiedJavaType + "[" + len + "];\n");
                    writer.write(tab(indent) + "for (int I = 0; I < " + len + "; I++) {\n");
                    writer.write(tab(indent) + "    var OBJ = RESULT.get(" + valuelayout + ", I);\n");
                    writer.write(tab(indent) + "    resultARRAY[I] = " + marshalNativeToJava(array.type, "OBJ", false) + ";\n");
                    writer.write(tab(indent) + "}\n");
                    writer.write(tab(indent) + "return resultARRAY;\n");
                }
            } else {
                generateReturnStatement(writer, indent);
            }
        } else {
            generateReturnStatement(writer, indent);
        }
    }
    
    /**
     * Generate the return statement for a function or method call.
     * @param writer The source code file writer
     * @param indent How many tabs to indent
     * @throws IOException Thrown when an error occurs while writing
     */
    public void generateReturnStatement(Writer writer, int indent) throws IOException {
        if (type != null && type.isVoid()) {
            return;
        }
        writer.write(tab(indent) + "return ");
        marshalNativeToJava(writer, "RESULT", false);
        writer.write(";\n");
    }
}
