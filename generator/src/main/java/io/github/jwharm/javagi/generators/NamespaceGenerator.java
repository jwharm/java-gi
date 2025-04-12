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

package io.github.jwharm.javagi.generators;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.github.jwharm.javagi.configuration.ClassNames;
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.util.GeneratedAnnotationBuilder;
import io.github.jwharm.javagi.util.PartialStatement;
import io.github.jwharm.javagi.util.Platform;

import javax.lang.model.element.Modifier;

public class NamespaceGenerator extends RegisteredTypeGenerator {

    private final Namespace ns;
    private final TypeSpec.Builder builder;

    public NamespaceGenerator(Namespace ns) {
        super(ns);
        this.ns = ns;
        this.builder = TypeSpec.classBuilder(ns.typeName());
        this.builder.addAnnotation(GeneratedAnnotationBuilder.generate());
    }

    public TypeSpec generateGlobalsClass() {
        builder.addJavadoc("Constants and functions that are declared in the global $L namespace.",
                        ns.name())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addStaticBlock(staticInitializer())
                .addMethod(ensureInitialized())
                .addMethod(registerTypes());

        for (var constant : ns.constants()) {
            var fieldSpec = new TypedValueGenerator(constant)
                                        .generateConstantDeclaration();
            if (fieldSpec != null)
                builder.addField(fieldSpec);
        }

        for (Function f : ns.functions()) {
            if (!f.skip()) {
                builder.addMethod(new MethodGenerator(f).generate());
                if (f.hasBitfieldParameters())
                    builder.addMethod(new CallableGenerator(f)
                                                .generateBitfieldOverload());
            }
        }

        if (ns.name().equals("GLib"))
            builder.addMethod(glibIdleAddOnce())
                   .addMethod(glibTimeoutAddOnce())
                   .addMethod(glibTimeoutAddSecondsOnce());

        if (hasDowncallHandles())
            builder.addType(downcallHandlesClass());

        return builder.build();
    }

    private CodeBlock staticInitializer() {
        // Load libraries
        CodeBlock.Builder block = CodeBlock.builder()
                .beginControlFlow("switch ($T.getRuntimePlatform())",
                        ClassNames.PLATFORM);

        // Add case for each platform
        for (Integer platform : Platform.toList(Platform.ALL)) {
            String lib = ns.sharedLibrary(platform);

            // Add placeholder comment when this platform is not supported
            if (lib == null || (platform & ns.platforms()) == 0) {
                block.add("// add $L library here\n", Platform.toString(platform));
                continue;
            }

            // Remove path from library name
            if (lib.contains("/"))
                lib = lib.substring(lib.lastIndexOf("/") + 1);

            // Multiple library names (comma-separated)
            if (lib.contains(",")) {
                block.beginControlFlow("case $S -> ",
                        Platform.toString(platform));
                for (String libName : lib.split(","))
                    block.addStatement("$T.loadLibrary($S)",
                            ClassNames.INTEROP,
                            libName);
                block.endControlFlow();
            }

            // Single library name
            else {
                block.addStatement("case $S -> $T.loadLibrary($S)",
                        Platform.toString(platform),
                        ClassNames.INTEROP,
                        lib);
            }
        }

        block.endControlFlow();

        // Type registration functions for GObject and Gtk
        if ("GObject".equals(ns.name()))
            block.addStatement("$1T.setTypeRegisterFunction($2T.class, $3T::register)",
                    ClassNames.TYPE_CACHE, ClassNames.G_OBJECT, ClassNames.TYPES);

        if ("Gtk".equals(ns.name()))
            block.addStatement("$1T.setTypeRegisterFunction($2T.class, $3T::register)",
                    ClassNames.TYPE_CACHE, ClassNames.GTK_WIDGET, ClassNames.TEMPLATE_TYPES);

        // Cache all generated types
        return block.addStatement("registerTypes()")
                .build();
    }

    private MethodSpec ensureInitialized() {
        return MethodSpec.methodBuilder("javagi$ensureInitialized")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .build();
    }

    private MethodSpec registerTypes() {
        MethodSpec.Builder spec = MethodSpec.methodBuilder("registerTypes")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC);

        for (Class c : ns.classes())
            spec.addCode(register(c.constructorName(), c.typeName(), c.typeClassName()));

        for (Interface i : ns.interfaces())
            spec.addCode(register(i.constructorName(), i.typeName(), i.typeClassName()));

        for (Alias a : ns.aliases()) {
            RegisteredType target = a.lookup();
            if (target instanceof Class c)
                spec.addCode(register(c.constructorName(), a.typeName(), c.typeClassName()));
            if (target instanceof Interface i)
                spec.addCode(register(i.constructorName(), a.typeName(), i.typeClassName()));
        }

        for (Boxed b : ns.boxeds())
            spec.addCode(register(b.constructorName(), b.typeName(), null));

        return spec.build();
    }

    private CodeBlock register(PartialStatement constructor,
                               ClassName typeName,
                               ClassName typeClassName) {
        var stmt = PartialStatement.of(
                    "$typeCache:T.register($typeName:T.class, $typeName:T.getType(), ",
                        "typeCache", ClassNames.TYPE_CACHE,
                        "typeName", typeName)
                .add(constructor)
                .add(typeClassName == null ? ", null" : ", $typeClassName:T::new",
                        "typeClassName", typeClassName)
                .add(");\n");
        return CodeBlock.builder()
                .addNamed(stmt.format(), stmt.arguments())
                .build();
    }

    /*
     * We replace g_idle_add_once, g_timeout_add_once and
     * g_timeout_add_seconds_once with custom wrappers around g_idle_add_full
     * g_timeout_add_full and g_timeout_add_seconds_full, because the "_once"
     * functions leak memory (they don't notify when the callback is completed
     * and the arena for the upcall stub can be closed).
     */

    private MethodSpec glibIdleAddOnce() {
        return MethodSpec.methodBuilder("idleAddOnce")
                .addJavadoc("""
                     Adds a function to be called whenever there are no higher priority
                     events pending to the default main loop. The function is given the
                     default idle priority, {@code $1T.PRIORITY_DEFAULT_IDLE}.
                     <p>
                     The function will only be called once and then the source will be
                     automatically removed from the main context.
                     <p>
                     This function otherwise behaves like {@link $1T#idleAdd}.
                     
                     @param function function to call
                     @return the ID (greater than 0) of the event source""", ClassNames.G_LIB)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(int.class)
                .addParameter(ClassNames.G_SOURCE_ONCE_FUNC, "function")
                .addCode(CodeBlock.builder()
                        .add("return idleAdd(PRIORITY_DEFAULT_IDLE, () -> {\n")
                        .indent()
                        .addStatement("function.run()")
                        .addStatement("return SOURCE_REMOVE")
                        .unindent()
                        .addStatement("})")
                        .build())
                .build();
    }

    private MethodSpec glibTimeoutAddOnce() {
        return MethodSpec.methodBuilder("timeoutAddOnce")
                .addJavadoc("""
                    Sets a function to be called after {@code interval} milliseconds have elapsed,
                    with the default priority, {@code $1T.PRIORITY_DEFAULT}.
                    <p>
                    The given {@code function} is called once and then the source will be automatically
                    removed from the main context.
                    <p>
                    This function otherwise behaves like {@link $1T#timeoutAdd}.
                    
                    @param interval the time after which the function will be called, in
                      milliseconds (1/1000ths of a second)
                    @param function function to call
                    @return the ID (greater than 0) of the event source""", ClassNames.G_LIB)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(int.class)
                .addParameter(int.class, "interval")
                .addParameter(ClassNames.G_SOURCE_ONCE_FUNC, "function")
                .addCode(CodeBlock.builder()
                        .add("return timeoutAdd(PRIORITY_DEFAULT, interval, () -> {\n")
                        .indent()
                        .addStatement("function.run()")
                        .addStatement("return SOURCE_REMOVE")
                        .unindent()
                        .addStatement("})")
                        .build())
                .build();
    }

    private MethodSpec glibTimeoutAddSecondsOnce() {
        return MethodSpec.methodBuilder("timeoutAddSecondsOnce")
                .addJavadoc("""
                    This function behaves like {@link $1T#timeoutAddOnce} but with a range in
                    seconds.
                    
                    @param interval the time after which the function will be called, in seconds
                    @param function function to call
                    @return the ID (greater than 0) of the event source""", ClassNames.G_LIB)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(int.class)
                .addParameter(int.class, "interval")
                .addParameter(ClassNames.G_SOURCE_ONCE_FUNC, "function")
                .addCode(CodeBlock.builder()
                        .add("return timeoutAddSeconds(PRIORITY_DEFAULT, interval, () -> {\n")
                        .indent()
                        .addStatement("function.run()")
                        .addStatement("return SOURCE_REMOVE")
                        .unindent()
                        .addStatement("})")
                        .build())
                .build();
    }
}
