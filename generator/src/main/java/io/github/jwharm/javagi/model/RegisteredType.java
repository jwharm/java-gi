package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.util.Objects;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

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
    protected boolean isInstanceOf(String classname) {
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

    public abstract void generate(SourceWriter writer) throws IOException;

    protected void generatePackageDeclaration(SourceWriter writer) throws IOException {
        writer.write("package " + getNamespace().packageName + ";\n");
        writer.write("\n");
    }

    public static void generateImportStatements(SourceWriter writer) throws IOException {
        writer.write("import io.github.jwharm.javagi.*;\n");
        writer.write("import java.lang.foreign.*;\n");
        writer.write("import java.lang.invoke.*;\n");
        writer.write("import org.jetbrains.annotations.*;\n");
        writer.write("\n");
    }

    protected void generateJavadoc(SourceWriter writer) throws IOException {
        if (doc != null) {
            doc.generate(writer, false);
        }
    }
    
    protected void generateCType(SourceWriter writer) throws IOException {
        String typeLiteral = Conversions.literal("java.lang.String", cType);
        writer.write("\n");
        writer.write("private static final java.lang.String C_TYPE_NAME = " + typeLiteral + ";\n");
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
    
    protected void generateMemoryLayout(SourceWriter writer) throws IOException {
        if (this instanceof Bitfield || this instanceof Enumeration) {
            return;
        }

        writer.write("\n");
        writer.write("/**\n");
        if (fieldList.isEmpty()) {
            writer.write(" * Memory layout of the native struct is unknown.\n");
            writer.write(" * @return always {@code Interop.valueLayout.ADDRESS}\n");
        } else {
            writer.write(" * The memory layout of the native struct.\n");
            writer.write(" * @return the memory layout\n");
        }
        writer.write(" */\n");
        writer.write("@ApiStatus.Internal\n");
        writer.write("public static MemoryLayout getMemoryLayout() {\n");
        writer.increaseIndent();

        if (fieldList.isEmpty()) {
            writer.write("return Interop.valueLayout.ADDRESS;\n");
        } else {
            writer.write("return MemoryLayout.");
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
                        int padding = (s - (size % s)) % 64;
                        writer.write("    MemoryLayout.paddingLayout(" + padding + "),\n");
                        size += padding;
                    }

                }

                // Write the memory layout declaration
                writer.write("    " + field.getMemoryLayoutString());

                size += s;
            }
            // Write the name of the struct
            writer.write("\n");
            writer.write(").withName(C_TYPE_NAME);\n");
        }
        writer.decreaseIndent();
        writer.write("}\n");
    }
    
    protected void generateMemoryAddressConstructor(SourceWriter writer) throws IOException {
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Create a " + javaName + " proxy instance for the provided memory address.\n");
        writer.write(" * @param address the memory address of the native object\n");
        writer.write(" */\n");
        writer.write("protected " + javaName + "(Addressable address) {\n");
        writer.write("    super(address);\n");

        // If this class has a custom "unref" method, pass it to the Cleaner.
        for (Method method : methodList) {
            if (method.name.equals("unref")) {
                writer.write("    setRefCleanerMethod(\"" + method.cIdentifier + "\");\n");
                break;
            }
        }

        writer.write("}\n");
    }

    protected void generateMarshal(SourceWriter writer) throws IOException {
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * The marshal function from a native memory address to a Java proxy instance\n");
        writer.write(" */\n");
        writer.write("@ApiStatus.Internal\n");
        String name = javaName + (this instanceof Interface ? "Impl" : "");
        writer.write("public static final Marshal<Addressable, " + name + "> fromAddress = (input, scope) -> "
                + "input.equals(MemoryAddress.NULL) ? null : new " + name + "(input);\n"
        );
    }

    protected void generateIsAvailable(SourceWriter writer) throws IOException {
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Check whether the type is available on the runtime platform.\n");
        writer.write(" * @return {@code true} when the type is available on the runtime platform\n");
        writer.write(" */\n");
        writer.write("public static boolean isAvailable() {\n");
        String targetName = null;
        for (Method m : functionList) {
            if (m.name.equals("get_type") && m.getParameters() == null) {
                targetName = m.cIdentifier;
                break;
            }
        }
        if (targetName == null)
            throw new NullPointerException("Could not find get_type method in " + getNamespace().packageName + "." + javaName);
        writer.write("    return DowncallHandles." + targetName + " != null;\n");
        writer.write("}\n");
    }

    protected void generateEnsureInitialized(SourceWriter writer) throws IOException {
        writer.write("\n");
        writer.write("static {\n");
        writer.write("    " + Conversions.toSimpleJavaType(getNamespace().globalClassName, getNamespace()) + ".javagi$ensureInitialized();\n");
        writer.write("}\n");
    }

    /**
     * Generates all constructors listed for this type. When the constructor is not named "new", a static
     * factory method is generated with the provided name.
     * @param writer The writer for the source code
     * @throws IOException Thrown by {@code writer.write()}
     */
    protected void generateConstructors(SourceWriter writer) throws IOException {
        for (Constructor c : constructorList) {
            if (c.name.equals("new")) {
                c.generate(writer);
            } else {
                c.generateNamed(writer);
            }
        }
    }
    
    /**
     * Generates an inner class DowncallHandles with MethodHandle declarations.
     * @param writer The writer for the source code
     * @throws IOException Thrown by {@code writer.write()}
     */
    protected void generateDowncallHandles(SourceWriter writer) throws IOException {
        boolean isInterface = this instanceof Interface;
        if (! (constructorList.isEmpty() && methodList.isEmpty() && functionList.isEmpty())) {
            writer.write("\n");
            writer.write(isInterface ? "@ApiStatus.Internal\n" : "private ");
            writer.write("static class DowncallHandles {\n");
            writer.increaseIndent();
            for (Constructor c : constructorList) {
                c.generateMethodHandle(writer, isInterface);
            }
            for (Method m : methodList) {
                m.generateMethodHandle(writer, isInterface);
            }
            for (Function f : functionList) {
                f.generateMethodHandle(writer, isInterface);
            }
            writer.decreaseIndent();
            writer.write("}\n");
        }
    }
    
    public String getInteropString(String paramName, boolean isPointer) {
        return paramName + ".handle()";
    }

    protected void generateInjected(SourceWriter writer) throws IOException {
        if (injected != null) writer.write(injected);
    }
}
