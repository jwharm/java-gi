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
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.Conversions;

import javax.lang.model.element.Modifier;

import java.lang.foreign.MemorySegment;

public abstract class RegisteredTypeGenerator {

    public final static ClassName GTYPE = ClassName.get("org.gnome.glib", "Type");
    public final static ClassName GOBJECT = ClassName.get("org.gnome.gobject", "GObject");
    public final static ClassName GOBJECTS = ClassName.get("org.gnome.gobject", "GObjects");
    public final static ClassName TYPE_INSTANCE = ClassName.get("org.gnome.gobject", "TypeInstance");

    public final static TypeVariableName GENERIC_T = TypeVariableName.get("T", GOBJECT);

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
                    """, rt.cType())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get("org.gnome.glib", "Type"))
                .addStatement("return $T.getType($S)",
                        ClassNames.INTEROP,
                        rt.getTypeFunc())
                .build();
    }

    protected void addFunctions(TypeSpec.Builder builder) {
        if (rt instanceof FunctionContainer fc)
            for (Function f : fc.functions())
                if (!f.skip())
                    builder.addMethod(new MethodGenerator(f).generate());
    }

    protected void addConstructors(TypeSpec.Builder builder) {
        if (rt instanceof ConstructorContainer cc)
            for (Constructor c : cc.constructors())
                if (!c.skip())
                    new ConstructorGenerator(c).generate(builder);
    }

    protected void addMethods(TypeSpec.Builder builder) {
        if (rt instanceof MethodContainer mc)
            for (Method m : mc.methods())
                if (!m.skip())
                    builder.addMethod(new MethodGenerator(m).generate());
    }

    protected void addVirtualMethods(TypeSpec.Builder builder) {
        if (rt instanceof VirtualMethodContainer vmc)
            for (VirtualMethod vm : vmc.virtualMethods())
                if (!vm.skip())
                    builder.addMethod(new MethodGenerator(vm).generate());
    }

    protected void addSignals(TypeSpec.Builder builder) {
        if (rt instanceof SignalContainer sc) {
            for (Signal s : sc.signals()) {
                var generator = new SignalGenerator(s);
                builder.addType(generator.generateFunctionalInterface());
                builder.addMethod(generator.generateConnectMethod());
                if (!generator.emitMethodExists())
                    builder.addMethod(generator.generateEmitMethod());
            }
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

        if (rt instanceof Record rec && rec.isOpaque()
                || rt instanceof Union union && union.isOpaque())
            builder.addStatement("super(address)");
        else
            builder.addStatement("super($T.reinterpret(address, getMemoryLayout().byteSize()))",
                    ClassNames.INTEROP);
        return builder.build();
    }

    protected TypeSpec implClass() {
        ClassName nested = rt.typeName().nestedClass(rt.name() + "Impl");
        TypeSpec.Builder spec = TypeSpec.classBuilder(nested)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStaticBlock(staticBlock());

        if (rt instanceof Interface)
            spec.addJavadoc("The $T type represents a native instance of the $T interface.",
                            nested,
                            rt.typeName())
                    .superclass(ClassName.get("org.gnome.gobject", "GObject"))
                    .addSuperinterface(rt.typeName());

        if (rt instanceof Class)
            spec.addJavadoc("The $T type represents a native instance of the abstract $T class.",
                            nested,
                            rt.typeName())
                    .superclass(rt.typeName());

        return spec.addMethod(MethodSpec.constructorBuilder()
                        .addJavadoc("""
                            Creates a new instance of $T for the provided memory address.

                            @param address the memory address of the instance
                            """, rt.typeName())
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(MemorySegment.class, "address")
                        .addStatement("super(address)")
                        .build()
                )
                .build();
    }
}
