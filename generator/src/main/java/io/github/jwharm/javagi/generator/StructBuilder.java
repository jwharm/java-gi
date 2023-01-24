package io.github.jwharm.javagi.generator;

import java.io.IOException;
import java.io.Writer;

import io.github.jwharm.javagi.model.Field;

/**
 * Helper class to generate a {@code Builder} subclass in Structs.
 * This way, a new struct can be instantiated using a builder pattern to set values of the fields.
 */
public class StructBuilder {

    /**
     * Generate a public static inner class {@code Builder} to implement the <em>builder pattern</em>.
     * @param writer The writer to the source file
     * @param r The outer class
     * @throws IOException Thrown when an error occurs while writing
     */
    public static void generateBuilder(SourceWriter writer, io.github.jwharm.javagi.model.Record r) throws IOException {

        // No builder for opaque structs (without field definitions) or structs with fields that refer to opaque structs)
        if (r.isOpaqueStruct() || r.hasOpaqueStructFields()) {
            return;
        }

        // Write the inner Build class definition
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * A {@link " + r.javaName + ".Builder} object constructs a {@link " + r.javaName + "} \n");
        writer.write(" * struct using the <em>builder pattern</em> to set the field values. \n");
        writer.write(" * Use the various {@code set...()} methods to set field values, \n");
        writer.write(" * and finish construction with {@link " + r.javaName + ".Builder#build()}. \n");
        writer.write(" */\n");
        writer.write("public static Builder builder() {\n");
        writer.write("    return new Builder();\n");
        writer.write("}\n");
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Inner class implementing a builder pattern to construct \n");
        writer.write(" * a struct and set its values.\n");
        writer.write(" */\n");
        writer.write("public static class Builder {\n");
        writer.increaseIndent();

        writer.write("\n");
        writer.write("private final " + r.javaName + " struct;\n");
        writer.write("\n");
        writer.write("private Builder() {\n");
        writer.write("    struct = " + r.javaName + ".allocate();\n");
        writer.write("}\n");
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Finish building the {@link " + r.javaName + "} struct.\n");
        writer.write(" * @return A new instance of {@code " + r.javaName + "} with the fields \n");
        writer.write(" *         that were set in the Builder object.\n");
        writer.write(" */\n");
        writer.write("public " + r.javaName + " build() {\n");
        writer.write("    return struct;\n");
        writer.write("}\n");
        
        // Generate setters for the fields
        for (Field f : r.fieldList) {
            f.generateStructField(writer);
        }

        writer.decreaseIndent();
        writer.write("}\n");
    }
}
