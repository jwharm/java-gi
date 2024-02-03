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

import com.squareup.javapoet.*;
import io.github.jwharm.javagi.configuration.ClassNames;
import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.gir.Record;

import javax.lang.model.element.Modifier;

import java.lang.foreign.MemorySegment;

public class ClassGenerator extends RegisteredTypeGenerator {

    public final static TypeVariableName GENERIC_T = TypeVariableName.get("T",
            ClassName.get("org.gnome.gobject", "GObject"));

    private final Class cls;
    private final TypeSpec.Builder builder;

    public ClassGenerator(Class cls) {
        super(cls);
        this.cls = cls;
        this.builder = TypeSpec.classBuilder(cls.typeName());
    }

    public TypeSpec generate() {
        if (cls.infoElements().doc() != null) builder.addJavadoc(new DocGenerator(cls.infoElements().doc()).generate());
        if (cls.attrs().deprecated()) builder.addAnnotation(Deprecated.class);

        builder.addModifiers(Modifier.PUBLIC);
        if (cls.abstract_()) builder.addModifiers(Modifier.ABSTRACT);
        if (cls.final_()) builder.addModifiers(Modifier.FINAL);
        if (cls.generic()) builder.addTypeVariable(GENERIC_T);

        Class parentClass = cls.parentClass();
        if (parentClass != null)
            builder.superclass(parentClass.typeName());
        else
            builder.superclass(ClassName.get("org.gnome.gobject", "TypeInstance"));

        for (var impl : cls.implements_())
            builder.addSuperinterface(impl.get().typeName());

        builder.addStaticBlock(staticBlock());

        for (var constant : cls.constants()) {
            var fieldSpec = new TypedValueGenerator(constant).generateConstantDeclaration();
            if (fieldSpec != null) builder.addField(fieldSpec);
        }

        if (hasTypeMethod())
            builder.addMethod(getTypeMethod());
        if (cls.isInstanceOf("GObject", "ParamSpec"))
            builder.addMethod(paramSpecGetTypeMethod());

        MethodSpec memoryLayout = new MemoryLayoutGenerator().generateMemoryLayout(cls);
        if (memoryLayout != null)
            builder.addMethod(memoryLayout);

        builder.addMethod(parentAccessor());
        builder.addMethod(memoryAddressConstructor());

        addConstructors(builder);
        addFunctions(builder);
        addMethods(builder);
        addVirtualMethods(builder);
        addSignals(builder);

        Record typeStruct = cls.typeStruct();
        if (typeStruct != null)
            builder.addType(new RecordGenerator(typeStruct).generate());

        if (cls.abstract_())
            builder.addType(implClass());

        BuilderGenerator generator = new BuilderGenerator(cls);
        builder.addMethod(generator.generateBuilderMethod());
        builder.addType(generator.generateBuilderClass());

        return builder.build();
    }

    private MethodSpec parentAccessor() {
        ClassName className = cls.abstract_()
                ? cls.typeName().nestedClass(cls.name() + "Impl")
                : cls.typeName();

        return MethodSpec.methodBuilder("asParent")
                .addJavadoc("""
                        Returns this instance as if it were its parent type. This is mostly synonymous to the Java
                        {@code super} keyword, but will set the native typeclass function pointers to the parent
                        type. When overriding a native virtual method in Java, "chaining up" with
                        {@code super.methodName()} doesn't work, because it invokes the overridden function pointer
                        again. To chain up, call {@code asParent().methodName()}. This will call the native function
                        pointer of this virtual method in the typeclass of the parent type.
                        """)
                .addModifiers(Modifier.PROTECTED)
                .returns(cls.typeName())
                .addStatement("$T _parent = new $T(handle())", cls.typeName(), className)
                .addStatement("_parent.callParent(true)")
                .addStatement("return _parent")
                .build();
    }

    @Override
    protected MethodSpec memoryAddressConstructor() {
        var spec = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("""
                    Create a $L proxy instance for the provided memory address.
                    @param address the memory address of the native object
                    """, name())
                .addParameter(MemorySegment.class, "address")
                .addStatement("super(address == null ? null : $T.reinterpret(address, getMemoryLayout().byteSize()))",
                        ClassNames.INTEROP);

        for (var m : cls.methods()) {
            if (("free".equals(m.name()) || "unref".equals(m.name()))
                    && m.parameters().instanceParameter() != null
                    && m.parameters().parameters().isEmpty()
                    && (m.returnValue().anyType().isVoid())) {
                spec.addStatement("$T.setFreeFunc(handle(), $S);", ClassNames.MEMORY_CLEANER, m.attrs().cIdentifier());
                break;
            }
        }
        return spec.build();
    }

    protected MethodSpec paramSpecGetTypeMethod() {
        return MethodSpec.methodBuilder("getType")
                .addJavadoc("""
                    Get the GType of the $L class
                    @return always {@link $T.PARAM}
                    """, cls.cType(), ClassNames.TYPES)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get("org.gnome.glib", "Type"))
                .addStatement("return $T.PARAM", ClassNames.TYPES)
                .build();
    }
}
