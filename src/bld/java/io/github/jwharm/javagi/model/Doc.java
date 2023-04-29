package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Javadoc;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;

public class Doc extends GirElement {
    public final String space;
    public String contents;

    public Doc(GirElement parent, String space) {
        super(parent);
        this.space = space;
    }

    public void generate(SourceWriter writer, boolean signalDeclaration) throws IOException {
        if (contents == null || contents.length() == 0) {
            return;
        }
        writer.write("/**\n");
        
        // Convert docstring to javadoc
        String javadoc = Javadoc.getInstance().convert(this);
        
        // Write docstring
        writeDoc(writer, javadoc, null);
        
        // Version
        if (parent instanceof RegisteredType rt) {
            if (rt.version != null) {
                writeDoc(writer, rt.version, "@version");
            }
        }
        
        // Methods and functions
        if (parent instanceof CallableType ct
                && (! (parent instanceof Callback || parent instanceof Signal))) {
            
            // Param
            Parameters parameters = ct.getParameters();
            if (parameters != null) {
                for (Parameter p : parameters.parameterList) {
                    if (p.isInstanceParameter() || p.isUserDataParameter()
                            || p.isDestroyNotifyParameter() || p.isArrayLengthParameter()) {
                        continue;
                    }
                    if (p.doc != null) {
                        String pJavadoc = Javadoc.getInstance().convert(p.doc);
                        writeDoc(writer, pJavadoc, "@param " + (p.varargs ? "varargs" : p.name));
                    }
                    writePortabilityWarning(writer, p);
                }
            }
            
            // Return (except for constructors)
            if (! (parent instanceof Constructor c && c.name.equals("new"))) {
                ReturnValue rv = ct.getReturnValue();
                if (rv != null && rv.doc != null) {
                    String rvJavadoc = Javadoc.getInstance().convert(rv.doc);
                    writeDoc(writer, rvJavadoc, "@return");
                }
                writePortabilityWarning(writer, rv);
            }
            
            // Exception
            if (parent instanceof Method m) {
                if ("1".equals(m.throws_)) {
                    writeDoc(writer, "GErrorException see {@link org.gnome.glib.GError}", "@throws");
                }
            }
        }
        
        // Signals
        if (signalDeclaration && parent instanceof Signal signal) {
            if (signal.detailed) {
                writeDoc(writer, "the signal detail", "@param detail");
            }
            writeDoc(writer, "the signal handler", "@param handler");
            writeDoc(writer, "a {@link io.github.jwharm.javagi.base.Signal} object to keep track of the signal connection", "@return");
        }
        
        // Deprecated
        if (parent instanceof Method m && "1".equals(m.deprecated)) {
            if (parent.docDeprecated != null) {
                String deprecatedJavadoc = Javadoc.getInstance().convert(parent.docDeprecated);
                writeDoc(writer, deprecatedJavadoc, "@deprecated");
            }
        }
        
        // Property setters
        if (parent instanceof Property p) {
            writeDoc(writer, p.name + " the value for the {@code " + p.propertyName + "} property", "@param");
            writeDoc(writer, "the {@code Build} instance is returned, to allow method chaining", "@return");
        }
        
        // Field setters
        if (parent instanceof Field f) {
            writeDoc(writer, f.name + " the value for the {@code " + f.name + "} field", "@param");
            writeDoc(writer, "the {@code Build} instance is returned, to allow method chaining", "@return");
        }
        
        writer.write(" */\n");
    }
    
    // When applicable, write a warning about long vs int values on Windows
    private void writePortabilityWarning(SourceWriter writer, Parameter p) throws IOException {
        if (p.type != null && ("glong".equals(p.type.cType) || "gulong".equals(p.type.cType))) {
            if (p.doc != null) {
                writer.write(" * . \n");
            } else if (p instanceof ReturnValue) {
                writer.write("@return ");
            } else {
                writer.write("@param " + p.name + " ");
            }
            writer.write(" * In Windows native code, this is a 32-bit value, while on Linux and MacOS, \n");
            writer.write(" * the value is 64 bits. Therefore, the Java parameter type is {@code int} on Windows, \n");
            writer.write(" * but a {@code long} on Linux and MacOS.\n");
        }
    }
    
    // Write documentation, with an optional tag, starting each line with a " * ", and escape backslashes
    private void writeDoc(SourceWriter writer, String javadoc, String tag) throws IOException {
        
        // Write the lines (starting each line with " * ")
        int count = 0;
        for (String line : javadoc.trim().lines().toList()) {
            String escapedLine = line.replace("\\", "\\\\");
            
            // Write tag (optional)
            if (count == 0 && tag != null) {
                escapedLine = tag + " " + escapedLine;
            }
            writer.write(" * " + escapedLine + "\n");
            count++;
        }
    }
}
