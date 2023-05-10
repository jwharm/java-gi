package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.Class;
import io.github.jwharm.javagi.model.Record;
import io.github.jwharm.javagi.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BindingsGenerator {

    /**
     * Generate Java bindings for the provided GI repository
     */
    public static void generate(Repository gir, Path basePath) throws IOException {

        Files.createDirectories(basePath);

        // Create a java file for each RegisteredType (class, interface, ...)
        for (RegisteredType rt : gir.namespace.registeredTypeMap.values()) {
            
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
        generateGlobals(gir, basePath);
    }

    /**
     * Generate the contents for the class with the namespace-global declarations and a package-info.
     * The name of the class is the namespace identifier.
     */
    public static void generateGlobals(Repository gir, Path basePath) throws IOException {
        String className = Conversions.convertToJavaType(gir.namespace.globalClassName, false, gir.namespace);
        try (SourceWriter writer = new SourceWriter(Files.newBufferedWriter(basePath.resolve(className + ".java")))) {
            
            writer.write("package " + gir.namespace.packageName + ";\n");
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
            if (gir.natives != null) {
                for (String libraryName : gir.natives) {
                    writer.write("    LibLoad.loadLibrary(\"" + libraryName + "\");\n");
                }
            }

            // Register types
            writer.write("    registerTypes();\n");
            writer.write("}\n");
            writer.write("\n");
            writer.write("@ApiStatus.Internal public static void javagi$ensureInitialized() {}\n");
            
            // Generate constants
            for (Constant constant : gir.namespace.constantList) {
                constant.generate(writer);
            }

            // Generate global functions
            for (Function function : gir.namespace.functionList) {
                function.generate(writer);
            }
            
            // Generate downcallhandles
            if (! gir.namespace.functionList.isEmpty()) {
                writer.write("\n");
                writer.write("private static class DowncallHandles {\n");
                writer.increaseIndent();
                for (Function f : gir.namespace.functionList) {
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
            for (Class c : gir.namespace.classList) {
                writer.write("if (" + c.javaName + ".isAvailable()) TypeCache.register(" + c.javaName + ".getType(), " + c.getConstructorString() + ");\n");
            }

            // Interfaces
            for (Interface i : gir.namespace.interfaceList) {
                writer.write("if (" + i.javaName + ".isAvailable()) TypeCache.register(" + i.javaName + ".getType(), " + i.getConstructorString() + ");\n");
            }

            // Aliases
            for (Alias a : gir.namespace.aliasList) {
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
            writer.write(" * This package contains the generated bindings for " + gir.namespace.name + ".\n");
            writer.write(" * <p>\n");

            if (gir.natives != null) {
                writer.write(" * The following native libraries are required and will be loaded: ");
                for (String libraryName : gir.natives) {
                    writer.write(" {@code " + libraryName + "}");
                }
                writer.write("\n");
                writer.write(" * <p>\n");
            }
            writer.write(" * For namespace-global declarations, refer to the {@link " + className + "} class documentation.\n");

            if (gir.platforms.size() < 3) {
                writer.write(" * <p>\n");
                writer.write(" * This package is only available on ");
                boolean first = true;
                for (var p : gir.platforms) {
                    if (! first) {
                        writer.write(" and ");
                    }
                    writer.write(Conversions.toCamelCase(p.name, true));
                    first = false;
                }
                writer.write(".\n");
            }

            writer.write(" */\n");
            writer.write("package " + gir.namespace.packageName + ";\n");
        }
    }
}
