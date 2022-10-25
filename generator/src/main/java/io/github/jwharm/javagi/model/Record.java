package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

public class Record extends Class {

    public final String disguised, isGTypeStructFor;

    public Record(GirElement parent, String name, String cType, String disguised, String isGTypeStructFor) {
        super(parent, name, null, cType);
        this.disguised = disguised;
        this.isGTypeStructFor = isGTypeStructFor;
    }
    
    /**
     * Don't generate empty classes for GType struct declarations.
     * @return Whether this record has a "is-gtype-struct-for" attribute and 
     * does not contain any constructors, methods, functions or signals.
     */
    public boolean isEmpty() {
        return (isGTypeStructFor != null)
                && constructorList.isEmpty() 
                && methodList.isEmpty() 
                && functionList.isEmpty() 
                && signalList.isEmpty();
    }

    /**
     * A record in GI is a struct in C. Java doesn't have a struct type, and the java 'record'
     * functionality has a different purpose. So the generated API just creates a class that
     * extends ResourceBase (instead of GObject).
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

        writer.write("public class " + javaName + " extends io.github.jwharm.javagi.ResourceBase {\n");

        generateEnsureInitialized(writer);
        generateMemoryAddressConstructor(writer);

        if (constructorList.isEmpty()) {
            //generateRecordConstructor(writer); // Currently broken, this was based on jextract memorylayout declarations
        } else {
            generateConstructors(writer);
        }

        for (Method m : methodList) {
            m.generate(writer, false, false);
        }

        for (Function function : functionList) {
            function.generate(writer, false, true);
        }

        for (Signal s : signalList) {
            s.generate(writer, false);
        }

        if (! (constructorList.isEmpty() && methodList.isEmpty() && functionList.isEmpty())) {
        	writer.write("    \n");
            writer.write("    private static class DowncallHandles {\n");
            for (Constructor c : constructorList) {
                c.generateMethodHandle(writer, false);
            }
            for (Method m : methodList) {
                m.generateMethodHandle(writer, false);
            }
            for (Function f : functionList) {
                f.generateMethodHandle(writer, false);
            }
            writer.write("    }\n");
        }
        
        if (! signalList.isEmpty()) {
        	writer.write("    \n");
        	writer.write("    @ApiStatus.Internal\n");
            writer.write("    public static class Callbacks {\n");
            for (Signal s : signalList) {
                s.generateStaticCallback(writer, false);
            }
            writer.write("    }\n");
        }
        writer.write("}\n");
    }

//    public void generateRecordConstructor(Writer writer) throws IOException {
//
//        // Use reflection to check whether the struct was declared in the header file.
//        // In that case, jextract generated an "allocate" method.
//        try {
//            java.lang.Class.forName("io.github.jwharm.javagi.interop.jextract." + cType,
//                            false,
//                            this.getClass().getClassLoader())
//                    .getMethod("allocate", SegmentAllocator.class);
//        } catch (ClassNotFoundException | NoSuchMethodException e) {
//            return;
//        }
//        writer.write("    public " + javaName + "() {\n");
//        writer.write("        super(Refcounted.get(io.github.jwharm.javagi.interop.jextract." + cType + ".allocate(Interop.getAllocator()).address()));\n");
//        writer.write("    }\n");
//        writer.write("    \n");
//    }
}
