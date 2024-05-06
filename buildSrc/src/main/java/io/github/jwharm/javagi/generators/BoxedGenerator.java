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

import com.squareup.javapoet.TypeSpec;
import io.github.jwharm.javagi.configuration.ClassNames;
import io.github.jwharm.javagi.gir.Boxed;
import io.github.jwharm.javagi.util.GeneratedAnnotationBuilder;

import javax.lang.model.element.Modifier;

public class BoxedGenerator extends RegisteredTypeGenerator {

    private final Boxed boxed;
    private final TypeSpec.Builder builder;

    public BoxedGenerator(Boxed boxed) {
        super(boxed);
        this.boxed = boxed;
        this.builder = TypeSpec.classBuilder(boxed.typeName());
        this.builder.addAnnotation(GeneratedAnnotationBuilder.generate());
    }

    public TypeSpec generate() {
        if (boxed.infoElements().doc() != null)
            builder.addJavadoc(
                    new DocGenerator(boxed.infoElements().doc()).generate());

        if (boxed.infoAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        builder.addModifiers(Modifier.PUBLIC)
                .superclass(ClassNames.MANAGED_INSTANCE)
                .addStaticBlock(staticBlock())
                .addMethod(memoryAddressConstructor());

        if (hasTypeMethod())
            builder.addMethod(getTypeMethod());

        addFunctions(builder);

        if (hasDowncallHandles())
            builder.addType(downcallHandlesClass());

        return builder.build();
    }
}
