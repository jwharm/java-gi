package io.github.jwharm.javagi.generator;

import java.io.IOException;
import java.io.Writer;

import io.github.jwharm.javagi.model.Field;

/**
 * Helper class to generate a {@code Build} subclass in Structs.
 * This way, a new struct can be instantiated using a builder pattern to set values of the fields.
 */
public class StructBuilder {

    /**
     * Generate a public static inner class {@code Build} to implement the <em>builder pattern</em>.
     * @param writer The writer to the source file
     * @param r The outer class
     * @throws IOException Thrown when an error occurs while writing
     */
    public static void generateBuilder(Writer writer, io.github.jwharm.javagi.model.Record r) throws IOException {
        
        // Write the inner Build class definition
        writer.write("\n" 
                + "    /**\n"
                + "     * Inner class implementing a builder pattern to construct \n"
                + "     * a struct and set its values.\n"
                + "     */\n"
                + "    public static class Build {\n" 
                + "        \n"
                + "        private " + r.javaName + " struct;\n"
                + "        \n"
                + "         /**\n"
                + "         * A {@link " + r.javaName + ".Build} object constructs a {@link " + r.javaName + "} \n"
                + "         * struct using the <em>builder pattern</em> to set the field values. \n"
                + "         * Use the various {@code set...()} methods to set field values, \n"
                + "         * and finish construction with {@link #construct()}. \n"
                + "         */\n"
                + "        public Build() {\n" 
                + "            struct = " + r.javaName + ".allocate();\n"
                + "        }\n"
                + "        \n"
                + "         /**\n"
                + "         * Finish building the {@link " + r.javaName + "} struct.\n"
                + "         * @return A new instance of {@code " + r.javaName + "} with the fields \n"
                + "         *         that were set in the Build object.\n"
                + "         */\n"
                + "        public " + r.javaName + " construct() {\n"
                + "            return struct;\n"
                + "        }\n");
        
        // Generate setters for the fields
        for (Field f : r.fieldList) {
            f.generateStructField(writer);
        }
        
        writer.write("    }\n");
    }
}
