package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.GTypeDefinitions;

public class Alias extends ValueWrapper {
    
    public static final int UNKNOWN_ALIAS = 0;
    public static final int RECORD_ALIAS = 1;
    public static final int CLASS_ALIAS = 2;
    public static final int INTERFACE_ALIAS = 3;
    public static final int CALLBACK_ALIAS = 4;
    public static final int VALUE_ALIAS = 5;
    
    public int aliasFor() {
        if (type.isPrimitive || "utf8".equals(type.name)) {
            return VALUE_ALIAS;
        } else if (type.girElementInstance == null) {
            return UNKNOWN_ALIAS;
        } else if (type.girElementInstance instanceof Callback) {
            return CALLBACK_ALIAS;
        } else if (type.girElementInstance instanceof Interface) {
            return INTERFACE_ALIAS;
        } else if (type.girElementInstance instanceof Record) {
            return RECORD_ALIAS;
        } else if (type.girElementInstance instanceof Class) {
            return CLASS_ALIAS;
        }
        return UNKNOWN_ALIAS;
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

        switch (aliasFor()) {
            case CLASS_ALIAS, RECORD_ALIAS -> {
                writer.write("public class " + javaName);
                if (type.qualifiedJavaType.equals("void")) {
                    writer.write(" extends org.gtk.gobject.Object {\n");
                } else {
                    writer.write(" extends " + type.qualifiedJavaType + " {\n");
                }
                
                generateMemoryAddressConstructor(writer);
                
                // Do not generate a cast from GObject for records
                if (aliasFor() == CLASS_ALIAS) {
                    generateCastFromGObject(writer);
                }
                
                writer.write("}\n");
            }
            case INTERFACE_ALIAS, CALLBACK_ALIAS -> {
                writer.write("public interface " + javaName + " extends " + type.qualifiedJavaType + " {\n");
                writer.write("}\n");
            }
            case VALUE_ALIAS -> {
                String genericType = Conversions.primitiveClassName(type.qualifiedJavaType);
                if ("utf8".equals(type.name)) {
                    genericType = "java.lang.String";
                }
                writer.write("public class " + javaName + " extends io.github.jwharm.javagi.Alias<" + genericType + "> {");
                writer.write("\n");
                generateValueConstructor(writer, type.qualifiedJavaType);
                
                // Write fundamental G_TYPE definitions
                if (getNamespace().packageName.equals("org.gtk.glib") && this.javaName.equals("Type")) {
                    GTypeDefinitions.generateFundamentalGTypes(writer);
                }
                
                writer.write("}\n");
            }
            case UNKNOWN_ALIAS -> {
                writer.write("public class " + javaName + " {\n");
                writer.write("}\n");
            }
        }
    }
    
    @Override
    public String getInteropString(String paramName, boolean isPointer, String transferOwnership) {
        if (aliasFor() == VALUE_ALIAS) {
            return super.getInteropString(paramName, isPointer, transferOwnership);
        } else {
            return type.girElementInstance.getInteropString(paramName, isPointer, transferOwnership);
        }
    }
}
