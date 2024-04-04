/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 the Java-GI developers
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

package io.github.jwharm.javagi.generators;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.stream.Collectors;

import com.squareup.javapoet.*;
import io.github.jwharm.javagi.configuration.ClassNames;
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.GeneratedAnnotationBuilder;

import javax.lang.model.element.Modifier;

import static io.github.jwharm.javagi.util.Conversions.*;
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
            builder.addJavadoc(new DocGenerator(rec.infoElements().doc()).generate());
        if (rec.infoAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        builder.addModifiers(Modifier.PUBLIC);

        if (rec.generic()
                || (outerClass instanceof Class c && c.generic()))
            builder.addTypeVariable(ClassNames.GENERIC_T);

        // TypeClass and TypeInterface records are generated as Java inner classes that
        // extend the TypeClass or TypeInterface of the parent type.
        if (outerClass != null) {
            builder.addModifiers(Modifier.STATIC);
            if (rec.fields().isEmpty()) {
                builder.superclass(outerClass instanceof Interface
                        ? ClassName.get("org.gnome.gobject", "TypeInterface")
                        : ClassName.get("org.gnome.gobject", "TypeClass"));
            } else {
                // parent_class is always the first field, unless the struct is disguised
                Record parentRec = (Record) ((Type) rec.fields().getFirst().anyType()).get();
                RegisteredType parentClass = parentRec.isGTypeStructFor();
                if (parentClass != null) {
                    String nestedClassName = toJavaSimpleType(parentRec.name(), parentRec.namespace());
                    builder.superclass(parentClass.typeName().nestedClass(nestedClassName));
                } else {
                    builder.superclass(parentRec.typeName());
                }
            }
        } else if (List.of("GTypeInstance", "GTypeClass", "GTypeInterface").contains(rec.cType())) {
            builder.superclass(ClassNames.PROXY_INSTANCE);
        } else {
            builder.superclass(ClassNames.MANAGED_INSTANCE);
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

        MethodSpec memoryLayout = new MemoryLayoutGenerator().generateMemoryLayout(rec);
        if (memoryLayout != null) {
            builder.addMethod(memoryLayout);

            if (noNewConstructor())
                builder.addMethod(constructor(true))
                       .addMethod(constructor(false))
                       .addMethod(allocate());

            if (outerClass == null && hasFieldSetters() && noNewConstructor())
                builder.addMethod(constructorWithParameters(true))
                       .addMethod(constructorWithParameters(false))
                       .addMethod(allocateWithParameters());

            for (Field f : rec.fields())
                generateField(f);

            if (!rec.unions().isEmpty())
                for (Field f : rec.unions().getFirst().fields())
                    generateField(f);
        }

        addConstructors(builder);
        addFunctions(builder);
        addMethods(builder);

        if ("GTypeInstance".equals(rec.cType()))
            addCallParentMethods();

        if ("GValue".equals(rec.cType()))
            builder.addMethod(gvalueToString());

        return builder.build();
    }

    private void generateField(Field f) {
        if (f.isDisguised()) return;
        FieldGenerator generator = new FieldGenerator(f);
        Callback cb  = f.callback();

        if (cb == null) {
            if (f.anyType() instanceof Type t && (!t.isPointer()) && t.get() instanceof Record)
                // Copy contents from nested struct
                builder.addMethod(generator.generateReadCopyMethod());
            else
                // Read pointer or primitive value
                builder.addMethod(generator.generateReadMethod());

        } else {
            builder.addType(new ClosureGenerator(cb).generateFunctionalInterface());

            // For callbacks, generate a second override method
            // with java.lang.reflect.Method parameter
            if (outerClass != null && cb.parameters() != null) {
                builder.addField(FieldSpec.builder(
                            java.lang.reflect.Method.class,
                            "_" + generator.getName() + "Method",
                            Modifier.PRIVATE)
                        .build()
                );
                builder.addMethod(generator.generateOverrideMethod());
                builder.addMethod(new ClosureGenerator(cb).generateUpcallMethod(
                        "_" + generator.getName() + "Method",
                        generator.getName() + "Upcall",
                        "this._" + generator.getName() + "Method.invoke"
                ));
            }
        }

        if (f.anyType() instanceof Type t && (!t.isPointer()) && t.get() instanceof Record)
            // Copy contents to nested struct
            builder.addMethod(generator.generateWriteCopyMethod());
        else
            // Write pointer or primitive value
            builder.addMethod(generator.generateWriteMethod());
    }

    public void setFreeFunc(MethodSpec.Builder builder, String identifier, TypeName className) {
        if (List.of("GTypeInstance", "GTypeClass", "GTypeInterface").contains(rec.cType()))
            return;

        if (rec.foreign())
            return;

        // Look for instance methods named "free()" and "unref()"
        for (Method method : rec.methods()) {
            if (List.of("free", "unref").contains(method.name())
                    && method.parameters() == null
                    && (method.returnValue().anyType().isVoid())) {
                builder.addStatement("$T.setFreeFunc(%L.handle(), %S)",
                        ClassNames.MEMORY_CLEANER,
                        identifier,
                        method.callableAttrs().cIdentifier());
                return;
            }
        }

        // Boxed types
        if (rec.getTypeFunc() != null) {
            builder.addStatement("$T.setFreeFunc($L.handle(), $S)",
                    ClassNames.MEMORY_CLEANER,
                    identifier,
                    "g_boxed_free");
            if (className == null)
                builder.addStatement("$T.setBoxedType($L.handle(), getType())",
                        ClassNames.MEMORY_CLEANER,
                        identifier);
            else
                builder.addStatement("$T.setBoxedType($L.handle(), $T.getType())",
                        ClassNames.MEMORY_CLEANER,
                        identifier,
                        className);
        }
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

        return spec.build();
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

        rec.fields().stream().filter(not(Field::isDisguised)).forEach(f ->
                spec.addJavadoc("@param $1L $2L for the field {@code $1L}\n",
                        toJavaIdentifier(f.name()),
                        f.callback() == null ? "value" : "callback function")
                    .addParameter(
                        new TypedValueGenerator(f).getType(),
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
        rec.fields().stream().filter(not(Field::isDisguised)).forEach(f -> {
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

        return spec.build();
    }

    private MethodSpec allocate() {
        return MethodSpec.methodBuilder("allocate")
                .addJavadoc("""
                        Allocate a new $1T.
                        
                        @param  arena to control the memory allocation scope
                        @return a new, uninitialized {@link $1T}
                        @deprecated Replaced by {@link $1T#$1T($2T)}
                        """, rec.typeName(), Arena.class)
                .addAnnotation(Deprecated.class)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(rec.typeName())
                .addParameter(Arena.class, "arena")
                .addStatement("$T segment = arena.allocate(getMemoryLayout())",
                        MemorySegment.class)
                .addStatement("return new $T(segment)",
                        rec.typeName())
                .build();
    }

    private boolean noNewConstructor() {
        return rec.constructors().stream()
                .noneMatch(c -> c.name().equals("new"));
    }

    private boolean hasFieldSetters() {
        return rec.fields().stream()
                .anyMatch(not(f -> f.isDisguised() || f.callback() != null));
    }

    private MethodSpec allocateWithParameters() {
        var spec = MethodSpec.methodBuilder("allocate")
                .addJavadoc("""
                        Allocate a new $T with the fields set to the provided values.
                        
                        @param  arena to control the memory allocation scope
                        """, rec.typeName());

        // Javadoc for parameters and return value
        rec.fields().stream().filter(not(Field::isDisguised)).forEach(f ->
                spec.addJavadoc("@param  $1L $2L for the field {@code $1L}\n",
                        toJavaIdentifier(f.name()),
                        f.callback() == null ? "value" : "callback function")
        );
        spec.addJavadoc("@return a new {@link $T} with the fields set to the provided values\n",
                        rec.typeName());

        String paramTypes = rec.fields().stream()
                .filter(not(Field::isDisguised))
                .map(f -> new TypedValueGenerator(f).getType().toString() + ", ")
                .collect(Collectors.joining());
        spec.addJavadoc("@deprecated Replaced by {@link $1T#$1T($2L$3T)}\n",
                        rec.typeName(), paramTypes, Arena.class)
                        .addAnnotation(Deprecated.class);

        // Set visibility, return type, and parameters
        spec.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(rec.typeName())
                .addParameter(Arena.class, "arena");
        rec.fields().stream().filter(not(Field::isDisguised)).forEach(f ->
                spec.addParameter(
                        new TypedValueGenerator(f).getType(),
                        toJavaIdentifier(f.name()))
        );

        String params = rec.fields().stream()
                .filter(not(Field::isDisguised))
                .map(f -> toJavaIdentifier(f.name()) + ", ")
                .collect(Collectors.joining());

        return spec.addStatement("return new $T($Larena)", rec.typeName(), params)
                .build();
    }

    private MethodSpec gvariantGetType() {
        ClassName G_TYPE = ClassName.get("org.gnome.glib", "Type");
        return MethodSpec.methodBuilder("getType")
                .addJavadoc("""
                    Get the GType of the GVariant class
                    
                    @return the GType
                    """)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(G_TYPE)
                // Types.VARIANT is declared in GObject. Hard-coded value as workaround
                .addStatement("return new $T(21L << 2)", G_TYPE)
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

    private MethodSpec gvalueToString() {
        return MethodSpec.methodBuilder("toString")
                .addJavadoc("""
                                Return a newly allocated String using {@link $1T#strdupValueContents($2T)},
                                which describes the contents of a {@link $2T}.
                                The main purpose of this function is to describe {@link $2T}
                                contents for debugging output, the way in which the contents are
                                described may change between different GLib versions.
                                                        
                                @return the newly allocated String.
                                """,
                        ClassName.get("org.gnome.gobject", "GObjects"),
                        ClassName.get("org.gnome.gobject", "Value"))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(String.class)
                .addStatement("return $T.strdupValueContents(this)",
                        ClassName.get("org.gnome.gobject", "GObjects"))
                .build();
    }
}
