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

import org.javagi.javapoet.*;
import org.javagi.configuration.ClassNames;
import org.javagi.gir.Class;
import org.javagi.gir.Interface;
import org.javagi.gir.Record;
import org.javagi.util.GeneratedAnnotationBuilder;

import javax.lang.model.element.Modifier;

import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.Collection;

public class ClassGenerator extends RegisteredTypeGenerator {

    private final Class cls;
    private final TypeSpec.Builder builder;

    public ClassGenerator(Class cls) {
        super(cls);
        this.cls = cls;
        this.builder = TypeSpec.classBuilder(cls.typeName());
        this.builder.addAnnotation(GeneratedAnnotationBuilder.generate());
    }

    public TypeSpec generate() {
        if (cls.infoElements().doc() != null)
            builder.addJavadoc(
                    new DocGenerator(cls.infoElements().doc()).generate());
        if (cls.infoAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        builder.addModifiers(Modifier.PUBLIC);
        if (cls.abstract_()) builder.addModifiers(Modifier.ABSTRACT);
        if (cls.final_()) builder.addModifiers(Modifier.FINAL);
        if (cls.generic()) builder.addTypeVariable(ClassNames.GENERIC_T);
        TypeName actualGeneric = genericActualTypeName();

        Class parentClass = cls.parentClass();
        if (parentClass != null)
            builder.superclass(parentClass.generic()
                    ? ParameterizedTypeName.get(parentClass.typeName(),
                    actualGeneric)
                    : parentClass.typeName());
        else
            builder.superclass(ClassNames.G_TYPE_INSTANCE);

        // Add "implements" clause for all implemented interfaces.
        // For generic interfaces, add "<GObject>" generic type.
        for (var impl : cls.implements_())
            if (impl.lookup() instanceof Interface iface)
                builder.addSuperinterface(iface.generic()
                        ? ParameterizedTypeName.get(iface.typeName(), actualGeneric)
                        : iface.typeName());

        if (cls.autoCloseable())
            builder.addSuperinterface(ClassNames.AUTO_CLOSEABLE);

        if (cls.mutableList()) {
            builder.addSuperinterface(ParameterizedTypeName.get(
                    ClassNames.LIST_MODEL_JAVA_LIST_MUTABLE, actualGeneric));
            if (actualGeneric.equals(ClassNames.STRING_OBJECT))
                builder.addMethod(appendStringObjectUnwrapper());
        }

        if (cls.spliceableList()) {
            if (actualGeneric instanceof TypeVariableName)
                throw new IllegalArgumentException("actualGeneric is a TypeVariableName");

            builder.addSuperinterface(ParameterizedTypeName.get(
                    ClassNames.LIST_MODEL_JAVA_LIST_SPLICEABLE, actualGeneric));
            builder.addMethod(spliceCollectionWrapper(actualGeneric));
            if (actualGeneric.equals(ClassNames.STRING_OBJECT)) {
                builder.addMethod(spliceStringObjectUnwrapper())
                       .addMethod(appendStringObjectUnwrapper());
            }
        }

        if (cls.isFloating())
            builder.addSuperinterface(ClassNames.FLOATING);

        builder.addStaticBlock(staticBlock());

        for (var constant : cls.constants()) {
            var fieldSpec = new TypedValueGenerator(constant)
                    .generateConstantDeclaration();
            if (fieldSpec != null) builder.addField(fieldSpec);
        }

        if (hasTypeMethod())
            builder.addMethod(getTypeMethod());

        builder.addMethod(new MemoryLayoutGenerator().generateMemoryLayout(cls));
        builder.addMethod(parentAccessor());
        builder.addMethod(memoryAddressConstructor());

        addConstructors(builder);
        addFunctions(builder);
        addMethods(builder);
        addVirtualMethods(builder);
        addSignals(builder);
        addFreeTextCodeblocks(builder);

        if (cls.toStringTarget() != null)
            builder.addMethod(toStringRedirect());

        Record typeStruct = cls.typeStruct();
        if (typeStruct != null)
            builder.addType(new RecordGenerator(typeStruct).generate());

        if (cls.abstract_())
            builder.addType(implClass());

        if (cls.checkIsGObject()) {
            builder.addMethod(gobjectConstructor());
            BuilderGenerator generator = new BuilderGenerator(cls);
            builder.addType(generator.generateBuilderClass());
            if (!cls.abstract_()) // no builder() for abstract classes
                builder.addMethod(generator.generateBuilderMethod());
        }

        if (hasNativeHandles())
            builder.addType(nativeHandlesClass());

        return builder.build();
    }

    private TypeName genericActualTypeName() {
        var actualGeneric = cls.genericActual();
        return actualGeneric == null
                ? ClassNames.GENERIC_T
                : actualGeneric.typeName();
    }

    private MethodSpec parentAccessor() {
        ClassName className = cls.abstract_()
                ? cls.typeName().nestedClass(cls.name() + "$Impl")
                : cls.typeName();

        return MethodSpec.methodBuilder("asParent")
                .addJavadoc("""
                    Return this instance as if it were its parent type. Comparable to the
                    Java `super` keyword, but ensures the parent typeclass is also used in
                    native code.
                    
                    @return the instance as if it were its parent type
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
                    Create a $L instance for the provided memory address.
                    
                    @param address the memory address of the native object
                    """, name())
                .addParameter(MemorySegment.class, "address")
                .addStatement("super($T.reinterpret(address, getMemoryLayout().byteSize()))",
                        ClassNames.INTEROP);

        /*
         * Register a free-function for this class. If the class has a method
         * named unref(), use that one. If not, the MemoryCleaner will fallback
         * to g_free().
         */
        if (! cls.isInstanceOf("GObject", "Object")) {
            var m = cls.unrefFunc();
            if (m != null)
                spec.addStatement("$T.setFreeFunc(this, $S)",
                        ClassNames.MEMORY_CLEANER,
                        m.callableAttrs().cIdentifier());
        }
        return spec.build();
    }

    private MethodSpec gobjectConstructor() {
        return MethodSpec.constructorBuilder()
                .addJavadoc("Create a new $1L.", name())
                .addModifiers(Modifier.PUBLIC)
                .addStatement("super($T.NULL)", MemorySegment.class)
                .addStatement("$1T.newGObject(this, $2T.getType(this.getClass()), getMemoryLayout().byteSize(), (Object[]) null)",
                        ClassNames.INSTANCE_CACHE, ClassNames.TYPE_CACHE)
                .build();
    }

    private MethodSpec spliceCollectionWrapper(TypeName actualGeneric) {
        return MethodSpec.methodBuilder("splice")
                .addJavadoc("""
                        Modify this list by removing `nRemovals` elements starting at
                        `index` and replacing them with the elements in `additions`.
                        
                        @param index the index at which to splice the list
                        @param nRemovals the number of elements to remove
                        @param additions the elements to insert at the index
                        @throws IndexOutOfBoundsException if the index is out of range
                        """)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(int.class, "index")
                .addParameter(int.class, "nRemovals")
                .addParameter(ParameterizedTypeName.get(
                        ClassName.get(Collection.class),
                        WildcardTypeName.subtypeOf(actualGeneric)
                ), "additions")
                .addStatement("splice(index, nRemovals, additions.toArray($T[]::new))", actualGeneric)
                .build();
    }

    private MethodSpec spliceStringObjectUnwrapper() {
        return MethodSpec.methodBuilder("splice")
                .addJavadoc("""
                        Modify this list by removing `nRemovals` elements starting at
                        `index` and replacing them with the elements in `additions`.
                        
                        @param index the index at which to splice the list
                        @param nRemovals the number of elements to remove
                        @param additions the elements to insert at the index
                        @throws IndexOutOfBoundsException if the index is out of range
                        """)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(int.class, "index")
                .addParameter(int.class, "nRemovals")
                .addParameter(ArrayTypeName.of(ClassNames.STRING_OBJECT), "additions")
                .addStatement("splice(index, nRemovals, additions == null ? null : $T.stream(additions).map($T::getString).toArray(String[]::new))",
                        Arrays.class, ClassNames.STRING_OBJECT)
                .build();
    }

    private MethodSpec appendStringObjectUnwrapper() {
            return MethodSpec.methodBuilder("append")
                .addJavadoc("""
                        Add the specified element to the end of the list.
                        
                        @param e element to be appended to the list
                        """)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassNames.STRING_OBJECT, "e")
                .addStatement("append(e.getString())")
                .build();
    }
}
