package girparser.model;

import girparser.generator.Conversions;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

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
        writer.write(Objects.requireNonNullElse(parentClass, "org.gtk.gobject.Object"));
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

        for (Signal s : signalList) {
            if (s.isSafeToBind()) {
                s.generate(writer, false, false);
            }
        }

        writer.write("}\n");
    }

}
