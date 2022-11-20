package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

import io.github.jwharm.javagi.generator.GValueCreator;

public class Record extends Class {

    public final String disguised, isGTypeStructFor;

    public Record(GirElement parent, String name, String cType, String version, String disguised, String isGTypeStructFor) {
        super(parent, name, null, cType, null, null, null, version);
        this.disguised = disguised;
        this.isGTypeStructFor = isGTypeStructFor;
    }
    
    /**
     * A record in GI is a struct in C. Java doesn't have a struct type, and the java 'record'
     * functionality has a different purpose. So the generated API creates a class that
     * extends Struct (instead of GObject).
     * Structs are often initialized implicitly, which means they don't always have constructors.
     * To solve this, we generate a static allocate() function that allocates a memory segment.
     */
    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);
        
        writer.write("public class " + javaName + " extends Struct {\n");

        generateEnsureInitialized(writer);
        generateCType(writer);
        generateMemoryLayout(writer);
        generateRecordAllocator(writer);
        for (Field f : fieldList) {
            f.generate(writer);
        }

        generateMemoryAddressConstructor(writer);
        generateConstructors(writer);

        for (Method m : methodList) {
            m.generate(writer, false, false);
        }

        for (Function function : functionList) {
            function.generate(writer, false, true);
        }

        for (Signal s : signalList) {
            s.generate(writer, false);
        }

        generateDowncallHandles(writer);
        generateSignalCallbacks(writer);
        
        // Write convenience constructors to allocate, initialize and set GValues
        if (getNamespace().packageName.equals("org.gtk.gobject") && this.javaName.equals("Value")) {
            GValueCreator.generateGValueConstructors(writer);
        }
        
        writer.write("}\n");
    }

    public void generateRecordAllocator(Writer writer) throws IOException {
        writer.write("    \n");
        writer.write("    private MemorySegment allocatedMemorySegment;\n");
        writer.write("    \n");
        writer.write("    /**\n");
        writer.write("     * Allocate a new {@link " + javaName + "}\n");
        writer.write("     * @return A new, uninitialized @{link " + javaName + "}\n");
        writer.write("     */\n");
        writer.write("    public static " + javaName + " allocate() {\n");
        writer.write("        MemorySegment segment = Interop.getAllocator().allocate(getMemoryLayout());\n");
        writer.write("        " + javaName + " newInstance = new " + javaName + "(segment.address(), Ownership.NONE);\n");
        writer.write("        newInstance.allocatedMemorySegment = segment;\n");
        writer.write("        return newInstance;\n");
        writer.write("    }\n");
    }
}
