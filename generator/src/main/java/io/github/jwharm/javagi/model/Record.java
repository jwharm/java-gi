package io.github.jwharm.javagi.model;

import java.io.IOException;

import io.github.jwharm.javagi.generator.SourceWriter;

public class Record extends Class {

    public final String disguised, isGTypeStructFor;

    public Record(GirElement parent, String name, String cType, String version, String disguised, String isGTypeStructFor) {
        super(parent, name, null, cType, null, null, null, version, null, null);
        this.disguised = disguised;
        this.isGTypeStructFor = isGTypeStructFor;
    }
    
    /**
     * A record in GI is a struct in C. Java doesn't have a struct type, and the java 'record'
     * functionality has a different purpose. So the generated API creates a class that
     * extends StructProxy (instead of GObject).
     * Structs are often initialized implicitly, which means they don't always have constructors.
     * To solve this, we generate a static allocate() function that allocates a memory segment.
     */
    public void generate(SourceWriter writer) throws IOException {
        if (isGTypeStructFor != null) {
            writer.write("\n");
        } else {
            generatePackageDeclaration(writer);
            generateImportStatements(writer);
        }
        
        generateJavadoc(writer);
        writer.write("public ");
        
        if (isGTypeStructFor != null) {
            writer.write("static ");
        }
        
        writer.write("class " + javaName);
        
        if (generic) {
            writer.write("<T extends org.gnome.gobject.GObject>");
        }

        if (isGTypeStructFor != null) {
            writer.write(" extends org.gnome.gobject.TypeClass {\n");
        } else {
            writer.write(" extends StructProxy {\n");
        }

        writer.increaseIndent();

        if (isGTypeStructFor == null) {
            generateEnsureInitialized(writer);
        }
        
        generateCType(writer);

        // Opaque structs have unknown memory layout and should not have an allocator
        if (! (isOpaqueStruct() || hasOpaqueStructFields())) {
            generateMemoryLayout(writer);
            generateRecordAllocator(writer);
            for (Field f : fieldList) {
                f.generate(writer);
            }
            // Fields can be inside a <union> tag
            if (! unionList.isEmpty()) {
                for (Field f : unionList.get(0).fieldList) {
                    f.generate(writer);
                }
            }
        }

        generateMemoryAddressConstructor(writer);
        generateMarshal(writer);
        generateConstructors(writer);
        generateMethodsAndSignals(writer);

        generateDowncallHandles(writer);
        generateInjected(writer);

        writer.decreaseIndent();
        writer.write("}\n");
    }

    public void generateRecordAllocator(SourceWriter writer) throws IOException {
        writer.write("\n");
        writer.write("private MemorySegment allocatedMemorySegment;\n");
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Allocate a new {@link " + javaName + "}. A {@link java.lang.ref.Cleaner} \n");
        writer.write(" * is assigned to the allocated memory segment that will release the \n");
        writer.write(" * memory when the {@link " + javaName + "} instance is garbage-collected.\n");
        writer.write(" * @return A new, uninitialized {@link " + javaName + "}\n");
        writer.write(" */\n");
        writer.write("public static " + javaName + " allocate() {\n");
        writer.write("    MemorySegment segment = MemorySession.openImplicit().allocate(getMemoryLayout());\n");
        writer.write("    " + javaName + " newInstance = new " + javaName + "(segment.address());\n");
        writer.write("    newInstance.allocatedMemorySegment = segment;\n");
        writer.write("    return newInstance;\n");
        writer.write("}\n");
    }
}
