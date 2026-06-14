/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2026 Jan-Willem Harmannij
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

import org.javagi.configuration.ClassNames;
import org.javagi.gir.Enumeration;
import org.javagi.javapoet.MethodSpec;
import org.javagi.javapoet.TypeSpec;
import org.javagi.util.GeneratedAnnotationBuilder;

import javax.lang.model.element.Modifier;

import static org.javagi.util.Conversions.replaceKnownType;

/**
 * Generate a "FooException" class for a "FooError" enumeration.
 * The exception class will extend GErrorException.
 */
public class ExceptionGenerator {

    private final Enumeration e;
    private final TypeSpec.Builder builder;

    public ExceptionGenerator(Enumeration e) {
        this.e = e;
        this.builder = TypeSpec.classBuilder(generateName())
                .addAnnotation(GeneratedAnnotationBuilder.generate());
    }

    /**
     * Generate a name for the exception class.
     * Replaces "...Error" and "...EnumError" with "...Exception".
     */
    public String generateName() {
        String typeName = e.name();

        // Strip "...Enum"
        if (typeName.endsWith("Enum"))
            typeName = typeName.substring(0, typeName.length() - 4);

        // Strip "...Error"
        if (typeName.endsWith("Error"))
            typeName = typeName.substring(0, typeName.length() - 5);

        // Append "...Exception"
        typeName = typeName + "Exception";

        // Replace "IOException" with "GIOException" to prevent confusion
        return replaceKnownType(typeName, e.namespace());
    }

    public TypeSpec generate() {
        if (e.infoAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);
        return builder.addJavadoc("Represents a GError with domain {@link $T}.", e.typeName())
                .addModifiers(Modifier.PUBLIC)
                .superclass(ClassNames.GERROR_EXCEPTION)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ClassNames.G_ERROR, "err")
                        .addStatement("super(err)")
                        .build())
                .addMethod(MethodSpec.methodBuilder("getEnum")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(e.typeName())
                        .addStatement("return $T.of(getCode())", e.typeName())
                        .build())
                .build();
    }

}
