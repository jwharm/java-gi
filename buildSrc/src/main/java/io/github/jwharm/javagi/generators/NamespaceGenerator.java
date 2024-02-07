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

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.github.jwharm.javagi.configuration.ClassNames;
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.util.Platform;

import javax.lang.model.element.Modifier;

public class NamespaceGenerator {

    private final Namespace ns;
    private final TypeSpec.Builder builder;

    public NamespaceGenerator(Namespace ns) {
        this.ns = ns;
        this.builder = TypeSpec.classBuilder(ns.typeName());
    }

    public TypeSpec generateGlobalsClass() {
        builder.addJavadoc("Constants and functions that are declared in the global $L namespace.",
                        ns.name())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addStaticBlock(loadLibraries())
                .addMethod(ensureInitialized())
                .addMethod(registerTypes());

        for (var constant : ns.constants()) {
            var fieldSpec = new TypedValueGenerator(constant).generateConstantDeclaration();
            if (fieldSpec != null)
                builder.addField(fieldSpec);
        }

        for (Function f : ns.functions())
            if (! f.skip())
                builder.addMethod(new MethodGenerator(f).generate());

        return builder.build();
    }

    private CodeBlock loadLibraries() {
        CodeBlock.Builder block = CodeBlock.builder()
                .beginControlFlow("switch ($T.getRuntimePlatform())",
                        ClassNames.PLATFORM);

        // Add case for each platform
        for (Integer platform : Platform.toList(ns.platforms())) {

            // Remove path from library name
            String library = ns.sharedLibrary(platform);
            if (library.contains("/"))
                library = library.substring(library.lastIndexOf("/") + 1);

            // Multiple library names (comma-separated)
            if (library.contains(",")) {
                block.beginControlFlow("case $S -> ",
                        Platform.toString(platform));
                for (String libName : library.split(","))
                    block.addStatement("$T.loadLibrary($S)",
                            ClassNames.LIB_LOAD,
                            libName);
                block.endControlFlow();
            }

            // Single library name
            else {
                block.addStatement("case $S -> $T.loadLibrary($S)",
                        Platform.toString(platform),
                        ClassNames.LIB_LOAD,
                        library);
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
            spec.addStatement("$T.register($T.getType(), $L)",
                    ClassNames.TYPE_CACHE, c.typeName(), c.constructorName());

        for (Interface i : ns.interfaces())
            spec.addStatement("$T.register($T.getType(), $L)",
                    ClassNames.TYPE_CACHE, i.typeName(), i.constructorName());

        for (Alias a : ns.aliases()) {
            RegisteredType target = a.type().get();
            if (target instanceof Class c)
                spec.addStatement("$T.register($T.getType(), $L)",
                        ClassNames.TYPE_CACHE, a.typeName(), c.constructorName());
            if (target instanceof Interface i)
                spec.addStatement("$T.register($T.getType(), $L)",
                        ClassNames.TYPE_CACHE, a.typeName(), i.constructorName());
        }

        return spec.build();
    }
}
