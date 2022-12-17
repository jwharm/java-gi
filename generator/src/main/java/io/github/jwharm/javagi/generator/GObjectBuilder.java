package io.github.jwharm.javagi.generator;

import java.io.IOException;
import java.io.Writer;

import io.github.jwharm.javagi.model.Property;

/**
 * Helper class to generate a {@code Builder} subclass in GObjects.
 * This way, a new GObject can be instantiated using a builder pattern.
 */
public class GObjectBuilder {

    /**
     * Generate a public static inner class {@code Builder} to implement the <em>builder pattern</em>.
     * @param writer The writer to the source file
     * @param c The outer class
     * @throws IOException Thrown when an error occurs while writing
     */
    public static void generateBuilder(Writer writer, io.github.jwharm.javagi.model.Class c) throws IOException {

        // Each Builder class extends the Builder class of the object's parent, to allow
        // setting the properties of the parent type. GObject does not have a parent, 
        // so GObject.Builder extends from the base Builder class.
        String parent = c.parentClass + ".Builder";
        if (c.parentClass == null) {
            parent = "io.github.jwharm.javagi.Builder";
        }

        // Write the inner Build class definition
        writer.write("    \n"
                + "    /**\n"
                + "     * A {@link " + c.javaName + ".Builder} object constructs a {@link " + c.javaName + "} \n"
                + "     * using the <em>builder pattern</em> to set property values. \n"
                + "     * Use the various {@code set...()} methods to set properties, \n"
                + "     * and finish construction with {@link " + c.javaName + ".Builder#build()}. \n"
                + "     */\n"
                + "    public static Builder builder() {\n"
                + "        return new Builder();\n"
                + "    }\n"
                + "    \n"
                + "    /**\n"
                + "     * Inner class implementing a builder pattern to construct \n"
                + "     * GObjects with properties.\n"
                + "     */\n"
                + "    public static class Builder extends " + parent + " {\n"
                + "        \n"
                + "        protected Builder() {\n"
                + "        }\n"
                + "        \n"
                + "        /**\n"
                + "         * Finish building the {@link " + c.javaName + "} object.\n"
                + "         * Internally, a call to {@link org.gtk.gobject.GObject#typeFromName} \n"
                + "         * is executed to create a new GObject instance, which is then cast to \n"
                + "         * {@link " + c.javaName + "} using {@link " + c.javaName + "#castFrom}.\n"
                + "         * @return A new instance of {@code " + c.javaName + "} with the properties \n"
                + "         *         that were set in the Builder object.\n"
                + "         */\n"
                + "        public " + c.javaName + " build() {\n"
                + "            return " + c.javaName + ".castFrom(\n"
                + "                org.gtk.gobject.GiObject.newWithProperties(\n"
                + "                    " + c.javaName + ".getType(),\n"
                + "                    names.size(),\n"
                + "                    names.toArray(new String[0]),\n"
                + "                    values.toArray(new org.gtk.gobject.Value[0])\n"
                + "                )\n"
                + "            );\n"
                + "        }\n");
        
        // Generate setters for the properties
        for (Property p : c.propertyList) {
            // TODO: Don't know how to set array properties with gvalues
            if (p.array == null) {
                p.generate(writer);
            }
        }
        
        writer.write("    }\n");
    }
}
