package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class RegisteredType extends GirElement {

    public final String javaName, parentClass;

    public boolean used = false;

    public RegisteredType(GirElement parent, String name, String parentClass) {
        super(parent);
        this.parentClass = Conversions.toQualifiedJavaType(parentClass);
        this.name = name;
        this.javaName = Conversions.toSimpleJavaType(name);
    }

    public abstract void generate(Writer writer) throws IOException;

    protected void generatePackageDeclaration(Writer writer) throws IOException {
        writer.write("package " + getNamespace().packageName + ";\n");
        writer.write("\n");
    }

    public static void generateImportStatements(Writer writer) throws IOException {
        writer.write("import org.gtk.gobject.*;\n");
        writer.write("import io.github.jwharm.javagi.interop.*;\n");
        writer.write("import jdk.incubator.foreign.*;\n");
        writer.write("import java.lang.invoke.*;\n");
        writer.write("\n");
    }

    protected void generateJavadoc(Writer writer) throws IOException {
        if (doc != null) {
            doc.generate(writer, 0);
        }
    }

    /**
     * Generate standard constructors from a MemoryAddress and a GObject
     */
    protected void generateCastFromGObject(Writer writer) throws IOException {
        writer.write("    /** Cast object to " + javaName + " */\n");
        writer.write("    public static " + javaName + " castFrom(org.gtk.gobject.Object gobject) {\n");
        writer.write("        return new " + javaName + "(ProxyFactory.getProxy(gobject.HANDLE()));\n");
        writer.write("    }\n");
        writer.write("    \n");
    }

    protected void generateMemoryAddressConstructor(Writer writer) throws IOException {
        writer.write("    public " + javaName + "(io.github.jwharm.javagi.interop.Proxy proxy) {\n");
        writer.write("        super(proxy);\n");
        writer.write("    }\n");
        writer.write("    \n");
    }

    /**
     * GObject constructors are named, and this allows multiple constructors with the same parameter types.
     * This method detects the problem, and generates a regular constructor when possible, or else static factory
     * methods.
     */
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
