package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

import io.github.jwharm.javagi.generator.Conversions;

/**
 * Represents an {@code alias} element
 */
public class Alias extends ValueWrapper {

    /**
     * Represents the different types of elements which may be aliased
     */
    public enum TargetType {
        CLASS, RECORD, INTERFACE, CALLBACK, VALUE, UNKNOWN
    }

    /**
     * Gets the type of element that this alias targets
     */
    public TargetType getTargetType() {
        if (type.isPrimitive
                || "java.lang.String".equals(type.qualifiedJavaType)
                || "java.lang.foreign.MemoryAddress".equals(type.qualifiedJavaType)) {
            return TargetType.VALUE;
        } else if (type.girElementInstance instanceof Callback) {
            return TargetType.CALLBACK;
        } else if (type.girElementInstance instanceof Interface) {
            return TargetType.INTERFACE;
        } else if (type.girElementInstance instanceof Record) {
            return TargetType.RECORD;
        } else if (type.girElementInstance instanceof Class) {
            return TargetType.CLASS;
        }
        return TargetType.UNKNOWN;
    }

    public Alias(GirElement parent, String name, String cType, String version) {
        super(parent, name, null, cType, version);
    }

    // Aliases (typedefs) don't exist in Java. We can emulate this using inheritance.
    // For primitives and Strings, we wrap the value.
    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        switch (getTargetType()) {
            case CLASS, RECORD -> {
                writer.write("public class " + javaName);
                if (type.qualifiedJavaType.equals("void")) {
                    writer.write(" extends org.gtk.gobject.GiObject {\n");
                } else {
                    writer.write(" extends " + type.qualifiedJavaType + " {\n");
                }
                
                generateMemoryAddressConstructor(writer);
                
                // Do not generate a cast from GObject for records
                if (getTargetType() == TargetType.CLASS) {
                    generateCastFromGObject(writer);
                }

                generateMarshal(writer);
            }
            case INTERFACE, CALLBACK -> {
                writer.write("public interface " + javaName + " extends " + type.qualifiedJavaType + " {\n");
            }
            case VALUE -> {
                String genericType = Conversions.primitiveClassName(type.qualifiedJavaType);
                if ("utf8".equals(type.name)) {
                    genericType = "java.lang.String";
                } else if ("java.lang.foreign.MemoryAddress".equals(type.qualifiedJavaType)) {
                    genericType = type.qualifiedJavaType;
                }
                writer.write("public class " + javaName + " extends io.github.jwharm.javagi.Alias<" + genericType + "> {");
                writer.write("\n");
                generateValueConstructor(writer, type.qualifiedJavaType);
            }
            default -> {
                writer.write("public class " + javaName + " {\n");
            }
        }
        generateInjected(writer);
        writer.write("}\n");
    }
    
    @Override
    public String getInteropString(String paramName, boolean isPointer, String transferOwnership) {
        if (getTargetType() == TargetType.VALUE) {
            return super.getInteropString(paramName, isPointer, transferOwnership);
        } else {
            return type.girElementInstance.getInteropString(paramName, isPointer, transferOwnership);
        }
    }
}
