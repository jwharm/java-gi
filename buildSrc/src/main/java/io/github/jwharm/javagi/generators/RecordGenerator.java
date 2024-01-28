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

import com.squareup.javapoet.*;
import io.github.jwharm.javagi.configuration.ClassNames;
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.gir.Record;

import javax.lang.model.element.Modifier;

import static io.github.jwharm.javagi.generators.ClassGenerator.GENERIC_T;
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
    }

    public TypeSpec generate() {
        if (rec.infoElements().doc() != null) builder.addJavadoc(new DocGenerator(rec.infoElements().doc()).generate());
        if (rec.attrs().deprecated()) builder.addAnnotation(Deprecated.class);

        builder.addModifiers(Modifier.PUBLIC);

        if (outerClass instanceof Class c && c.generic())
            builder.addTypeVariable(GENERIC_T);

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

        // TODO: implement Floating interface

        if (outerClass == null)
            builder.addStaticBlock(staticBlock());

        if (hasTypeMethod())
            builder.addMethod(getTypeMethod());

        builder.addMethod(memoryAddressConstructor());

        MethodSpec memoryLayout = new MemoryLayoutGenerator().generateMemoryLayout(rec);
        if (memoryLayout != null) {
            builder.addMethod(memoryLayout);
            builder.addMethod(allocate());
            if (outerClass == null && hasFieldSetters())
                builder.addMethod(allocateWithParameters());
            for (Field f : rec.fields()) generateField(f);
            if (!rec.unions().isEmpty())
                for (Field f : rec.unions().getFirst().fields()) generateField(f);
        }

        addConstructors(builder);
        addFunctions(builder);

        return builder.build();
    }

    private void generateField(Field f) {
        if (f.isDisguised()) return;
        FieldGenerator generator = new FieldGenerator(f);
        Callback cb  = f.callback();
        if (cb == null) {
            builder.addMethod(generator.generateReadMethod());
        } else {
            builder.addType(new ClosureGenerator(cb).generateFunctionalInterface());
            // For callbacks, generate a second override method with java.lang.reflect.Method parameter
            if (outerClass != null && cb.parameters() != null) {
                builder.addField(FieldSpec.builder(java.lang.reflect.Method.class,
                        "_" + generator.getName() + "Method",
                        Modifier.PRIVATE).build()
                );
                builder.addMethod(generator.generateOverrideMethod());
                builder.addMethod(new ClosureGenerator(cb).generateUpcallMethod(
                        "_" + generator.getName() + "Method",
                        generator.getName() + "Upcall",
                        "this._" + generator.getName() + "Method.invoke"
                ));
            }
        }
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
                        ClassNames.MEMORY_CLEANER, identifier, method.attrs().cIdentifier());
                return;
            }
        }

        // Boxed types
        if (rec.getTypeFunc() != null) {
            builder.addStatement("$T.setFreeFunc($L.handle(), $S)",
                    ClassNames.MEMORY_CLEANER, identifier, "g_boxed_free");
            if (className == null)
                builder.addStatement("$T.setBoxedType($L.handle(), getType())",
                        ClassNames.MEMORY_CLEANER, identifier);
            else
                builder.addStatement("$T.setBoxedType($L.handle(), $T.getType())",
                        ClassNames.MEMORY_CLEANER, identifier, className);
        }
    }

    private MethodSpec allocate() {
        return MethodSpec.methodBuilder("allocate")
                .addJavadoc("""
                        Allocate a new $1T.
                                                
                        @param  arena to control the memory allocation scope
                        @return a new, uninitialized {@link $1T}
                        """, rec.typeName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(rec.typeName())
                .addParameter(Arena.class, "arena")
                .addStatement("$T segment = arena.allocate(getMemoryLayout())", MemorySegment.class)
                .addStatement("return new $T(segment)", rec.typeName())
                .build();
    }

    private boolean hasFieldSetters() {
        return rec.fields().stream().anyMatch(not(f -> f.isDisguised() || f.callback() != null));
    }

    private MethodSpec allocateWithParameters() {
        var spec = MethodSpec.methodBuilder("allocate")
                .addJavadoc("""
                        Allocate a new $T with the fields set to the provided values.
                                                        
                        @param  arena to control the memory allocation scope
                        """, rec.typeName());
        rec.fields().stream().filter(not(Field::isDisguised)).forEach(f ->
                spec.addJavadoc("@param $1L $2L for the field {@code $1L}",
                        toJavaIdentifier(f.name()),
                        f.callback() == null ? "value" : "callback function")
        );
        spec.addJavadoc("@return a new {@link $T} with the fields set to the provided values", rec.typeName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(rec.typeName())
                .addParameter(Arena.class, "arena");
        rec.fields().stream().filter(not(Field::isDisguised)).forEach(f ->
                spec.addParameter(f.anyType().typeName(), toJavaIdentifier(f.name()))
        );
        spec.addStatement("$T _instance = allocate(arena)", rec.typeName());
        rec.fields().stream().filter(not(Field::isDisguised)).forEach(f ->
                spec.addStatement("_instance.$L$L($L, $L)",
                        f.callback() == null ? "write" : "override",
                        toCamelCase(f.name(), true),
                        f.allocatesMemory() ? "arena, " : "",
                        toJavaIdentifier(f.name()))
        );
        return spec.addStatement("return _instance")
                .build();
    }
}
