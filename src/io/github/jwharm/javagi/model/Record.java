package io.github.jwharm.javagi.model;

import jdk.incubator.foreign.SegmentAllocator;

import java.io.IOException;
import java.io.Writer;

public class Record extends Class {

    public final String cType, isGTypeStructFor;

    public Record(GirElement parent, String name, String cType, String isGTypeStructFor) {
        super(parent, name, null);
        this.cType = cType;
        this.isGTypeStructFor = isGTypeStructFor;
    }

    /**
     * A record in GI is a struct in C. Java doesn't have a struct type, and the java 'record'
     * functionality has a different purpose. So the generated API just creates a class that
     * extends ResourceProxy (instead of GObject).
     * Structs are often initialized implicitly, which means they don't always have constructors.
     * In those cases, we generate a default constructor that just allocates a memory segment.
     * The API from jextract is used for this; it offers an allocate() method for structs.
     * Sometimes, the struct layout is not exposed in the C header file, and jextract is unable
     * to generate an allocate() method in that case. For those types, we expect the GI API to
     * offer a constructor.
     */
    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public class " + javaName + " extends io.github.jwharm.javagi.interop.ResourceBase {\n");
        writer.write("\n");

        generateMemoryAddressConstructor(writer);

        if (constructorList.isEmpty()) {
            generateRecordConstructor(writer);
        } else {
            generateConstructors(writer);
        }

        for (Method m : methodList) {
            if (m.isSafeToBind()) {
                m.generate(writer, false, false);
            }
        }

        for (Signal s : signalList) {
            if (s.isSafeToBind()) {
                s.generate(writer, false);
            }
        }

        writer.write("}\n");
    }

    public void generateRecordConstructor(Writer writer) throws IOException {

        // Use reflection to check whether the struct was declared in the header file.
        // In that case, jextract generated an "allocate" method.
        try {
            java.lang.Class.forName("io.github.jwharm.javagi.interop.jextract." + cType,
                            false,
                            this.getClass().getClassLoader())
                    .getMethod("allocate", SegmentAllocator.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return;
        }
        writer.write("    public " + javaName + "() {\n");
        writer.write("        super(ProxyFactory.getProxy(io.github.jwharm.javagi.interop.jextract." + cType + ".allocate(Interop.getAllocator()).address()));\n");
        writer.write("    }\n");
        writer.write("    \n");
    }
}
