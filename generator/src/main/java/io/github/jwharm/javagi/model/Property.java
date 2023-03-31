package io.github.jwharm.javagi.model;

import java.io.IOException;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

public class Property extends Variable {

    public final String propertyName, transferOwnership, getter;

    public Property(GirElement parent, String name, String transferOwnership, String getter) {
        super(parent);
        this.propertyName = name;
        this.name = Conversions.toLowerCaseJavaName(name);
        this.transferOwnership = transferOwnership;
        this.getter = getter;
    }
    
    /**
     * Generate a setter method for use in a GObjectBuilder
     * @param writer The writer to the class file
     * @throws IOException Thrown when an exception occurs during writing
     */
    public void generate(SourceWriter writer) throws IOException {
        String gTypeDeclaration = getGTypeDeclaration();
        writer.write("\n");
        if (doc != null) {
            doc.generate(writer, false);
        }
        writer.write((parent instanceof Interface) ? "default " : "public ");
        writer.write("S set" + Conversions.toCamelCase(name, true) + "(");
        writeTypeAndName(writer, false);
        writer.write(") {\n");
        
        if (isApi()) {
            writer.write("    throw Interop.apiError();");
            writer.write("}\n");
            return;
        }
        
        writer.increaseIndent();
        writer.write("org.gnome.gobject.Value _value = org.gnome.gobject.Value.allocate();\n");
        writer.write("_value.init(" + gTypeDeclaration + ");\n");
        if (array != null) {
            writer.write("MemorySession _scope = MemorySession.openImplicit();\n");
        }
        writer.write(getValueSetter("_value", gTypeDeclaration, name) + ";\n");
        writer.write("addBuilderProperty(\"" + propertyName + "\", _value);\n");
        writer.write("return (S) this;\n");
        writer.decreaseIndent();
        writer.write("}\n");
    }
}
