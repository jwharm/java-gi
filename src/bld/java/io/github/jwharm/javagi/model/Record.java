package io.github.jwharm.javagi.model;

import java.io.IOException;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

public class Record extends Class {

    public final String disguised, isGTypeStructFor;

    public Record(GirElement parent, String name, String cType, String getType, String version, String disguised, String isGTypeStructFor) {
        super(parent, name, null, cType, null, getType, null, null, null, version, null, null);
        this.disguised = disguised;
        this.isGTypeStructFor = isGTypeStructFor;
    }
    
    /**
     * A record in GI is a struct in C. Java doesn't have a struct type, and the java 'record'
     * functionality has a different purpose. So the generated API creates a class that
     * extends StructProxy (instead of GObject).
     * Structs are often initialized implicitly, which means they don't always have constructors.
     * To solve this, we generate a static allocate() function that allocates a memory segment.
     */
    public void generate(SourceWriter writer) throws IOException {
        if (isGTypeStructFor != null) {
            writer.write("\n");
        } else {
            generatePackageDeclaration(writer);
            generateImportStatements(writer);
        }
        
        generateJavadoc(writer);
        writer.write("public ");
        
        if (isGTypeStructFor != null) {
            writer.write("static ");
        }
        
        writer.write("class " + javaName);
        
        if (generic) {
            writer.write("<T extends org.gnome.gobject.GObject>");
        }

        if (isGTypeStructFor != null) {
            // parent_class is always the first field, unless the struct is disguised
            if (fieldList.isEmpty()) {
                RegisteredType outerClass = module().cTypeLookupTable.get(isGTypeStructFor);
                writer.write(" extends org.gnome.gobject."
                        + (outerClass instanceof Interface ? "TypeInterface" : "TypeClass"));
            } else {
                String parentCType = fieldList.get(0).type.cType;
                Record parentRec = (Record) module().cTypeLookupTable.get(parentCType);
                if (parentRec.isGTypeStructFor != null) {
                    String parentClass = Conversions.toQualifiedJavaType(parentRec.isGTypeStructFor, parentRec.getNamespace());
                    String parentStr = parentClass + "." + parentRec.javaName;
                    writer.write(" extends " + parentStr);
                } else {
                    String parentClass = Conversions.toQualifiedJavaType(parentRec.name, parentRec.getNamespace());
                    writer.write(" extends " + parentClass);
                }
            }
        } else {
            writer.write(" extends ProxyInstance");
        }

        // Floating
        if (isFloating()) {
            writer.write(" implements io.github.jwharm.javagi.base.Floating");
        }

        writer.write(" {\n");
        writer.increaseIndent();

        if (isGTypeStructFor == null) {
            generateEnsureInitialized(writer);
        }
        generateGType(writer);

        // Opaque structs have unknown memory layout and should not have an allocator
        if (! (isOpaqueStruct() || hasOpaqueStructFields())) {
            generateMemoryLayout(writer);
            generateRecordAllocator(writer);
            for (Field f : fieldList) {
                f.generate(writer);
            }
            // Fields can be inside a <union> tag
            if (! unionList.isEmpty()) {
                for (Field f : unionList.get(0).fieldList) {
                    f.generate(writer);
                }
            }
        }

        generateMemoryAddressConstructor(writer);
        generateConstructors(writer);
        generateMethodsAndSignals(writer);
        generateInjected(writer);

        // Generate a custom gtype declaration for GVariant
        if (isInstanceOf("org.gnome.glib.Variant") && "intern".equals(getType)) {
            writer.write("\n");
            writer.write("public static final org.gnome.glib.Type gtype = Types.VARIANT;\n");
        }

        writer.decreaseIndent();
        writer.write("}\n");
    }

    public void generateRecordAllocator(SourceWriter writer) throws IOException {

        // Cache the memory segment
        writer.write("\n");
        writer.write("private MemorySegment allocatedMemorySegment;\n");

        // Accessor function for the memory segment, to enable co-allocation of other segments with the same lifetime
        writer.write("\n");
        writer.write("private MemorySegment getAllocatedMemorySegment() {\n");
        writer.write("    if (allocatedMemorySegment == null) {\n");
        writer.write("        allocatedMemorySegment = MemorySegment.ofAddress(handle().address(), getMemoryLayout().byteSize(), SegmentScope.auto());\n");
        writer.write("    }\n");
        writer.write("    return allocatedMemorySegment;\n");
        writer.write("}\n");

        // Allocator function
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Allocate a new {@link " + javaName + "}. A {@link java.lang.ref.Cleaner} \n");
        writer.write(" * is assigned to the allocated memory segment that will release the \n");
        writer.write(" * memory when the {@link " + javaName + "} instance is garbage-collected.\n");
        writer.write(" * @return A new, uninitialized {@link " + javaName + "}\n");
        writer.write(" */\n");
        writer.write("public static " + javaName + " allocate() {\n");
        writer.write("    MemorySegment segment = SegmentAllocator.nativeAllocator(SegmentScope.auto()).allocate(getMemoryLayout());\n");
        writer.write("    " + javaName + " newInstance = new " + javaName + "(segment);\n");
        writer.write("    newInstance.allocatedMemorySegment = segment;\n");
        writer.write("    return newInstance;\n");
        writer.write("}\n");

        // For regular structs (not typeclasses), generate a second allocator function
        // that takes values for all fields, so it becomes possible to quicly allocate
        // a struct. For example: var color = RGBA.allocate(0.6F, 0.5F, 0.9F, 1.0F);
        if (isGTypeStructFor == null) {

            // First, determine if this struct has field setters
            boolean hasSetters = false;
            for (Field field : fieldList) {
                if (field.disguised()) {
                    continue;
                }
                if (field.callback != null) {
                    continue;
                }
                hasSetters = true;
                break;
            }
            if (! hasSetters) {
                return;
            }

            writer.write("\n");
            writer.write("/**\n");
            writer.write(" * Allocate a new {@link " + javaName + "} with the fields set to the provided values. \n");
            writer.write(" * A {@link java.lang.ref.Cleaner} is assigned to the allocated memory segment that will \n");
            writer.write(" * release the memory when the {@link " + javaName + "} instance is garbage-collected.\n");
            // Write javadoc for parameters
            for (Field field : fieldList) {
                // Ignore disguised fields
                if (field.disguised()) {
                    continue;
                }
                writer.write(" * @param ");
                field.writeName(writer);
                writer.write((field.callback == null ? " value " : " callback function ") + "for the field {@code ");
                field.writeName(writer);
                writer.write("}\n");
            }
            writer.write(" * @return A new {@link " + javaName + "} with the fields set to the provided values\n");
            writer.write(" */\n");
            writer.write("public static " + javaName + " allocate(");

            // Write parameters
            boolean first = true;
            for (Field field : fieldList) {
                // Ignore disguised fields
                if (field.disguised()) {
                    continue;
                }
                if (! first) {
                    writer.write(", ");
                }
                field.writeTypeAndName(writer, false);
                first = false;
            }
            writer.write(") {\n");
            writer.increaseIndent();

            // Call the allocate() method
            writer.write(javaName + " _instance = allocate();\n");

            // Call the field setters
            for (Field field : fieldList) {
                // Ignore disguised fields
                if (field.disguised()) {
                    continue;
                }
                writer.write("_instance." + (field.callback == null ? "write" : "override"));
                writer.write(Conversions.toCamelCase(field.name, true) + "(");
                field.writeName(writer);
                writer.write(");\n");
            }

            // Return the new instance
            writer.write("return _instance;\n");
            writer.decreaseIndent();
            writer.write("}\n");
        }
    }
}
