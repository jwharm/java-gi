package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.Class;
import io.github.jwharm.javagi.model.*;

import java.io.IOException;
import java.io.Writer;
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

        generateTypeRegister(gir, natives, basePath);
    }

    /**
     * Generate the contents for the class with the namespace-global declarations.
     * The name of the class is the namespace identifier.
     */
    public static void generateGlobals(Repository gir, Set<String> natives, Path basePath) throws IOException {
        String className = Conversions.convertToJavaType(gir.namespace.globalClassName, false, gir.namespace);
        try (Writer writer = Files.newBufferedWriter(basePath.resolve(className + ".java"))) {
            writer.write("package " + gir.namespace.packageName + ";\n");
            writer.write("\n");
            RegisteredType.generateImportStatements(writer);
            writer.write("/**\n");
            writer.write(" * Constants and functions that are declared in the global " + className + " namespace.\n");
            writer.write(" */\n");
            writer.write("public final class " + className + " {\n");
            writer.write("    \n");
            writer.write("    static {\n");
            for (String libraryName : natives) {
                writer.write("        LibLoad.loadLibrary(\"" + libraryName + "\");\n");
            }
            writer.write("            JavaGITypeRegister.register();\n");
            writer.write("    }\n");
            writer.write("    \n");
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

    public static void generateTypeRegister(Repository gir, Set<String> natives, Path basePath) throws IOException {
        String className = Conversions.convertToJavaType("JavaGITypeRegister", false, gir.namespace);
        try (Writer writer = Files.newBufferedWriter(basePath.resolve(className + ".java"))) {
            writer.write("package " + gir.namespace.packageName + ";\n");
            writer.write("\n");
            RegisteredType.generateImportStatements(writer);
            writer.write("final class " + className + " {\n");
            writer.write("    \n");
            writer.write("    static void register() {\n");

            for (Class c : gir.namespace.classList)
                writer.write("        if (" + c.javaName + ".isAvailable()) Interop.typeRegister.put(" + c.javaName + ".getType(), " + c.javaName + ".fromAddress);\n");

            for (Interface c : gir.namespace.interfaceList)
                writer.write("        if (" + c.javaName + ".isAvailable()) Interop.typeRegister.put(" + c.javaName + ".getType(), " + c.javaName + ".fromAddress);\n");

            for (Alias c : gir.namespace.aliasList)
                if (c.getTargetType() == Alias.TargetType.CLASS || c.getTargetType() == Alias.TargetType.INTERFACE)
                    writer.write("        if (" + c.javaName + ".isAvailable()) Interop.typeRegister.put(" + c.javaName + ".getType(), " + c.javaName + ".fromAddress);\n");

            writer.write("    }\n");
            writer.write("}\n");
        }
    }
}
