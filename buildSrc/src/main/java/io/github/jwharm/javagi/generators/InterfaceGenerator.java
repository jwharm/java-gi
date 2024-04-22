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
import io.github.jwharm.javagi.gir.Interface;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.GeneratedAnnotationBuilder;

import javax.lang.model.element.Modifier;

public class InterfaceGenerator extends RegisteredTypeGenerator {

    private final Interface inf;

    public InterfaceGenerator(Interface inf) {
        super(inf);
        this.inf = inf;
    }

    public TypeSpec generate() {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(inf.typeName());
        builder.addAnnotation(GeneratedAnnotationBuilder.generate());

        if (inf.infoElements().doc() != null)
            builder.addJavadoc(new DocGenerator(inf.infoElements().doc()).generate());
        if (inf.infoAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        builder.addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassNames.PROXY)
                .addType(implClass());

        for (var prereq : inf.prerequisites())
            if (prereq.get() instanceof Interface parent)
                builder.addSuperinterface(parent.typeName());

        if (hasTypeMethod())
            builder.addMethod(getTypeMethod());

        addConstructors(builder);
        addFunctions(builder);
        addMethods(builder);
        addVirtualMethods(builder);
        addSignals(builder);

        Record typeStruct = inf.typeStruct();
        if (typeStruct != null)
            builder.addType(new RecordGenerator(typeStruct).generate());

        if (inf.hasProperties())
            builder.addType(new BuilderGenerator(inf).generateBuilderInterface());

        return builder.build();
    }
}
