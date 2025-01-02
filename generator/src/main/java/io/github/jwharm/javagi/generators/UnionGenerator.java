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

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.github.jwharm.javagi.configuration.ClassNames;
import io.github.jwharm.javagi.gir.Union;
import io.github.jwharm.javagi.util.GeneratedAnnotationBuilder;

import javax.lang.model.element.Modifier;
import java.lang.foreign.Arena;

public class UnionGenerator extends RegisteredTypeGenerator {

    private final Union union;
    private final TypeSpec.Builder builder;

    public UnionGenerator(Union union) {
        super(union);
        this.union = union;
        this.builder = TypeSpec.classBuilder(union.typeName());
        this.builder.addAnnotation(GeneratedAnnotationBuilder.generate());
    }

    public TypeSpec generate() {
        if (union.infoElements().doc() != null)
            builder.addJavadoc(
                    new DocGenerator(union.infoElements().doc()).generate());

        if (union.infoAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        builder.addModifiers(Modifier.PUBLIC)
                .superclass(ClassNames.PROXY_INSTANCE)
                .addStaticBlock(staticBlock())
                .addMethod(memoryAddressConstructor());

        MethodSpec memoryLayout = new MemoryLayoutGenerator()
                                            .generateMemoryLayout(union);
        if (memoryLayout != null) {
            builder.addMethod(memoryLayout);
            builder.addMethod(constructor());
        }

        if (hasTypeMethod())
            builder.addMethod(getTypeMethod());

        addConstructors(builder);
        addMethods(builder);
        addFunctions(builder);

        if (union.toStringTarget() != null)
            builder.addMethod(toStringRedirect());

        if (hasDowncallHandles())
            builder.addType(downcallHandlesClass());

        return builder.build();
    }

    private MethodSpec constructor() {
        return MethodSpec.constructorBuilder()
                .addJavadoc("""
                        Allocate a new $1T.
                        
                        @param arena to control the memory allocation scope
                        """, union.typeName())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Arena.class, "arena")
                .addStatement("super(arena.allocate(getMemoryLayout()))")
                .build();
    }
}
