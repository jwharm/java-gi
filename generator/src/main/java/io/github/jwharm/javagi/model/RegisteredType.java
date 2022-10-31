package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

import io.github.jwharm.javagi.generator.Conversions;

public abstract class RegisteredType extends GirElement {

    public final String javaName, parentClass, cType, version;

    public RegisteredType(GirElement parent, String name, String parentClass, String cType, String version) {
        super(parent);
        this.parentClass = Conversions.toQualifiedJavaType(parentClass, getNamespace().packageName);
        this.name = name;
        this.cType = cType;
        this.version = version;
        this.javaName = Conversions.toSimpleJavaType(name);
    }

    public abstract void generate(Writer writer) throws IOException;

    protected void generatePackageDeclaration(Writer writer) throws IOException {
        writer.write("package " + getNamespace().packageName + ";\n");
        writer.write("\n");
    }

    public static void generateImportStatements(Writer writer) throws IOException {
        writer.write("import io.github.jwharm.javagi.*;\n");
        writer.write("import java.lang.foreign.*;\n");
        writer.write("import java.lang.invoke.*;\n");
        writer.write("import org.jetbrains.annotations.*;\n");
        writer.write("\n");
    }

    protected void generateJavadoc(Writer writer) throws IOException {
        if (doc != null) {
            doc.generate(writer, 0);
        }
    }
    
    protected void generateCType(Writer writer) throws IOException {
    	writer.write("    \n");
    	writer.write("    private static final java.lang.String cTypeName = " + Conversions.literal("java.lang.String", cType) + ";\n");
    }
    
    protected void generateMemoryLayout(Writer writer) throws IOException {
    	if (this instanceof Bitfield || this instanceof Enumeration) {
    		return;
    	}
    	
    	if (! fieldList.isEmpty()) {
            writer.write("    \n");
            
            writer.write("    private static GroupLayout memoryLayout = MemoryLayout.");
            if (this instanceof Union) {
            	writer.write("unionLayout(\n");
            } else {
            	writer.write("structLayout(\n");
            }
            
            // How many bytes have we generated thus far
            int size = 0;
            
            for (int f = 0; f < fieldList.size(); f++) {
            	Field field = fieldList.get(f);
            	if (f > 0) {
            		writer.write(",\n");
            	}
            	// Get the byte size of the field. For example: int = 32bit, pointer = 64bit, char = 8bit
            	int s = field.getSize(field.getMemoryType());
            	// If the previous field had a smaller byte size than this one, add padding
            	if (size % s > 0) {
            		int padding = s - (size % s);
            		writer.write("        MemoryLayout.paddingLayout(" + padding + "),\n");
            		size += padding;
            	}
            	// Write the memorylayout declaration
            	writer.write("        " + field.getMemoryLayoutString());
            	size += s;
            }
            // Write the name of the struct
            writer.write("\n    ).withName(\"" + this.cType + "\");\n");
    	}
    	
        writer.write("    \n");
        writer.write("    /**\n");
        if (fieldList.isEmpty()) {
	        writer.write("     * Memory layout of the native struct is unknown.\n");
	        writer.write("     * @return always {@code Interop.valueLayout.ADDRESS}\n");
        } else {
	        writer.write("     * The memory layout of the native struct.\n");
	        writer.write("     * @return the memory layout\n");
        }
        writer.write("     */\n");
        writer.write("    public static MemoryLayout getMemoryLayout() {\n");
        if (fieldList.isEmpty()) {
            writer.write("        return Interop.valueLayout.ADDRESS;\n");
        } else {
            writer.write("        return memoryLayout;\n");
        }
        writer.write("    }\n");
    }
    
    /**
     * Generate standard constructors from a MemoryAddress and a GObject
     * @param writer The writer for the source code
     * @throws IOException Thrown by {@code writer.write()}
     */
    protected void generateCastFromGObject(Writer writer) throws IOException {
        writer.write("    \n");
        writer.write("    /**\n");
        writer.write("     * Cast object to " + javaName + " if its GType is a (or inherits from) \"" + cType + "\".\n");
        writer.write("     * @param  gobject            An object that inherits from GObject\n");
        writer.write("     * @return                    An instance of \"" + javaName + "\" that points to the memory address of the provided GObject.\n");
        writer.write("     *                            The type of the object is checked with {@code g_type_check_instance_is_a}.\n");
        writer.write("     * @throws ClassCastException If the GType is not derived from \"" + cType + "\", a ClassCastException will be thrown.\n");
        writer.write("     */\n");
        writer.write("    public static " + javaName + " castFrom(org.gtk.gobject.Object gobject) {\n");
        writer.write("        if (org.gtk.gobject.GObject.typeCheckInstanceIsA(gobject.g_type_instance$get(), org.gtk.gobject.GObject.typeFromName(\"" + cType + "\"))) {\n");
        writer.write("            return new " + javaName + "(gobject.refcounted());\n");
        writer.write("        } else {\n");
        writer.write("            throw new ClassCastException(\"Object type is not an instance of " + cType + "\");\n");
        writer.write("        }\n");
        writer.write("    }\n");
    }

    protected void generateMemoryAddressConstructor(Writer writer) throws IOException {
        writer.write("    \n");
    	writer.write("    @ApiStatus.Internal\n");
        writer.write("    public " + javaName + "(io.github.jwharm.javagi.Refcounted ref) {\n");
        writer.write("        super(ref);\n");
        writer.write("    }\n");
    }

    protected void generateEnsureInitialized(Writer writer) throws IOException {
        generateEnsureInitialized(writer, "    ");
    }

    protected void generateEnsureInitialized(Writer writer, String indent) throws IOException {
        writer.write(indent);
        writer.write("\n");
        writer.write(indent);
        writer.write("static {\n");
        writer.write(indent);
        writer.write("    " + Conversions.toSimpleJavaType(getNamespace().name) + ".javagi$ensureInitialized();\n");
        writer.write(indent);
        writer.write("}\n");
    }

    /**
     * Generates all constructors listed for this type. When the constructor is not named "new", a static
     * factory method is generated with the provided name.
     * @param writer The writer for the source code
     * @throws IOException Thrown by {@code writer.write()}
     */
    protected void generateConstructors(Writer writer) throws IOException {
        for (Constructor c : constructorList) {
            boolean isInterface = this instanceof Interface;
            if (c.name.equals("new")) {
                c.generate(writer, isInterface);
            } else {
                c.generateNamed(writer, isInterface);
            }
        }
    }
    
    /**
     * Generates an inner class DowncallHandles with MethodHandle declarations.
     * @param writer The writer for the source code
     * @param isInterface true when the type is an interface (this will write default instead of public methods)
     * @throws IOException Thrown by {@code writer.write()}
     */
    protected void generateDowncallHandles(Writer writer, boolean isInterface) throws IOException {
        if (! (constructorList.isEmpty() && methodList.isEmpty() && functionList.isEmpty())) {
        	writer.write("    \n");
        	writer.write(isInterface ? "    @ApiStatus.Internal\n    " : "    private ");
            writer.write("static class DowncallHandles {\n");
            for (Constructor c : constructorList) {
                c.generateMethodHandle(writer, isInterface);
            }
            for (Method m : methodList) {
                m.generateMethodHandle(writer, isInterface);
            }
            for (Function f : functionList) {
                f.generateMethodHandle(writer, isInterface);
            }
            writer.write("    }\n");
        }
    }
    
    /**
     * Generates an inner class Callbacks with static signal callback functions that will be used for upcalls
     * @param writer The writer for the source code
     * @param isInterface true when the type is an interface (this will write default instead of public methods)
     * @throws IOException Thrown by {@code writer.write()}
     */
    protected void generateSignalCallbacks(Writer writer, boolean isInterface) throws IOException {
        if (! signalList.isEmpty()) {
        	writer.write("    \n");
        	writer.write(isInterface ? "    @ApiStatus.Internal\n    " : "    private ");
            writer.write("static class Callbacks {\n");
            for (Signal s : signalList) {
                s.generateStaticCallback(writer, isInterface);
            }
            writer.write("    }\n");
        }
    }
    
    public abstract String getInteropString(String paramName, boolean isPointer, boolean transferOwnership);
}
