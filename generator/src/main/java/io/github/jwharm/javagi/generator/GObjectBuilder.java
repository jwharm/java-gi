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
    public static void generateBuilder(SourceWriter writer, io.github.jwharm.javagi.model.Class c) throws IOException {

        // Each Builder class extends the Builder class of the object's parent, to allow
        // setting the properties of the parent type. GObject does not have a parent, 
        // so GObject.Builder extends from the base Builder class.
        String parent = c.parentClass + ".Builder";
        if (c.parentClass == null) {
            parent = "io.github.jwharm.javagi.Builder";
        }

        // Write the inner Build class definition
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * A {@link " + c.javaName + ".Builder} object constructs a {@link " + c.javaName + "} \n");
        writer.write(" * using the <em>builder pattern</em> to set property values. \n");
        writer.write(" * Use the various {@code set...()} methods to set properties, \n");
        writer.write(" * and finish construction with {@link " + c.javaName + ".Builder#build()}. \n");
        writer.write(" */\n");
        writer.write("public static Builder builder() {\n");
        writer.write("    return new Builder();\n");
        writer.write("}\n");
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Inner class implementing a builder pattern to construct \n");
        writer.write(" * a GObject with properties.\n");
        writer.write(" */\n");
        writer.write("public static class Builder extends " + parent + " {\n");
        writer.increaseIndent();

        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Default constructor for a {@code Builder} object.\n");
        writer.write(" */\n");
        writer.write("protected Builder() {\n");
        writer.write("}\n");
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Finish building the {@link " + c.javaName + "} object.\n");
        writer.write(" * Internally, a call to {@link org.gtk.gobject.GObjects#typeFromName} \n");
        writer.write(" * is executed to create a new GObject instance, which is then cast to \n");
        writer.write(" * {@link " + c.javaName + "}.\n");
        writer.write(" * @return A new instance of {@code " + c.javaName + "} with the properties \n");
        writer.write(" *         that were set in the Builder object.\n");
        writer.write(" */\n");
        writer.write("public " + c.javaName + " build() {\n");
        writer.write("    return (" + c.javaName + ") org.gtk.gobject.GObject.newWithProperties(\n");
        writer.write("        " + c.javaName + ".getType(),\n");
        writer.write("        names.size(),\n");
        writer.write("        names.toArray(new String[names.size()]),\n");
        writer.write("        values.toArray(new org.gtk.gobject.Value[names.size()])\n");
        writer.write("    );\n");
        writer.write("}\n");
        
        // Generate setters for the properties
        for (Property p : c.propertyList) {
            // TODO: Don't know how to set array properties with gvalues
            if (p.array == null) {
                p.generate(writer);
            }
        }

        writer.decreaseIndent();
        writer.write("}\n");
    }
}
