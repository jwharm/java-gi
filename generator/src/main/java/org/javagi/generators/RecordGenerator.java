/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 the Java-GI developers
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

package org.javagi.generators;

import java.lang.foreign.Arena;
import java.util.stream.Stream;

import com.squareup.javapoet.*;
import org.javagi.configuration.ClassNames;
import org.javagi.gir.*;
import org.javagi.util.GeneratedAnnotationBuilder;
import org.javagi.util.Platform;
import org.javagi.gir.Class;
import org.javagi.gir.Record;

import javax.lang.model.element.Modifier;

import static org.javagi.util.Conversions.*;
import static java.util.function.Predicate.not;

public class RecordGenerator extends RegisteredTypeGenerator {

    private final Record rec;
    private final RegisteredType outerClass;
    private final TypeSpec.Builder builder;

    public RecordGenerator(Record rec) {
        super(rec);
        this.rec = rec;
        this.outerClass = rec.isGTypeStructFor();
        this.builder = TypeSpec.classBuilder(rec.typeName());

        if (outerClass == null)
            this.builder.addAnnotation(GeneratedAnnotationBuilder.generate());
    }

    public TypeSpec generate() {
        if (rec.infoElements().doc() != null)
            builder.addJavadoc(
                    new DocGenerator(rec.infoElements().doc()).generate());
        if (rec.infoAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        builder.addModifiers(Modifier.PUBLIC);

        if (rec.generic()
                || (outerClass instanceof Class c && c.generic()))
            builder.addTypeVariable(ClassNames.GENERIC_T);

        /*
         * TypeClass and TypeInterface records are generated as Java inner
         * classes that extend the TypeClass or TypeInterface of the parent
         * type.
         */
        if (outerClass != null) {
            builder.addModifiers(Modifier.STATIC);
            if (rec.fields().isEmpty()) {
                builder.superclass(outerClass instanceof Interface
                        ? ClassNames.G_TYPE_INTERFACE : ClassNames.G_TYPE_CLASS);
            } else {
                /*
                 * parent_class is always the first field, unless the struct is
                 * disguised.
                 */
                Type type = (Type) rec.fields().getFirst().anyType();
                Record parentRec = (Record) type.lookup();
                RegisteredType parentClass = parentRec.isGTypeStructFor();
                if (parentClass != null) {
                    String nestedClassName = toJavaSimpleType(parentRec.name(), parentRec.namespace());
                    builder.superclass(parentClass.typeName().nestedClass(nestedClassName));
                } else {
                    builder.superclass(parentRec.typeName());
                }
            }
        } else {
            builder.superclass(ClassNames.PROXY_INSTANCE);
        }

        if (rec.isFloating())
            builder.addSuperinterface(ClassNames.FLOATING);

        if (outerClass == null)
            builder.addStaticBlock(staticBlock());

        if (hasTypeMethod())
            builder.addMethod(getTypeMethod());

        if (rec.cType().equals("GVariant"))
            builder.addMethod(gvariantGetType());

        builder.addMethod(memoryAddressConstructor());

        MethodSpec memoryLayout = new MemoryLayoutGenerator()
                                            .generateMemoryLayout(rec);
        if (memoryLayout != null) {
            builder.addMethod(memoryLayout);

            if (noNewConstructor())
                builder.addMethod(constructor(true))
                       .addMethod(constructor(false));

            if (outerClass == null && hasFieldSetters() && noNewConstructor())
                builder.addMethod(constructorWithParameters(true))
                       .addMethod(constructorWithParameters(false));

            for (Field f : rec.fields())
                generateField(f);

            if (!rec.unions().isEmpty())
                for (Field f : rec.unions().getFirst().fields())
                    generateField(f);
        }

        addConstructors(builder);
        addFunctions(builder);
        addMethods(builder);

        if (hasDowncallHandles())
            builder.addType(downcallHandlesClass());

        if (rec.toStringTarget() != null)
            builder.addMethod(toStringRedirect());

        if ("GTypeInstance".equals(rec.cType()))
            addCallParentMethods();

        if ("GVariant".equals(rec.cType()))
            builder.addMethod(gvariantPack())
                    .addMethod(gvariantUnpack())
                    .addMethod(gvariantUnpackRecursive());

        return builder.build();
    }

    private void generateField(Field f) {
        if (f.isDisguised()) return;
        if (f.bits() != -1) return;
        FieldGenerator generator = new FieldGenerator(f);
        Callback cb = f.callback();

        // Generate read-method
        if (cb == null) {
            if (f.anyType() instanceof Type t
                    && (!t.isPointer())
                    && t.lookup() instanceof Record)
                // Copy contents from nested struct
                builder.addMethod(generator.generateReadCopyMethod());
            else
                // Read pointer or primitive value
                builder.addMethod(generator.generateReadMethod());
        }

        /*
         * For vfunc callbacks (in GObject type classes), generate an override
         * method with java.lang.reflect.Method parameter.
         *
         * Check for "cb.parameters() != null" because some callbacks are
         * incompletely specified in the gir data. Example: Gio FileIface
         * callback field "_query_settable_attributes_async"
         */
        else if (outerClass != null && cb.parameters() != null) {
            builder.addField(FieldSpec.builder(
                            java.lang.reflect.Method.class,
                            "_" + generator.getName() + "Method",
                            Modifier.PRIVATE)
                    .build());
            builder.addMethod(generator.generateOverrideMethod());
            builder.addMethod(new ClosureGenerator(cb).generateUpcallMethod(
                    "_" + generator.getName() + "Method",
                    generator.getName() + "Upcall",
                    "this._" + generator.getName() + "Method.invoke"));
        }

        // For other callback fields, generate a functional interface.
        else {
            builder.addType(new ClosureGenerator(cb)
                                    .generateFunctionalInterface());
        }

        if (f.anyType() instanceof Type t
                && (!t.isPointer())
                && t.lookup() instanceof Record)
            // Generate write-method to copy contents to nested struct
            builder.addMethod(generator.generateWriteCopyMethod());
        else if (cb == null || outerClass == null)
            // Generate write/override method for a pointer or primitive value
            builder.addMethod(generator.generateWriteMethod());
    }

    private MethodSpec constructor(boolean arenaParameter) {
        var spec = MethodSpec.constructorBuilder()
                .addJavadoc("Allocate a new $1T.\n", rec.typeName())
                .addModifiers(Modifier.PUBLIC);

        if (arenaParameter)
            spec.addJavadoc("\n@param arena to control the memory allocation scope\n")
                .addParameter(Arena.class, "arena")
                .addStatement("super(arena.allocate(getMemoryLayout()))");
        else
            spec.addJavadoc("The memory is allocated with {@link $T#ofAuto}.\n",
                            Arena.class)
                .addStatement("super($T.ofAuto().allocate(getMemoryLayout()))",
                    Arena.class);

        // Platform check
        if (rec.doPlatformCheck())
            spec.addStatement(Platform.generateSupportCheck(rec.platforms()));

        return spec.build();
    }

    private Stream<Field> streamAccessibleFields() {
        return rec.fields().stream()
                .filter(not(Field::isDisguised))
                .filter(f -> f.bits() == -1);
    }

    private MethodSpec constructorWithParameters(boolean arenaParameter) {
        var spec = MethodSpec.constructorBuilder()
                .addJavadoc("Allocate a new $T with the fields set to the provided values.\n",
                        rec.typeName())
                .addModifiers(Modifier.PUBLIC);

        if (!arenaParameter)
            spec.addJavadoc("The memory is allocated with {@link $T#ofAuto}.\n",
                    Arena.class);
        spec.addJavadoc("\n");

        streamAccessibleFields().forEach(f ->
                spec.addJavadoc("@param $1L $2L for the field {@code $1L}\n",
                        toJavaIdentifier(f.name()),
                        f.callback() == null ? "value" : "callback function")
                    .addParameter(
                        // Override the type of long values
                        f.anyType() instanceof Type t && t.isLong()
                                ? TypeName.INT
                                : new TypedValueGenerator(f).getType(),
                        toJavaIdentifier(f.name()))
        );

        if (arenaParameter)
            spec.addJavadoc("@param arena to control the memory allocation scope\n")
                .addParameter(Arena.class, "arena");

        // Allocate the new instance
        if (arenaParameter)
            spec.addStatement("this(arena)");
        else
            spec.addStatement("this($T.ofAuto())", Arena.class);

        // Copy the parameter values into the instance fields
        streamAccessibleFields().forEach(f -> {
            if (f.allocatesMemory() && arenaParameter)
                spec.addStatement("$L$L($L, arena)",
                        f.callback() == null ? "write" : "override",
                        toCamelCase(f.name(), true),
                        toJavaIdentifier(f.name()));
            else if (f.allocatesMemory())
                spec.addStatement("$L$L($L, $T.ofAuto())",
                        f.callback() == null ? "write" : "override",
                        toCamelCase(f.name(), true),
                        toJavaIdentifier(f.name()),
                        Arena.class);
            else
                spec.addStatement("$L$L($L)",
                        f.callback() == null ? "write" : "override",
                        toCamelCase(f.name(), true),
                        toJavaIdentifier(f.name()));
        });

        // Platform check
        if (rec.doPlatformCheck())
            spec.addStatement(Platform.generateSupportCheck(rec.platforms()));

        return spec.build();
    }

    private boolean noNewConstructor() {
        return rec.constructors().stream()
                .noneMatch(c -> c.name().equals("new"));
    }

    private boolean hasFieldSetters() {
        return streamAccessibleFields().anyMatch(f -> f.callback() == null);
    }

    private MethodSpec gvariantGetType() {
        return MethodSpec.methodBuilder("getType")
                .addJavadoc("""
                    Get the GType of the GVariant class
                    
                    @return the GType
                    """)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassNames.G_TYPE)
                // Types.VARIANT is declared in GObject. Hard-coded value as workaround
                .addStatement("return new $T(21L << 2)", ClassNames.G_TYPE)
                .build();
    }

    private void addCallParentMethods() {
        builder.addField(FieldSpec.builder(boolean.class, "callParent")
                .addModifiers(Modifier.PRIVATE)
                .initializer("false")
                .build());

        builder.addMethod(MethodSpec.methodBuilder("callParent")
                .addJavadoc("""
                        Set the flag that determines if for virtual method calls,
                        {@code g_type_class_peek_parent()} is used to obtain the function pointer of the
                        parent type instead of the instance class.
                        
                        @param callParent true to call the parent vfunc instead of an overridden vfunc
                        """)
                .addModifiers(Modifier.PROTECTED)
                .addParameter(boolean.class, "callParent")
                .addStatement("this.callParent = callParent")
                .build());

        builder.addMethod(MethodSpec.methodBuilder("callParent")
                .addJavadoc("""
                        Returns the flag that determines if for virtual method calls,
                        {@code g_type_class_peek_parent()} is used to obtain the function pointer of the
                        parent type instead of the instance class.
                        
                        @return true when parent vfunc is called instead of an overridden vfunc, or
                                false when the overridden vfunc of the instance is called.
                        """)
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addStatement("return this.callParent")
                .build());
    }

    private MethodSpec gvariantPack() {
        return MethodSpec.methodBuilder("pack")
                .addJavadoc("""
                        Create a GVariant from a Java Object.
                        
                        @param o the Java Object to pack into a GVariant
                        @return the GVariant with the packed Object
                        @see Variants#pack
                        """)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassNames.G_VARIANT)
                .addParameter(Object.class, "object")
                .addStatement("return $T.pack(object)", ClassNames.VARIANTS)
                .build();
    }

    private MethodSpec gvariantUnpack() {
        return MethodSpec.methodBuilder("unpack")
                .addJavadoc("""
                        Unpack a GVariant into a Java Object.
                        
                        @return the unpacked Java Object
                        @see Variants#unpack
                        """)
                .addModifiers(Modifier.PUBLIC)
                .returns(Object.class)
                .addStatement("return $T.unpack(this, false)", ClassNames.VARIANTS)
                .build();
    }

    private MethodSpec gvariantUnpackRecursive() {
        return MethodSpec.methodBuilder("unpackRecursive")
                .addJavadoc("""
                        Unpack a GVariant into a Java Object. Nested GVariants are recursively unpacked.
                        
                        @return the unpacked Java Object
                        @see Variants#unpack
                        """)
                .addModifiers(Modifier.PUBLIC)
                .returns(Object.class)
                .addStatement("return $T.unpack(this, true)", ClassNames.VARIANTS)
                .build();
    }
}
