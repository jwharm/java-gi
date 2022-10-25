package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class BindingsGenerator {

    public static StringBuilder signalCallbackFunctions;

    public BindingsGenerator() {
    }

    public void generate(Repository gir, Set<String> natives, Path basePath) throws IOException {
        signalCallbackFunctions = new StringBuilder();

        Files.createDirectories(basePath);

        for (RegisteredType rt : gir.namespace.registeredTypeMap.values()) {
            
            if (rt instanceof io.github.jwharm.javagi.model.Record rec
                    && rec.isEmpty()) {
                continue;
            }

            try (Writer writer = Files.newBufferedWriter(basePath.resolve(rt.javaName + ".java"))) {
                rt.generate(writer);
            }
        }
        generateGlobals(gir, natives, basePath);
    }

    public void generateGlobals(Repository gir, Set<String> natives, Path basePath) throws IOException {
        String className = Conversions.toSimpleJavaType(gir.namespace.name);
        try (Writer writer = Files.newBufferedWriter(basePath.resolve(className + ".java"))) {
            writer.write("package " + gir.namespace.packageName + ";\n");
            writer.write("\n");
            RegisteredType.generateImportStatements(writer);
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
            writer.write("    @ApiStatus.Internal static void javagi$ensureInitialized() {}\n");
 
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
