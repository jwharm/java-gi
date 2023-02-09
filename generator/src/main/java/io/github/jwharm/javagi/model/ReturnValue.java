package io.github.jwharm.javagi.model;

import java.io.IOException;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

public class ReturnValue extends Parameter {

    public boolean returnsFloatingReference;
    public String overrideReturnValue;

    public ReturnValue(GirElement parent, String transferOwnership, String nullable) {
        super(parent, null, transferOwnership, nullable, null, null, null, null);

        returnsFloatingReference = false;
        overrideReturnValue = null;
    }

    /**
     * Generate code to process and return the function call result.
     * @param writer The source code file writer
     * @param panamaReturnType The type of the value that was returned by the downcall
     * @throws IOException Thrown when an error occurs while writing
     */
    public void generate(SourceWriter writer, String panamaReturnType) throws IOException {
        // If the return value is an array, try to convert it to a Java array
        if (array != null) {
            String len = array.size(false);
            if (len != null) {
                if (nullable) {
                    switch (panamaReturnType) {
                        case "MemoryAddress" -> writer.write("if (RESULT.equals(MemoryAddress.NULL)) return null;\n");
                        case "MemorySegment" -> writer.write("if (RESULT.address().equals(MemoryAddress.NULL)) return null;\n");
                        default -> System.err.println("Unexpected nullable return type: " + panamaReturnType);
                    }
                }
                String valuelayout = Conversions.getValueLayout(array.type);
                if (array.type.isPrimitive && (!array.type.isBoolean())) {
                    // Array of primitive values
                    writer.write("return MemorySegment.ofAddress(RESULT.get(Interop.valueLayout.ADDRESS, 0), " + len + " * " + valuelayout + ".byteSize(), SCOPE).toArray(" + valuelayout + ");\n");
                } else {
                    // Array of proxy objects
                    writer.write(array.type.qualifiedJavaType + "[] resultARRAY = new " + array.type.qualifiedJavaType + "[" + len + "];\n");
                    writer.write("for (int I = 0; I < " + len + "; I++) {\n");
                    writer.write("    var OBJ = RESULT.get(" + valuelayout + ", I);\n");
                    writer.write("    resultARRAY[I] = " + marshalNativeToJava(array.type, "OBJ", false) + ";\n");
                    writer.write("}\n");
                    writer.write("return resultARRAY;\n");
                }
            } else {
                generateReturnStatement(writer);
            }
        } else {
            generateReturnStatement(writer);
        }
    }
    
    /**
     * Generate the return statement for a function or method call.
     * @param writer The source code file writer
     * @throws IOException Thrown when an error occurs while writing
     */
    public void generateReturnStatement(SourceWriter writer) throws IOException {
        if (type != null && type.isVoid()) {
            return;
        }
        
        // Return value hard-coded in PatchSet?
        if (overrideReturnValue != null) {
            writer.write("return " + overrideReturnValue + ";\n");
            return;
        }

        // When transfer-ownership="full", we must take ownership.
        if (isProxy() && "full".equals(transferOwnership) || returnsFloatingReference) {

            writer.write("var OBJECT = ");
            marshalNativeToJava(writer, "RESULT", false);
            writer.write(";\n");
            writer.write("if (OBJECT != null) {\n");

            // Sink floating reference
            if (returnsFloatingReference)
                writer.write("    OBJECT.refSink();\n");

            writer.write("    OBJECT.takeOwnership();\n");
            writer.write("}\n");
            writer.write("return OBJECT;\n");

        } else {

            writer.write("return ");
            marshalNativeToJava(writer, "RESULT", false);
            writer.write(";\n");
        }
    }
}
