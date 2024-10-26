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
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.util.PartialStatement;

import java.lang.foreign.ValueLayout;

public class PostprocessingGenerator extends TypedValueGenerator {

    private final Parameter p;

    public PostprocessingGenerator(Parameter p) {
        super(p);
        this.p = p;
    }

    public void generate(MethodSpec.Builder builder) {
        readPointer(builder);
    }

    public void generateUpcall(MethodSpec.Builder builder) {
        writePrimitiveAliasPointer(builder);
        writeOutParameter(builder);
    }

    private void readPointer(MethodSpec.Builder builder) {
        if (p.isOutParameter()
                || (type != null
                    && type.isPointer()
                    && target instanceof Alias a
                    && a.type().isPrimitive())) {

            // Pointer to single value
            if (array == null) {
                var stmt = PartialStatement.of(null, "valueLayout", ValueLayout.class);

                stmt.add(getName())
                        .add((target instanceof Alias a && a.type().isPrimitive())
                                ? ".setValue("
                                : ".set(");

                var layout = generateValueLayoutPlain(type);
                String identifier = "_%sPointer.get(%s, 0)"
                        .formatted(getName(), layout.format());

                if ((target instanceof Alias a && a.type().isPrimitive())
                        || (type.isPrimitive() && type.isPointer())) {
                    stmt.add(identifier);
                    if (type.isBoolean())
                        stmt.add(" != 0");
                } else {
                    stmt.add(marshalNativeToJava(type, identifier, false));
                }
                stmt.add(");\n");

                // Null-check
                if (checkNull())
                    builder.beginControlFlow("if ($1L != null)", getName())
                            .addNamedCode(stmt.format(), stmt.arguments())
                            .endControlFlow();
                else
                    builder.addNamedCode(stmt.format(), stmt.arguments());
            }

            // Pointer to array
            else {
                String len = array.sizeExpression(false);
                PartialStatement payload;
                Type arrayType = (Type) array.anyType();

                if (p.isOutParameter() && len != null && p.callerAllocates()) {
                    // Out-parameter array with known length
                    payload = arrayType.isPrimitive()
                            ? PartialStatement.of("_$name:LPointer.toArray(",
                                            "name", getName())
                                    .add(generateValueLayoutPlain(arrayType))
                                    .add(")")
                            : marshalNativeToJava("_%sPointer"
                                    .formatted(getName()), false);
                } else if (len != null
                            && arrayType.isPrimitive()
                            && !arrayType.isBoolean()) {
                    // Arrays with primitive values and known length
                    payload = PartialStatement.of("_$name:LPointer$Z.get($valueLayout:T.ADDRESS, 0)$Z.reinterpret(" + len + " * ",
                                    "name", getName(),
                                    "valueLayout", ValueLayout.class)
                            .add(generateValueLayoutPlain(arrayType))
                            .add(".byteSize(), _arena, null)$Z.toArray(")
                            .add(generateValueLayoutPlain(arrayType))
                            .add(")");
                } else {
                    // Other arrays
                    payload = marshalNativeToJava("_" + getName() + "Pointer", false);
                }

                var stmt = PartialStatement.of(getName())
                        .add(".set(")
                        .add(payload)
                        .add(");\n");

                // Null-check
                builder.beginControlFlow("if ($1L != null)", getName())
                        .addNamedCode(stmt.format(), stmt.arguments())
                        .endControlFlow();
            }
        }
    }

    private void writePrimitiveAliasPointer(MethodSpec.Builder builder) {
        if (target instanceof Alias a
                && a.type().isPrimitive()
                && type.isPointer()) {
            var stmt = PartialStatement.of("$name:LParam.set(")
                    .add(generateValueLayoutPlain(type))
                    .add(", 0, _$name:LAlias.getValue());\n",
                            "name", getName());
            builder.addNamedCode(stmt.format(), stmt.arguments());
        }
    }

    private void writeOutParameter(MethodSpec.Builder builder) {
        if (!p.isOutParameter())
            return;

        if (type != null) {
            PartialStatement payload;
            if (type.isPrimitive()
                    || (target instanceof Alias a && a.type().isPrimitive())) {
                payload = PartialStatement.of("_" + getName() + "Out.get()");
                if (type.isBoolean())
                    payload.add(" ? 1 : 0");
            }
            else if (target instanceof FlaggedType)
                payload = PartialStatement.of("_$name:LOut.get().getValue()",
                        "name", getName());
            else
                payload = marshalJavaToNative("_" + getName() + "Out.get()");

            var stmt = PartialStatement.of("$name:LParam.set(",
                            "name", getName())
                    .add(generateValueLayoutPlain(type))
                    .add(", 0, ")
                    .add(payload)
                    .add(");\n");
            builder.addNamedCode(stmt.format(), stmt.arguments());
        }
    }
}
