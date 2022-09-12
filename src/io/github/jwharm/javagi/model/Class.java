package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

import java.io.IOException;
import java.io.Writer;

public class Class extends RegisteredType {

    public Class(GirElement parent, String name, String parentClass) {
        super(parent, name, parentClass);
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public class " + javaName);
        writer.write(" extends ");
        if (name.equals("Object")) {
            writer.write("io.github.jwharm.javagi.ResourceBase");
        } else if (parentClass == null) {
            writer.write("org.gtk.gobject.Object");
        } else {
            writer.write(parentClass);
        }
        for (int i = 0; i < implementsList.size(); i++) {
            if (i == 0) {
                writer.write(" implements " + Conversions.toQualifiedJavaType(implementsList.get(i).name));
            } else {
                writer.write(", " + Conversions.toQualifiedJavaType(implementsList.get(i).name));
            }
        }
        writer.write(" {\n");
        writer.write("\n");

        generateMemoryAddressConstructor(writer);
        generateCastFromGObject(writer);
        generateConstructors(writer);

        for (Method m : methodList) {
            if (m.isSafeToBind()) {
                m.generate(writer, false, false);
            }
        }

        for (Function function : functionList) {
            if (function.isSafeToBind()) {
                function.generate(writer, false, true);
            }
        }

        for (Signal s : signalList) {
            if (s.isSafeToBind()) {
                s.generate(writer, false);
            }
        }

        writer.write("}\n");
    }

}
