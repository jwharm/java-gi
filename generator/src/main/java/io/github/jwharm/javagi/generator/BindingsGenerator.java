package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class BindingsGenerator {

    public static StringBuilder signalCallbackFunctions;

    public BindingsGenerator() {
    }

    public void generate(Repository gir, String outputDir) throws IOException {
        signalCallbackFunctions = new StringBuilder();
        String basePath = outputDir + gir.namespace.pathName;

        new File(basePath).mkdirs();

        for (RegisteredType rt : gir.namespace.registeredTypeMap.values()) {
            
            if (rt instanceof io.github.jwharm.javagi.model.Record rec
                    && rec.isEmpty()) {
                continue;
            }
            // No support for callbacks with out parameters or arrays for now
            if (rt instanceof io.github.jwharm.javagi.model.Callback cb
                    && cb.parameters != null
                    && cb.parameters.parameterList.stream().anyMatch(Parameter::isOutParameter)) {
                continue;
            }
            if (rt instanceof io.github.jwharm.javagi.model.Callback cb
                    && cb.parameters != null
                    && cb.parameters.parameterList.stream().anyMatch(p -> p.array != null)) {
                continue;
            }

            try (FileWriter writer = new FileWriter(basePath + rt.javaName + ".java")) {
                rt.generate(writer);
            }
        }
        generateGlobals(gir, basePath);
    }

    public void generateGlobals(Repository gir, String basePath) throws IOException {
        String className = Conversions.toSimpleJavaType(gir.namespace.name);
        try (FileWriter writer = new FileWriter(basePath + className + ".java")) {
            writer.write("package " + gir.namespace.packageName + ";\n");
            writer.write("\n");
            RegisteredType.generateImportStatements(writer);
            writer.write("public final class " + className + " {\n");
            writer.write("    \n");

            for (Constant constant : gir.namespace.constantList) {
                constant.generate(writer);
            }

            for (Function function : gir.namespace.functionList) {
                if (function.isSafeToBind()) {
                    function.generate(writer, function.parent instanceof Interface, true);
                }
            }
            
            writer.write(signalCallbackFunctions.toString());

            writer.write("}\n");
        }
    }
}
