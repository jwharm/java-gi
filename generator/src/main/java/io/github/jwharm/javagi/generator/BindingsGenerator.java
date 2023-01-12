package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.Class;
import io.github.jwharm.javagi.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class BindingsGenerator {

    /**
     * Generate Java bindings for the provided GI repository
     */
    public static void generate(Repository gir, Set<String> natives, Path basePath) throws IOException {

        Files.createDirectories(basePath);

        // Create a java file for each RegisteredType (class, interface, ...)
        for (RegisteredType rt : gir.namespace.registeredTypeMap.values()) {
            
            try (SourceWriter writer = new SourceWriter(Files.newBufferedWriter(basePath.resolve(rt.javaName + ".java")))) {
                rt.generate(writer);
            }
        }
        // Create a class file for global declarations
        generateGlobals(gir, natives, basePath);
    }

    /**
     * Generate the contents for the class with the namespace-global declarations and a package-info.
     * The name of the class is the namespace identifier.
     */
    public static void generateGlobals(Repository gir, Set<String> natives, Path basePath) throws IOException {
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
            for (String libraryName : natives) {
                writer.write("    LibLoad.loadLibrary(\"" + libraryName + "\");\n");
            }
            writer.write("    registerTypes();\n");
            writer.write("}\n");
            writer.write("\n");
            writer.write("@ApiStatus.Internal public static void javagi$ensureInitialized() {}\n");

            for (Constant constant : gir.namespace.constantList) {
                constant.generate(writer);
            }

            for (Function function : gir.namespace.functionList) {
                function.generate(writer, function.parent instanceof Interface, true);
            }
            
            if (! gir.namespace.functionList.isEmpty()) {
                writer.write("\n");
                writer.write("private static class DowncallHandles {\n");
                writer.increaseIndent();
                for (Function f : gir.namespace.functionList) {
                    f.generateMethodHandle(writer, false);
                }
                writer.decreaseIndent();
                writer.write("}\n");
            }
            
            writer.write("\n");
            writer.write("private static void registerTypes() {\n");
            writer.increaseIndent();

            for (Class c : gir.namespace.classList)
                writer.write("if (" + c.javaName + ".isAvailable()) Interop.register(" + c.javaName + ".getType(), " + c.javaName + ".fromAddress);\n");

            for (Interface c : gir.namespace.interfaceList)
                writer.write("if (" + c.javaName + ".isAvailable()) Interop.register(" + c.javaName + ".getType(), " + c.javaName + ".fromAddress);\n");

            for (Alias c : gir.namespace.aliasList)
                if (c.getTargetType() == Alias.TargetType.CLASS || c.getTargetType() == Alias.TargetType.INTERFACE)
                    writer.write("if (" + c.javaName + ".isAvailable()) Interop.register(" + c.javaName + ".getType(), " + c.javaName + ".fromAddress);\n");

            writer.decreaseIndent();
            writer.write("}\n");

            writer.decreaseIndent();
            writer.write("}\n");
        }

        try (SourceWriter writer = new SourceWriter(Files.newBufferedWriter(basePath.resolve("package-info.java")))) {
            writer.write("/**\n");
            writer.write(" * This package contains the generated bindings for " + gir.namespace.name + ".\n");
            writer.write(" * The following natives are required and will be loaded:");
            for (String libraryName : natives) writer.write(" \"" + libraryName + "\"");
            writer.write("\n");
            writer.write(" * For namespace-global declarations, please view {@link " + className + "}\n");
            writer.write(" */\n");
            writer.write("package " + gir.namespace.packageName + ";\n");
        }
    }
}
