package girparser.model;

import girparser.generator.Conversions;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.List;
import java.util.stream.Collectors;

public class Class extends RegisteredType {
    public final String parentClass;

    public Class(GirElement parent, String name, String parentClass) {
        super(parent, name);
        this.parentClass = Conversions.toQualifiedJavaType(parentClass);
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public class " + javaName);
        writer.write(" extends ");
        writer.write(Objects.requireNonNullElse(parentClass, "org.gtk.gobject.Object"));
        writer.write(" {\n");
        writer.write("\n");

        generateMemoryAddressConstructor(writer);
        generateCastFromGObject(writer);
        generateConstructors(writer);

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

    // GObject constructors are named, and this allows multiple constructors with the same parameter types.
    // This method detects the problem, and generates a regular constructor when possible, or else static factory
    // methods.
    protected void generateConstructors(Writer writer) throws IOException {
        // Generate type signatures for all constructors.
        List<String> signatures = new ArrayList<>();
        for (Constructor c : constructorList) {
            signatures.add(signature(c));
        }
        Map<String, Long> counts = signatures.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        for (Constructor c : constructorList) {
            if (c.isSafeToBind()) {
                if (counts.get(signature(c)) == 1) {
                    // The type signature of this constructor is unique. Generate a regular constructor.
                    c.generate(writer);
                } else {
                    // This constructor is one of multiple constructors with the same type signature.
                    // Generate a static factory method, unless the name of the constructor is "new", which would be
                    // translated into an ugly "new_" method, so will treat "new" constructors as the "default" one
                    // and generate a regular constructor anyway.
                    if (c.name.equals("new")) {
                        c.generate(writer);
                    } else {
                        c.generateNamed(writer);
                    }
                }
            }
        }
    }

    private String signature(Constructor c) {
        return c.parameters == null ? "" : c.parameters.parameterList.stream()
                .map(p -> p.type == null ? "" : p.type.qualifiedJavaType)
                .collect(Collectors.joining(","));
    }
}
