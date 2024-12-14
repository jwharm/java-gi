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
                .addStaticBlock(loadLibraries())
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

        if (hasDowncallHandles())
            builder.addType(downcallHandlesClass());

        return builder.build();
    }

    private CodeBlock loadLibraries() {
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

        return block.endControlFlow()
                .addStatement("registerTypes()")
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
            spec.addCode(register(c.constructorName(), c.typeName()));

        for (Interface i : ns.interfaces())
            spec.addCode(register(i.constructorName(), i.typeName()));

        for (Alias a : ns.aliases()) {
            RegisteredType target = a.lookup();
            if (target instanceof Class c)
                spec.addCode(register(c.constructorName(), a.typeName()));
            if (target instanceof Interface i)
                spec.addCode(register(i.constructorName(), a.typeName()));
        }

        for (Boxed b : ns.boxeds())
            spec.addCode(register(b.constructorName(), b.typeName()));

        return spec.build();
    }

    private CodeBlock register(PartialStatement constructor, ClassName typeName) {
        var stmt = PartialStatement.of(
                    "$typeCache:T.register($typeName:T.class, $typeName:T.getType(), ",
                        "typeCache", ClassNames.TYPE_CACHE,
                        "typeName", typeName)
                .add(constructor)
                .add(");\n");
        return CodeBlock.builder()
                .addNamed(stmt.format(), stmt.arguments())
                .build();
    }
}
