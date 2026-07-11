/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2026 the Java-GI developers
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
import java.util.List;
import java.util.stream.Stream;

import org.javagi.javapoet.*;
import org.javagi.configuration.ClassNames;
import org.javagi.gir.*;
import org.javagi.util.GeneratedAnnotationBuilder;
import org.javagi.gir.Class;
import org.javagi.gir.Record;

import javax.lang.model.element.Modifier;

import static java.util.Collections.emptyList;
import static org.javagi.util.Conversions.*;
import static java.util.function.Predicate.not;

/**
 * Generates a Java class for a "record", "union" or "glib:boxed" GIR element.
 */
public class RecordGenerator extends RegisteredTypeGenerator {

    private final StandardLayoutType rec;
    private final RegisteredType outerClass;
    private final TypeSpec.Builder builder;
    private final List<Field> fields;

    public RecordGenerator(StandardLayoutType rec) {
        super(rec);
        this.rec = rec;
        this.outerClass = rec instanceof Record r ? r.isGTypeStructFor() : null;
        this.builder = TypeSpec.classBuilder(rec.typeName());
        this.fields = rec instanceof FieldContainer fc ? fc.fields() : emptyList();

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
            if (fields.isEmpty()) {
                builder.superclass(outerClass instanceof Interface
                        ? ClassNames.G_TYPE_INTERFACE : ClassNames.G_TYPE_CLASS);
            } else {
                /*
                 * parent_class is always the first field, unless the struct is
                 * disguised.
                 */
                Type type = (Type) fields.getFirst().anyType();
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

        builder.addMethod(memoryAddressConstructor());

        if (rec instanceof FieldContainer fc) {
            MemoryLayoutGenerator memoryLayoutGenerator = new MemoryLayoutGenerator();
            builder.addMethod(memoryLayoutGenerator.generateMemoryLayout(fc));

            if (memoryLayoutGenerator.canGenerate(fc)) {
                if (outerClass == null && noNewConstructor()) {
                    builder.addMethod(constructor(true))
                           .addMethod(constructor(false));

                    if (hasFieldSetters())
                        builder.addMethod(constructorWithParameters(true))
                               .addMethod(constructorWithParameters(false));
                }

                for (Field f : fields)
                    generateField(f);

                if (rec instanceof Record r && !r.unions().isEmpty())
                    for (Field f : r.unions().getFirst().fields())
                        generateField(f);
            }
        }

        addConstructors(builder);
        addFunctions(builder);
        addMethods(builder);
        addFreeTextCodeblocks(builder);

        if (hasNativeHandles())
            builder.addType(nativeHandlesClass());

        if (rec.toStringTarget() != null)
            builder.addMethod(toStringRedirect());

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
                    && t.lookup() instanceof StandardLayoutType)
                // Copy contents from nested struct
                builder.addMethod(generator.generateReadCopyMethod());
            else if (f.anyType() instanceof Array a && a.fixedSize() > 0)
                // Copy contents from array
                builder.addMethod(generator.generateReadArrayMethod());
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
                            nullable(java.lang.reflect.Method.class), "_" + generator.getName() + "Method",
                            Modifier.PRIVATE)
                    .build());
            builder.addMethod(generator.generateOverrideMethod());
            builder.addMethod(new ClosureGenerator(cb).generateUpcallMethod(
                    "_" + generator.getName() + "Method",
                    generator.getName() + "Upcall",
                    "this._" + generator.getName() + "Method.invoke",
                    false));
            if (cb.hasLong())
                builder.addMethod(new ClosureGenerator(cb).generateUpcallMethod(
                        "_" + generator.getName() + "Method",
                        generator.getName() + "Upcall_w64",
                        "this._" + generator.getName() + "Method.invoke",
                        true));
        }

        // For other callback fields, generate a functional interface.
        else {
            builder.addType(new ClosureGenerator(cb)
                                    .generateFunctionalInterface());
        }

        if (f.anyType() instanceof Type t
                && (!t.isPointer())
                && t.lookup() instanceof StandardLayoutType)
            // Generate write-method to copy contents to nested struct
            builder.addMethod(generator.generateWriteCopyMethod());
        else if (f.anyType() instanceof Array a && a.fixedSize() > 0)
            // Generate write-method to copy contents to nested array
            builder.addMethod(generator.generateWriteArrayMethod());
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
            spec.addJavadoc("The memory is allocated with {@link $T#ofAuto}.\n", Arena.class)
                .addStatement("super($T.ofAuto().allocate(getMemoryLayout()))", Arena.class);

        return spec.build();
    }

    private Stream<Field> streamAccessibleFields() {
        return fields.stream()
                .filter(not(Field::isDisguised))
                .filter(f -> f.bits() == -1);
    }

    private MethodSpec constructorWithParameters(boolean arenaParameter) {
        var spec = MethodSpec.constructorBuilder()
                .addJavadoc("Allocate a new $T with the fields set to the provided values.\n", rec.typeName())
                .addModifiers(Modifier.PUBLIC);

        if (!arenaParameter)
            spec.addJavadoc("The memory is allocated with {@link $T#ofAuto}.\n", Arena.class);
        spec.addJavadoc("\n");

        streamAccessibleFields().forEach(f ->
                spec.addJavadoc("@param $1L $2L for the field {@code $1L}\n",
                        toJavaIdentifier(f.name()),
                        f.callback() == null ? "value" : "callback function")
                    .addParameter(
                        // Override the type of long values
                        f.anyType() instanceof Type t && t.isLong()
                                ? TypeName.INT
                                : new TypedValueGenerator(f).getAnnotatedType(true),
                        toJavaIdentifier(f.name()))
        );

        // Allocate the new instance
        if (arenaParameter)
            spec.addJavadoc("@param arena to control the memory allocation scope\n")
                .addParameter(Arena.class, "arena")
                .addStatement("this(arena)");
        else
            spec.addStatement("$1T arena = $1T.ofAuto()", Arena.class)
                .addStatement("this(arena)");

        // Copy the parameter values into the instance fields
        streamAccessibleFields().forEach(f -> {
            if (f.allocatesMemory())
                spec.addStatement("$L$L($L, arena)",
                        f.callback() == null ? "write" : "override",
                        toCamelCase(f.name(), true),
                        toJavaIdentifier(f.name()));
            else
                spec.addStatement("$L$L($L)",
                        f.callback() == null ? "write" : "override",
                        toCamelCase(f.name(), true),
                        toJavaIdentifier(f.name()));
        });

        return spec.build();
    }

    private boolean noNewConstructor() {
        List<Constructor> constructors = switch(rec) {
            case Record r -> r.constructors();
            case Union u -> u.constructors();
            case Boxed _ -> emptyList();
        };
        return constructors.stream().noneMatch(c -> c.name().equals("new"));
    }

    private boolean hasFieldSetters() {
        return streamAccessibleFields().anyMatch(f -> f.callback() == null);
    }
}
