package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class Repository extends GirElement {

    public final Module module;
    public Namespace namespace = null;
    public Package package_ = null;
    public boolean generate;
    public Set<String> natives;

    public Repository(Module module) {
        super(null);
        this.module = module;
    }

    /**
     * Generate Java bindings for the provided GI repository
     */
    public void generate(Path basePath) throws IOException {

        Files.createDirectories(basePath);

        // Create a java file for each RegisteredType (class, interface, ...)
        for (RegisteredType rt : namespace.registeredTypeMap.values()) {

            // Class type structs are generated as inner classes
            if (rt instanceof Record rec && rec.isGTypeStructFor != null) {
                continue;
            }

            // Private inner structs can be omitted from language bindings
            if (rt instanceof Record rec && rec.name.endsWith("Private")) {
                continue;
            }

            // Generate the class
            try (SourceWriter writer = new SourceWriter(Files.newBufferedWriter(basePath.resolve(rt.javaName + ".java")))) {
                rt.generate(writer);
            }
        }
        // Create a class file for global declarations
        generateGlobals(basePath);
    }

    /**
     * Generate the contents for the class with the namespace-global declarations and a package-info.
     * The name of the class is the namespace identifier.
     */
    public void generateGlobals(Path basePath) throws IOException {
        String className = Conversions.convertToJavaType(namespace.globalClassName, false, namespace);
        try (SourceWriter writer = new SourceWriter(Files.newBufferedWriter(basePath.resolve(className + ".java")))) {

            writer.write("package " + namespace.packageName + ";\n");
            writer.write("\n");
            RegisteredType.generateImportStatements(writer);

            writer.write("/**\n");
            writer.write(" * Constants and functions that are declared in the global " + className + " namespace.\n");
            writer.write(" */\n");
            writer.write("public final class " + className + " {\n");
            writer.increaseIndent();
            writer.write("\n");
            writer.write("static {\n");

            // Load libraries
            if (natives != null) {
                for (String libraryName : natives) {
                    writer.write("    LibLoad.loadLibrary(\"" + libraryName + "\");\n");
                }
            }

            // Register types
            writer.write("    registerTypes();\n");
            writer.write("}\n");
            writer.write("\n");
            writer.write("@ApiStatus.Internal public static void javagi$ensureInitialized() {}\n");

            // Generate constants
            for (Constant constant : namespace.constantList) {
                constant.generate(writer);
            }

            // Generate global functions
            for (Function function : namespace.functionList) {
                function.generate(writer);
            }

            // Generate downcallhandles
            if (! namespace.functionList.isEmpty()) {
                writer.write("\n");
                writer.write("private static class DowncallHandles {\n");
                writer.increaseIndent();
                for (Function f : namespace.functionList) {
                    f.generateMethodHandle(writer);
                }
                writer.decreaseIndent();
                writer.write("}\n");
            }

            // Generate registerTypes function
            writer.write("\n");
            writer.write("private static void registerTypes() {\n");
            writer.increaseIndent();

            // Classes
            for (Class c : namespace.classList) {
                writer.write("if (" + c.javaName + ".isAvailable()) TypeCache.register(" + c.javaName + ".getType(), " + c.getConstructorString() + ");\n");
            }

            // Interfaces
            for (Interface i : namespace.interfaceList) {
                writer.write("if (" + i.javaName + ".isAvailable()) TypeCache.register(" + i.javaName + ".getType(), " + i.getConstructorString() + ");\n");
            }

            // Aliases
            for (Alias a : namespace.aliasList) {
                if (a.getTargetType() == Alias.TargetType.CLASS) {
                    Class c = (Class) a.type.girElementInstance;
                    writer.write("if (" + a.javaName + ".isAvailable()) TypeCache.register(" + a.javaName + ".getType(), " + c.getConstructorString() + ");\n");
                } else if (a.getTargetType() == Alias.TargetType.INTERFACE) {
                    Interface i = (Interface) a.type.girElementInstance;
                    writer.write("if (" + a.javaName + ".isAvailable()) TypeCache.register(" + a.javaName + ".getType(), " + i.getConstructorString() + ");\n");
                }
            }

            writer.decreaseIndent();
            writer.write("}\n");

            writer.decreaseIndent();
            writer.write("}\n");
        }

        // Generate package-info.java file
        try (SourceWriter writer = new SourceWriter(Files.newBufferedWriter(basePath.resolve("package-info.java")))) {
            writer.write("/**\n");
            writer.write(" * This package contains the generated bindings for " + namespace.name + ".\n");
            writer.write(" * <p>\n");

            if (natives != null) {
                writer.write(" * The following native libraries are required and will be loaded: ");
                for (String libraryName : natives) {
                    writer.write(" {@code " + libraryName + "}");
                }
                writer.write("\n");
                writer.write(" * <p>\n");
            }
            writer.write(" * For namespace-global declarations, refer to the {@link " + className + "} class documentation.\n");

            if (platforms.size() < 3) {
                writer.write(" * <p>\n");
                writer.write(" * This package is only available on ");
                boolean first = true;
                for (var p : platforms) {
                    if (! first) {
                        writer.write(" and ");
                    }
                    writer.write(Conversions.toCamelCase(p.name, true));
                    first = false;
                }
                writer.write(".\n");
            }

            writer.write(" */\n");
            writer.write("package " + namespace.packageName + ";\n");
        }
    }

    public Repository copy() {
        var copy = new Repository(module);
        copy.namespace = namespace.copy();
        copy.package_ = package_;
        copy.generate = generate;
        copy.natives = natives;
        return copy;
    }
}
