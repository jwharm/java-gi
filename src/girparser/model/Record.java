package girparser.model;

import jdk.incubator.foreign.SegmentAllocator;

import java.io.IOException;
import java.io.Writer;

public class Record extends Class {

    public String cType, isGTypeStructFor;

    public Record(GirElement parent, String name, String cType, String isGTypeStructFor) {
        super(parent, name, null);
        this.cType = cType;
        this.isGTypeStructFor = isGTypeStructFor;
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer, 0);

        writer.write("public class " + javaName + " extends org.gtk.interop.ResourceProxy {\n");
        writer.write("\n");

        generateMemoryAddressConstructor(writer);

        if (constructorList.isEmpty()) {
            generateRecordConstructor(writer);
        } else {
            generateConstructors(writer);
        }

        for (Method m : methodList) {
            if (m.isSafeToBind()) {
                m.generate(writer);
            }
        }

        for (Signal s : signalList) {
            if (s.isSafeToBind()) {
                s.generate(writer);
            }
        }

        writer.write("}\n");
    }

    public void generateRecordConstructor(Writer writer) throws IOException {

        // Use reflection to check whether the struct was declared in the header file.
        // In that case, jextract generated an "allocate" method.
        try {
            java.lang.Class.forName("org.gtk.interop.jextract." + cType,
                            false,
                            this.getClass().getClassLoader())
                    .getMethod("allocate", SegmentAllocator.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return;
        }
        writer.write("    public " + javaName + "() {\n");
        writer.write("        super(org.gtk.interop.jextract." + cType + ".allocate(Interop.getAllocator()).address());\n");
        writer.write("    }\n");
        writer.write("    \n");
    }
}
