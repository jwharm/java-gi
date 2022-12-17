package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class BindingsGenerator {

    /**
     * Callback functions for signals are appended to this StringBuilder.<br>
     * When a class has been generated, these function declarations are written into
     * a static inner class "Callbacks".
     */
    public static final StringBuilder signalCallbackFunctions = new StringBuilder();

    /**
     * Generate Java bindings for the provided GI repository
     */
    public static void generate(Repository gir, Set<String> natives, Path basePath) throws IOException {
        signalCallbackFunctions.setLength(0);


        Files.createDirectories(basePath);

        // Create a java file for each RegisteredType (class, interface, ...)
        for (RegisteredType rt : gir.namespace.registeredTypeMap.values()) {
            
            try (Writer writer = Files.newBufferedWriter(basePath.resolve(rt.javaName + ".java"))) {
                rt.generate(writer);
            }
        }
        // Create a class file for global declarations
        generateGlobals(gir, natives, basePath);
    }

    /**
     * Generate the contents for the class with the namespace-global declarations.
     * The name of the class is the namespace identifier.
     */
    public static void generateGlobals(Repository gir, Set<String> natives, Path basePath) throws IOException {
        String className = Conversions.toSimpleJavaType(gir.namespace.name);
        try (Writer writer = Files.newBufferedWriter(basePath.resolve(className + ".java"))) {
            writer.write("package " + gir.namespace.packageName + ";\n");
            writer.write("\n");
            RegisteredType.generateImportStatements(writer);
            writer.write("/**\n");
            writer.write(" * Constants and functions that are declared in the global " + className + " namespace.\n");
            writer.write(" */\n");
            writer.write("public final class " + className + " {\n");
            writer.write("    \n");
            if (!natives.isEmpty()) {
                writer.write("    static {\n");
                for (String libraryName : natives) {
                    writer.write("        System.loadLibrary(\"" + libraryName + "\");\n");
                }
                writer.write("    }\n");
                writer.write("    \n");
            }
            writer.write("    @ApiStatus.Internal public static void javagi$ensureInitialized() {}\n");
 
            for (Constant constant : gir.namespace.constantList) {
                constant.generate(writer);
            }

            for (Function function : gir.namespace.functionList) {
                function.generate(writer, function.parent instanceof Interface, true);
            }
            
            if (! gir.namespace.functionList.isEmpty()) {
                writer.write("    \n");
                writer.write("    private static class DowncallHandles {\n");
                for (Function f : gir.namespace.functionList) {
                    f.generateMethodHandle(writer, false);
                }
                writer.write("    }\n");
            }
            
            if (! gir.namespace.callbackList.isEmpty()) {
                writer.write("    \n");
                writer.write("    @ApiStatus.Internal\n");
                writer.write("    public static class Callbacks {\n");
                writer.write(signalCallbackFunctions.toString());
                writer.write("    }\n");
            }

            writer.write("}\n");
        }
    }
}
