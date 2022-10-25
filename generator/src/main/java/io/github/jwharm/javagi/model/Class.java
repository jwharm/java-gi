package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

import java.io.IOException;
import java.io.Writer;

public class Class extends RegisteredType {

    public Class(GirElement parent, String name, String parentClass, String cType) {
        super(parent, name, parentClass, cType);
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public class " + javaName);
        writer.write(" extends ");
        if (name.equals("Object")) {
            writer.write("io.github.jwharm.javagi.ResourceBase");
        } else if (parentClass == null) {
            writer.write("org.gtk.gobject.Object");
        } else {
            writer.write(parentClass);
        }
        for (int i = 0; i < implementsList.size(); i++) {
            if (i == 0) {
                writer.write(" implements " + Conversions.toQualifiedJavaType(implementsList.get(i).name, getNamespace().packageName));
            } else {
                writer.write(", " + Conversions.toQualifiedJavaType(implementsList.get(i).name, getNamespace().packageName));
            }
        }
        writer.write(" {\n");

        generateEnsureInitialized(writer);
        generateMemoryAddressConstructor(writer);
        generateCastFromGObject(writer);
        generateConstructors(writer);

        for (Method m : methodList) {
            m.generate(writer, false, false);
        }

        for (Function function : functionList) {
            function.generate(writer, false, true);
        }

        for (Signal s : signalList) {
            s.generate(writer, false);
        }
        
        if (! (constructorList.isEmpty() && methodList.isEmpty() && functionList.isEmpty())) {
        	writer.write("    \n");
            writer.write("    private static class DowncallHandles {\n");
            for (Constructor c : constructorList) {
                c.generateMethodHandle(writer, false);
            }
            for (Method m : methodList) {
                m.generateMethodHandle(writer, false);
            }
            for (Function f : functionList) {
                f.generateMethodHandle(writer, false);
            }
            writer.write("    }\n");
        }
        
        if (! signalList.isEmpty()) {
        	writer.write("    \n");
        	writer.write("    @ApiStatus.Internal\n");
            writer.write("    public static class Callbacks {\n");
            for (Signal s : signalList) {
                s.generateStaticCallback(writer, false);
            }
            writer.write("    }\n");
        }
        writer.write("}\n");
    }

    public String getInteropString(String paramName, boolean isPointer, boolean transferOwnership) {
        if (transferOwnership) {
            return paramName + ".refcounted().unowned().handle()";
        } else {
            return paramName + ".handle()";
        }
    }
}
