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

import com.squareup.javapoet.*;
import org.javagi.configuration.ClassNames;
import org.javagi.gir.*;
import org.javagi.util.Conversions;
import org.javagi.util.GeneratedAnnotationBuilder;
import org.javagi.gir.Class;
import org.javagi.gir.Record;

import javax.lang.model.element.Modifier;

import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Objects;

import static org.javagi.util.CollectionUtils.filter;
import static java.util.function.Predicate.not;

public class RegisteredTypeGenerator {

    private final RegisteredType rt;

    public RegisteredTypeGenerator(RegisteredType rt) {
        this.rt = rt;
    }

    protected String name() {
        return Conversions.toJavaSimpleType(rt.name(), rt.namespace());
    }

    protected CodeBlock staticBlock() {
        return CodeBlock.of("$T.javagi$$ensureInitialized();\n",
                rt.namespace().typeName());
    }

    protected boolean hasTypeMethod() {
        String typeFunc = rt.getTypeFunc();
        return typeFunc != null && !"intern".equals(typeFunc);
    }

    protected MethodSpec getTypeMethod() {
        return MethodSpec.methodBuilder("getType")
                .addJavadoc("""
                    Get the GType of the $L class
                    
                    @return the GType
                    """, name())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassNames.G_TYPE)
                .addStatement("return $T.getType($S)",
                        ClassNames.INTEROP,
                        rt.getTypeFunc())
                .build();
    }

    protected void addFunctions(TypeSpec.Builder builder) {
        for (Function f : filter(rt.children(), Function.class)) {
            if (!f.skip()) {
                builder.addMethod(new MethodGenerator(f).generate());
                if (f.hasBitfieldParameters())
                    builder.addMethod(new CallableGenerator(f)
                                                .generateBitfieldOverload());
            }
        }
    }

    protected void addConstructors(TypeSpec.Builder builder) {
        for (Constructor c : filter(rt.children(), Constructor.class)) {
            if (!c.skip()) {
                builder.addMethods(new ConstructorGenerator(c).generate());
                if (c.hasBitfieldParameters())
                    builder.addMethod(new CallableGenerator(c)
                                                .generateBitfieldOverload());
            }
        }
    }

    protected void addMethods(TypeSpec.Builder builder) {
        for (Method m : filter(rt.children(), Method.class)) {
            if (!m.skip()) {
                builder.addMethod(new MethodGenerator(m).generate());
                if (m.hasBitfieldParameters())
                    builder.addMethod(new CallableGenerator(m)
                                                .generateBitfieldOverload());
            }
        }
    }

    protected void addVirtualMethods(TypeSpec.Builder builder) {
        for (VirtualMethod vm : filter(rt.children(), VirtualMethod.class))
            if (!vm.skip())
                builder.addMethod(new MethodGenerator(vm).generate());
    }

    protected void addSignals(TypeSpec.Builder builder) {
        for (Signal s : filter(rt.children(), Signal.class)) {
            var generator = new SignalGenerator(s);
            builder.addType(generator.generateFunctionalInterface());
            builder.addMethod(generator.generateConnectMethod());
            if (!generator.emitMethodExists())
                builder.addMethod(generator.generateEmitMethod());
        }
    }

    protected MethodSpec memoryAddressConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("""
                    Create a $L proxy instance for the provided memory address.
                    
                    @param address the memory address of the native object
                    """, name())
                .addParameter(MemorySegment.class, "address");

        if (rt instanceof FieldContainer fc
                && (fc.opaque() || fc.hasOpaqueStructFields()))
            builder.addStatement("super(address)");
        else
            builder.addStatement("super($T.reinterpret(address, getMemoryLayout().byteSize()))",
                    ClassNames.INTEROP);
        return builder.build();
    }

    protected MethodSpec toStringRedirect() {
        String target = rt.toStringTarget();
        return MethodSpec.methodBuilder("toString")
                .addJavadoc("""
                        Returns a string representation of the object.
                        
                        @return a string representation of the object
                        """)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $T.toString($L)", Objects.class, target)
                .build();
    }

    protected TypeSpec implClass() {
        ClassName nested = rt.typeName().nestedClass(rt.name() + "$Impl");
        TypeSpec.Builder spec = TypeSpec.classBuilder(nested)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        TypeName base = rt.generic()
                ? ParameterizedTypeName.get(rt.typeName(), ClassNames.G_OBJECT)
                : rt.typeName();

        if (rt instanceof Interface i) {
            spec.addJavadoc("The $T type represents a native instance of the $T interface.",
                            nested,
                            rt.typeName())
                    .superclass(getInterfaceSuperclass(i))
                    .addSuperinterface(base)
                    .addStaticBlock(staticBlock());
        }

        if (rt instanceof Class)
            spec.addJavadoc("The $T type represents a native instance of the abstract $T class.",
                            nested,
                            rt.typeName())
                    .superclass(base);

        return spec.addMethod(MethodSpec.constructorBuilder()
                        .addJavadoc("""
                            Creates a new instance of $T for the provided memory address.

                            @param address the memory address of the instance
                            """, rt.typeName())
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(MemorySegment.class, "address")
                        .addStatement("super(address)")
                        .build())
                .build();
    }

    private TypeName getInterfaceSuperclass(Interface i) {
        Class c = i.prerequisiteBaseClass();
        return c.generic()
                ? ParameterizedTypeName.get(c.typeName(), ClassNames.G_OBJECT)
                : c.typeName();
    }

    public boolean hasDowncallHandles() {
        return (! listNamedFunctions().isEmpty());
    }

    public TypeSpec downcallHandlesClass() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(rt.helperClass())
                .addModifiers(Modifier.FINAL);

        if (rt instanceof Interface)
            builder.addAnnotation(GeneratedAnnotationBuilder.generate());
        else
            builder.addModifiers(Modifier.PRIVATE, Modifier.STATIC);

        for (Callable c : listNamedFunctions()) {
            if (!c.skip()) {
                var gen = new MethodGenerator(c);
                var spec = gen.generateNamedDowncallHandle(
                        Modifier.STATIC, Modifier.FINAL);
                builder.addField(spec);
            }
        }

        return builder.build();
    }

    private List<Callable> listNamedFunctions() {
        return rt.children().stream()
                .filter(c -> c instanceof Constructor
                                || c instanceof Function
                                || c instanceof Method)
                .map(Callable.class::cast)
                .filter(not(Callable::skip))
                .toList();
    }

    public void setFreeFunc(MethodSpec.Builder builder,
                            String identifier,
                            TypeName className) {

        if (List.of("GTypeInstance", "GTypeClass", "GTypeInterface")
                .contains(rt.cType()))
            return;

        if (rt instanceof Record rec && rec.foreign())
            return;

        // Class with unref method
        if (rt instanceof Class cls) {
            var unrefFunc = cls.unrefFunc();
            if (unrefFunc != null) {
                builder.addStatement("$T.setFreeFunc($L, $S)",
                        ClassNames.MEMORY_CLEANER,
                        identifier,
                        unrefFunc.callableAttrs().cIdentifier());
            }
            return;
        }

        // Boxed types
        if (rt instanceof StandardLayoutType slt && slt.freeFunction() == null && slt.isBoxedType()) {
            if (className == null)
                builder.addStatement("$T.setBoxedType($L, getType())",
                        ClassNames.MEMORY_CLEANER,
                        identifier);
            else
                builder.addStatement("$T.setBoxedType($L, $T.getType())",
                        ClassNames.MEMORY_CLEANER,
                        identifier,
                        className);
        }

        // Record or union with free-function
        if (rt instanceof StandardLayoutType slt) {
            var freeFunc = slt.freeFunction();
            if (freeFunc != null) {
                builder.addStatement("$T.setFreeFunc($L, $S)",
                        ClassNames.MEMORY_CLEANER,
                        identifier,
                        freeFunc.callableAttrs().cIdentifier());
            }
        }
    }
}
