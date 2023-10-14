/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2023 Jan-Willem Harmannij
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

/**
 * Base class for all GIR elements that generate a Java class. Most
 * functionality that is common for generating a class declaration is
 * implemented here.
 */
public abstract class RegisteredType extends GirElement {

    public final String javaName;
    public final String parentClass;
    public final String cType;
    public String getType;
    public final String version;
    public final String qualifiedName;

    public boolean generic = false;
    public boolean autoCloseable = false;
    public String injected = null;

    public RegisteredType(GirElement parent, String name, String parentClass, String cType, String getType, String version) {
        super(parent);
        
        this.parentClass = Conversions.toQualifiedJavaType(parentClass, getNamespace());
        this.name = name;
        this.javaName = Conversions.toSimpleJavaType(name, getNamespace());
        this.qualifiedName = Conversions.toQualifiedJavaType(name, getNamespace());
        
        // If c type is not provided, guess that the name is also the c type
        this.cType = Objects.requireNonNullElse(cType, name);

        this.getType = getType;
        this.version = version;

        // Register the full names of this class and the parent class
        module().superLookupTable.put(this.qualifiedName, this.parentClass);

        // Register globally-declared types
        if (parent instanceof Namespace ns) {
            ns.registeredTypeMap.put(this.name, this);
        }
    }

    // Find out if this tyjpe is a subclass of the provided classname
    protected boolean isInstanceOf(String classname) {
        if (this.qualifiedName.equals(classname)) {
            return true;
        }
        String current = this.qualifiedName;
        while (current != null) {
            current = module().superLookupTable.get(current);
            if (classname.equals(current)) {
                return true;
            }
        }
        return false;
    }

    public boolean isFloating() {
        // GObject has a ref_sink function but we don't want to treat all GObjects as floating references.
        if ("GObject".equals(javaName) && "org.gnome.gobject".equals(getNamespace().packageName)) {
            return false;
        }
        // GInitiallyUnowned is always a floating reference, and doesn't explicitly need to be marked as such.
        if ("InitiallyUnowned".equals(javaName) && "org.gnome.gobject".equals(getNamespace().packageName)) {
            return false;
        }
        // Subclasses of GInitiallyUnowned don't need to implement the `Floating` interface,
        // because GInitiallyUnowned already does.
        if (isInstanceOf("org.gnome.gobject.InitiallyUnowned")) {
            return false;
        }
        // Any other classes that have a ref_sink method, will be treated as floating references.
        for (Method method : methodList) {
            if ("ref_sink".equals(method.name)) {
                return true;
            }
        }
        return false;
    }

    public abstract void generate(SourceWriter writer) throws IOException;

    public void generateCopyrightNoticeAndPackageDeclaration(SourceWriter writer) throws IOException {
        ((Repository) getNamespace().parent).generateCopyrightNoticeAndPackageDeclaration(writer);
    }

    public void generateImportStatements(SourceWriter writer) throws IOException {
        ((Repository) getNamespace().parent).generateImportStatements(writer);
    }

    protected void generateJavadoc(SourceWriter writer) throws IOException {
        if (doc != null) {
            doc.generate(writer, false);
        }
    }
    
    /**
     * Generate a static method declaration to retrieve the gtype of this object.
     */
    protected void generateGType(SourceWriter writer) throws IOException {
        if (getType == null || "intern".equals(getType)) {
            return;
        }
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Get the GType of the " + cType + " " + (this instanceof Interface ? "interface" : "class") + ".\n");
        writer.write(" * @return the GType\n");
        writer.write(" */\n");
        writer.write("public static org.gnome.glib.Type getType() {\n");
        writer.write("    return Interop.getType(\"" + getType + "\");\n");
        writer.write("}\n");
    }

    public String getConstructorString() {
        String qName = Conversions.convertToJavaType(this.javaName, true, getNamespace());
        return qName + "::new";
    }

    /**
     * Opaque structs have unknown memory layout and should not have an allocator
     * @return true if the struct has no fields specified in the GIR file
     */
    public boolean isOpaqueStruct() {
        return fieldList.isEmpty() && unionList.isEmpty();
    }

    /**
     * If one of the fields directly refers to an opaque struct (recursively), we cannot
     * generate the memory layout or allocate memory for this type
     * @return whether on of the fields refers (recursively) to an opaque struct
     */
    public boolean hasOpaqueStructFields() {
        for (Field field : fieldList) {
            if (field.type != null
                    && (! field.type.isPointer())
                    && field.type.girElementInstance instanceof Class cls
                    && (cls.isOpaqueStruct() || cls.hasOpaqueStructFields())) {
                return true;
            }
        }
        return false;
    }
    
    protected void generateMemoryLayout(SourceWriter writer) throws IOException {
        if (this instanceof Bitfield || this instanceof Enumeration) {
            return;
        }

        // Opaque structs have unknown memory layout and should not have an allocator
        if (hasOpaqueStructFields() || (this instanceof Class cls && cls.isOpaqueStruct())) {
            return;
        }

        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * The memory layout of the native struct.\n");
        writer.write(" * @return the memory layout\n");
        writer.write(" */\n");
        writer.write("public static MemoryLayout getMemoryLayout() {\n");
        writer.increaseIndent();

        // Check if this type is either defined as a union, or has a union element
        boolean isUnion = this instanceof Union || (! unionList.isEmpty());

        writer.write("return MemoryLayout.");
        if (isUnion) {
            writer.write("unionLayout(\n");
        } else {
            writer.write("structLayout(\n");
        }

        List<Field> fieldList = this.fieldList;
        if (fieldList.isEmpty() && unionList.size() > 0 && unionList.get(0).fieldList.size() > 0)
            fieldList = unionList.get(0).fieldList;

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
            if (! (isUnion)) {

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
        writer.write(").withName(" + Conversions.literal("java.lang.String", cType) + ");\n");
        writer.decreaseIndent();
        writer.write("}\n");
    }
    
    protected void generateMemoryAddressConstructor(SourceWriter writer) throws IOException {
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Create a " + javaName + " proxy instance for the provided memory address.\n");
        writer.write(" * @param address the memory address of the native object\n");
        writer.write(" */\n");
        writer.write("public " + javaName + "(MemorySegment address) {\n");
        writer.increaseIndent();
        writer.write("super(address);\n");
        generateSetFreeFunc(writer, "this", null);
        writer.decreaseIndent();
        writer.write("}\n");
    }

    public void generateSetFreeFunc(SourceWriter writer, String identifier, String classname) throws IOException {
        if (! (this instanceof Record || this instanceof Union)) {
            return;
        }
        if ("GTypeInstance".equals(this.cType) || "GTypeClass".equals(this.cType) || "GTypeInterface".equals(this.cType)) {
            return;
        }
        if (this instanceof Record rec && "1".equals(rec.foreign)) {
            return;
        }

        // Look for instance methods named "free()" and "unref()"
        for (Method method : methodList) {
            if (("free".equals(method.name) || "unref".equals(method.name))
                    && method.parameters.parameterList.size() == 1
                    && method.parameters.parameterList.get(0) instanceof InstanceParameter
                    && (method.returnValue.type == null || method.returnValue.type.isVoid())) {

                String cIdentifier = Conversions.literal("java.lang.String", method.cIdentifier);
                writer.write("MemoryCleaner.setFreeFunc(" + identifier + ".handle(), " + cIdentifier + ");\n");
                return;
            }
        }

        if (this instanceof Record && getType != null) {
            writer.write("MemoryCleaner.setFreeFunc(" + identifier + ".handle(), \"g_boxed_free\");\n");
            writer.write("MemoryCleaner.setBoxedType(" + identifier + ".handle(), ");
            if (classname != null) {
                writer.write(classname + ".getType()");
            } else {
                writer.write("getType()");
            }
            writer.write(");\n");
        }
    }

    protected void generateMethodsAndSignals(SourceWriter writer) throws IOException {
        HashSet<String> generatedMethods = new HashSet<>();
        
        // First, generate all virtual methods
        for (VirtualMethod vm : virtualMethodList) {
            vm.generate(writer);
            generatedMethods.add(vm.name + " " + vm.getMethodSpecification());
        }

        // Next, generate the non-virtual instance methods
        for (Method m : methodList) {
            if (! generatedMethods.contains(m.name + " " + m.getMethodSpecification())) {
                m.generate(writer);
                generatedMethods.add(m.name + " " + m.getMethodSpecification());
            }
        }

        // Generate all functions as static methods
        for (Function function : functionList) {
            function.generate(writer);
        }

        // Generate signals: functional interface, onSignal method and emitSignal method
        for (Signal s : signalList) {
            s.generate(writer);
        }
    }

    protected void generateIsAvailable(SourceWriter writer) throws IOException {
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Check whether the type is available on the runtime platform.\n");
        writer.write(" * @return {@code true} when the type is available on the runtime platform\n");
        writer.write(" */\n");
        writer.write("public static boolean isAvailable() {\n");
        writer.write("    return Interop.isAvailable(\"" + getType + "\", FunctionDescriptor.of(ValueLayout.JAVA_LONG), false);\n");
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
    
    public void generateImplClass(SourceWriter writer) throws IOException {
        writer.write("\n");

        if (this instanceof Interface) {
            writer.write("/**\n");
            writer.write(" * The " + javaName + "Impl type represents a native instance of the " + javaName + " interface.\n");
            writer.write(" */\n");
            writer.write("class " + javaName + "Impl extends org.gnome.gobject.GObject implements " + javaName + " {\n");
        } else if (this instanceof Class) {
            writer.write("/**\n");
            writer.write(" * The " + javaName + "Impl type represents a native instance of the abstract " + javaName + " class.\n");
            writer.write(" */\n");
            writer.write("public static class " + javaName + "Impl extends " + javaName + " {\n");
        }
        writer.increaseIndent();

        generateEnsureInitialized(writer);

        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Creates a new instance of " + javaName + " for the provided memory address.\n");
        writer.write(" * @param address the memory address of the instance\n");
        writer.write(" */\n");
        writer.write("public " + javaName + "Impl(MemorySegment address) {\n");
        writer.write("    super(address);\n");
        writer.write("}\n");

        writer.decreaseIndent();
        writer.write("}\n");
    }

    public String getInteropString(String paramName, boolean isPointer) {
        return paramName + ".handle()";
    }

    protected void generateInjected(SourceWriter writer) throws IOException {
        if (injected != null) writer.write(injected);
    }
}
