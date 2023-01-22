package io.github.jwharm.javagi.generator;

import java.io.IOException;

import io.github.jwharm.javagi.model.Implements;
import io.github.jwharm.javagi.model.Interface;
import io.github.jwharm.javagi.model.Property;

/**
 * Helper class to generate a {@code Builder} subclass in GObjects.
 * This way, a new GObject can be instantiated using a builder pattern.
 */
public class GObjectBuilder {

    /**
     * Generate a public static inner class {@code Builder} to implement the <em>builder pattern</em>.
     * @param writer the writer to the source file
     * @param c the outer class
     * @throws IOException thrown when an error occurs while writing
     */
    public static void generateBuilder(SourceWriter writer, io.github.jwharm.javagi.model.Class c) throws IOException {

        // Each Builder class extends the Builder class of the object's parent, to allow
        // setting the properties of the parent type. GObject does not have a parent, 
        // so GObject.Builder extends from the base Builder class.
        String parent = c.parentClass + ".Builder<S>";
        if (c.parentClass == null) {
            parent = "io.github.jwharm.javagi.Builder<S>";
        }

        // Write the inner Build class definition
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * A {@link " + c.javaName + ".Builder} object constructs a {@link " + c.javaName + "} \n");
        writer.write(" * using the <em>builder pattern</em> to set property values. \n");
        writer.write(" * Use the various {@code set...()} methods to set properties, \n");
        writer.write(" * and finish construction with {@link " + c.javaName + ".Builder#build()}. \n");
        writer.write(" */\n");
        writer.write("public static Builder<? extends Builder> builder() {\n");
        writer.write("    return new Builder<>();\n");
        writer.write("}\n");
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Inner class implementing a builder pattern to construct \n");
        writer.write(" * a GObject with properties.\n");
        writer.write(" */\n");
        writer.write("public static class Builder<S extends Builder<S>> extends " + parent);

        // Interfaces can have builders too
        boolean first = true;
        for (Implements implem : c.implementsList) {

            // Skip interfaces without properties
            if (implem.girElementInstance.propertyList.isEmpty())
                continue;

            if (first) {
                writer.write("\n");
                writer.write("        implements " + implem.getQualifiedJavaName() + ".Builder<S>");
            } else {
                writer.write(", " + implem.getQualifiedJavaName() + ".Builder<S>");
            }
            first = false;
        }

        writer.increaseIndent();
        writer.write(" {\n");
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Default constructor for a {@code Builder} object.\n");
        writer.write(" */\n");
        writer.write("protected Builder() {\n");
        writer.write("}\n");
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Finish building the {@link " + c.javaName + "} object. This will call \n");
        writer.write(" * {@link org.gtk.gobject.GObject#newWithProperties} to create a new \n");
        writer.write(" * GObject instance, which is then cast to {@link " + c.javaName + "}.\n");
        writer.write(" * @return A new instance of {@code " + c.javaName + "} with the properties \n");
        writer.write(" *         that were set in the Builder object.\n");
        writer.write(" */\n");
        writer.write("public " + c.javaName + " build() {\n");
        writer.write("    return (" + c.javaName + ") org.gtk.gobject.GObject.newWithProperties(\n");
        writer.write("            " + c.javaName + ".getType(), getSize(), getNames(), getValues()\n");
        writer.write("    );\n");
        writer.write("}\n");
        
        // Generate setters for the properties
        for (Property p : c.propertyList) {
            p.generate(writer);
        }

        writer.decreaseIndent();
        writer.write("}\n");
    }

    /**
     * Generate a nested interface that extends {@code PropertyBuilder} and contains setters for
     * the properties that the provided interface defines.
     * @param writer the writer to the source file
     * @param i the interface
     * @throws IOException thrown when an error occurs while writing
     */
    public static void generateInterfaceBuilder(SourceWriter writer, Interface i) throws IOException {

        // Don't generate empty Builders in interfaces
        if (i.propertyList.isEmpty())
            return;

        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Nested interface implemented by Builder classes to construct \n");
        writer.write(" * a GObject with properties of this interface.\n");
        writer.write(" */\n");
        writer.write("interface Builder<S extends io.github.jwharm.javagi.Builder<S>> extends io.github.jwharm.javagi.PropertyBuilder {\n");
        writer.increaseIndent();

        // Generate setters for the properties
        for (Property p : i.propertyList) {
            p.generate(writer);
        }

        writer.decreaseIndent();
        writer.write("}\n");
    }
}
