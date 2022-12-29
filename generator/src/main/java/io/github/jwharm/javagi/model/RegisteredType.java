package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

import io.github.jwharm.javagi.generator.Conversions;

public abstract class RegisteredType extends GirElement {

    public final String javaName;
    public final String parentClass;
    public final String cType;
    public final String version;
    protected final String qualifiedName;

    public String injected = null;

    public RegisteredType(GirElement parent, String name, String parentClass, String cType, String version) {
        super(parent);
        
        this.parentClass = Conversions.toQualifiedJavaType(parentClass, getNamespace());
        this.name = name;
        this.javaName = Conversions.toSimpleJavaType(name, getNamespace());
        this.qualifiedName = Conversions.toQualifiedJavaType(name, getNamespace());
        
        // If c type is not provided, guess that the name is also the c type
        this.cType = Objects.requireNonNullElse(cType, name);
        
        this.version = version;

        // Register the full names of this class and the parent class
        Conversions.superLookupTable.put(this.qualifiedName, this.parentClass);
    }

    // Find out if this tyjpe is a subclass of the provided classname
    private boolean isInstanceOf(String classname) {
        if (this.qualifiedName.equals(classname)) {
            return true;
        }
        String current = this.qualifiedName;
        while (current != null) {
            current = Conversions.superLookupTable.get(current);
            if (classname.equals(current)) {
                return true;
            }
        }
        return false;
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
        String typeLiteral = Conversions.literal("java.lang.String", cType);
        writer.write("    \n");
        writer.write("    private static final java.lang.String C_TYPE_NAME = " + typeLiteral + ";\n");
    }
    
    /**
     * Generate a function declaration to retrieve the type of this object.
     * @param getType the name of the function
     */
    protected void registerGetTypeFunction(String getType) {
        // Function
        Function getTypeFunc = new Function(this, "get_type", getType, null, null);
        getTypeFunc.returnValue = new ReturnValue(getTypeFunc, "none", null);
        getTypeFunc.returnValue.type = new Type(getTypeFunc.returnValue, "GType", "GType");
        
        // Docstrings
        getTypeFunc.doc = new Doc(getTypeFunc, null);
        getTypeFunc.doc.contents = "Get the gtype";
        getTypeFunc.returnValue.doc = new Doc(getTypeFunc.returnValue, null);
        getTypeFunc.returnValue.doc.contents = "The gtype";
        
        // Add the function
        this.functionList.add(getTypeFunc);
    }
    
    protected void generateMemoryLayout(Writer writer) throws IOException {
        if (this instanceof Bitfield || this instanceof Enumeration) {
            return;
        }
        
        if (! fieldList.isEmpty()) {
            writer.write("    \n");
            
            writer.write("    private static final GroupLayout memoryLayout = MemoryLayout.");
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
                
                // Calculate padding (except for union layouts)
                if (! (this instanceof Union)) {
                    
                    // If the previous field had a smaller byte size than this one, add padding (to a maximum of 64 bits)
                    if (size % s % 64 > 0) {
                        int padding = s - (size % s);
                        writer.write("        MemoryLayout.paddingLayout(" + padding + "),\n");
                        size += padding;
                    }
                    
                }
                
                // Write the memory layout declaration
                writer.write("        " + field.getMemoryLayoutString());
                
                size += s;
            }
            // Write the name of the struct
            writer.write("\n    ).withName(C_TYPE_NAME);\n");
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
        writer.write("    @ApiStatus.Internal\n");
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
        writer.write("     * <p>\n");
        writer.write("     * Internally, this creates a new Proxy object with the same ownership status as the parameter. If \n");
        writer.write("     * the parameter object was owned by the user, the Cleaner will be removed from it, and will be attached \n");
        writer.write("     * to the new Proxy object, so the call to {@code g_object_unref} will happen only once the new Proxy instance \n");
        writer.write("     * is garbage-collected. \n");
        writer.write("     * @param  gobject            An object that inherits from GObject\n");
        writer.write("     * @return                    A new proxy instance of type {@code " + javaName + "} that points to the memory address of the provided GObject.\n");
        writer.write("     *                            The type of the object is checked with {@code g_type_check_instance_is_a}.\n");
        writer.write("     * @throws ClassCastException If the GType is not derived from \"" + cType + "\", a ClassCastException will be thrown.\n");
        writer.write("     */\n");
        writer.write("    public static " + javaName + " castFrom(org.gtk.gobject.GObject gobject) {\n");
        writer.write("        if (org.gtk.gobject.GObjects.typeCheckInstanceIsA(org.gtk.gobject.TypeInstance.fromAddress.marshal(gobject.handle(), Ownership.NONE), " + javaName + ".getType())) {\n");
        writer.write("            return new " + javaName + (this instanceof Interface ? "Impl" : "") + "(gobject.handle(), gobject.yieldOwnership());\n");
        writer.write("        } else {\n");
        writer.write("            throw new ClassCastException(\"Object type is not an instance of " + cType + "\");\n");
        writer.write("        }\n");
        writer.write("    }\n");
    }

    protected void generateMemoryAddressConstructor(Writer writer) throws IOException {

        // Find out if this class is instanceof InitiallyUnowned
        boolean initiallyUnowned = isInstanceOf("org.gtk.gobject.InitiallyUnowned");

        writer.write("    \n");
        writer.write("    /**\n");
        writer.write("     * Create a " + javaName + " proxy instance for the provided memory address.\n");
        if (initiallyUnowned) {
            writer.write("     * <p>\n");
            writer.write("     * Because " +javaName + " is an {@code InitiallyUnowned} instance, when \n");
            writer.write("     * {@code ownership == Ownership.NONE}, the ownership is set to {@code FULL} \n");
            writer.write("     * and a call to {@code refSink()} is executed to sink the floating reference.\n");
        }
        writer.write("     * @param address   The memory address of the native object\n");
        writer.write("     * @param ownership The ownership indicator used for ref-counted objects\n");
        writer.write("     */\n");
        writer.write("    protected " + javaName + "(Addressable address, Ownership ownership) {\n");

        if (initiallyUnowned) {
            writer.write("        super(address, Ownership.FULL);\n");
            writer.write("        if (ownership == Ownership.NONE) {\n");
            writer.write("            refSink();\n");
            writer.write("        }\n");
        } else {
            writer.write("        super(address, ownership);\n");
        }
        writer.write("    }\n");
    }

    protected void generateMarshal(Writer writer) throws IOException {
        writer.write("    \n");
        writer.write("    @ApiStatus.Internal\n");
        String name = javaName + (this instanceof Interface ? "Impl" : "");
        writer.write("    public static final Marshal<Addressable, " + name + "> fromAddress = (input, ownership) -> "
                + "input.equals(MemoryAddress.NULL) ? null : new " + name + "(input, ownership);\n"
        );
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
        writer.write("    " + Conversions.toSimpleJavaType(getNamespace().globalClassName, getNamespace()) + ".javagi$ensureInitialized();\n");
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
     * @throws IOException Thrown by {@code writer.write()}
     */
    protected void generateDowncallHandles(Writer writer) throws IOException {
        boolean isInterface = this instanceof Interface;
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
    
    public String getInteropString(String paramName, boolean isPointer, String transferOwnership) {
        return paramName + ".handle()";
    }

    protected void generateInjected(Writer writer) throws IOException {
        if (injected != null) writer.write(injected);
    }
}
