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
import io.github.jwharm.javagi.configuration.ClassNames;
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.Conversions;
import io.github.jwharm.javagi.util.PartialStatement;

import java.lang.foreign.ValueLayout;

import static io.github.jwharm.javagi.generators.RegisteredTypeGenerator.GOBJECT;
import static io.github.jwharm.javagi.util.Conversions.getValueLayoutPlain;
import static io.github.jwharm.javagi.util.Conversions.primitiveClassName;

public class PostprocessingGenerator extends TypedValueGenerator {

    private final Parameter p;

    public PostprocessingGenerator(Parameter p) {
        super(p);
        this.p = p;
    }

    public void generate(MethodSpec.Builder builder) {
        readPointer(builder);
        transferOwnership(builder);
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

                if (checkNull())
                    builder.beginControlFlow("if ($L != null)", getName());

                stmt.add(getName())
                        .add((target instanceof Alias a && a.type().isPrimitive())
                                ? ".setValue("
                                : ".set(");

                String identifier = "_%sPointer.get($valueLayout:T.%s, 0)"
                        .formatted(getName(), Conversions.getValueLayoutPlain(type));

                if ((target instanceof Alias a && a.type().isPrimitive())
                        || (type.isPrimitive() && type.isPointer())) {
                    stmt.add(identifier);
                    if (type.isBoolean())
                        stmt.add(" != 0");
                } else {
                    stmt.add(marshalNativeToJava(type, identifier, false));
                }
                stmt.add(");\n");
                builder.addNamedCode(stmt.format(), stmt.arguments());

                if (checkNull())
                    builder.endControlFlow();
            }

            // Pointer to array
            else {
                Type arrayType = (Type) array.anyType();
                String len = array.sizeExpression(false);
                String valueLayout = Conversions.getValueLayoutPlain(arrayType);

                // Out-parameter array
                if (p.isOutParameter() && len != null) {
                    PartialStatement payload = arrayType.isPrimitive()
                            ? PartialStatement.of("_%sPointer.toArray($valueLayout:T.%s)"
                                    .formatted(getName(), valueLayout),
                                    "valueLayout", ValueLayout.class)
                            : marshalNativeToJava("_%sPointer".formatted(getName()), false);

                    var stmt = PartialStatement.of(getName() + ".set(")
                            .add(payload)
                            .add(");\n");

                    builder.beginControlFlow("if ($L != null)", getName())
                            .addNamedCode(stmt.format(), stmt.arguments())
                            .endControlFlow();
                }

                // Array of primitive values
                else if (arrayType.isPrimitive() && (!arrayType.isBoolean())) {
                    builder.beginControlFlow("if ($1L != null)", getName())
                            .addStatement("$1L.set($2T.get$3LArrayFrom(_$1LPointer, $4L_arena, false))",
                                    getName(),
                                    ClassNames.INTEROP,
                                    primitiveClassName(arrayType.javaType()),
                                    len != null ? (len + ",") : "")
                            .endControlFlow();
                }

                // Array of proxy objects
                else {
                    builder.beginControlFlow("if ($L != null)",
                                    getName())
                            .beginControlFlow("for (int _idx = 0; _idx < $L; _idx++)",
                                    len)
                            .addStatement("var _object = _$LPointer.get($T.$L, _idx)",
                                    getName(),
                                    ValueLayout.class,
                                    valueLayout);

                    var assign = PartialStatement.of("_" + getName() + "Array[_idx] = ")
                            .add(marshalNativeToJava(arrayType, "_object", false))
                            .add(";\n");
                    builder.addNamedCode(assign.format(), assign.arguments())
                            .endControlFlow()
                            .addStatement("$1L.set(_$1LArray)",
                                    getName())
                            .endControlFlow();
                }
            }
        }
    }

    // If the parameter has attribute transfer-ownership="full", we must
    // register a reference, because the native code is going to call unref()
    // at some point while we still keep a pointer in the InstanceCache.
    private void transferOwnership(MethodSpec.Builder builder) {
        // GObjects where ownership is fully transferred away (unless it's an
        // out parameter or a pointer)
        if (target != null && target.checkIsGObject()
                && p.transferOwnership() == TransferOwnership.FULL
                && (!p.isOutParameter())
                && (type.cType() == null || (! type.cType().endsWith("**")))) {
            builder.addStatement("if ($L instanceof $T _gobject) _gobject.ref()",
                    getName(), GOBJECT);
        }

        // Same, but for structs/unions: Disable the cleaner
        else if ((target instanceof Record || target instanceof Union)
                && p.transferOwnership() == TransferOwnership.FULL
                && (!p.isOutParameter())
                && (type.cType() == null || (! type.cType().endsWith("**")))) {
            builder.addStatement(
                    checkNull()
                            ? "if ($1L != null) $2T.yieldOwnership($1L.handle())"
                            : "$2T.yieldOwnership($1L.handle())",
                    getName(),
                    ClassNames.MEMORY_CLEANER);
        }
    }

    private void writePrimitiveAliasPointer(MethodSpec.Builder builder) {
        if (target instanceof Alias a && a.type().isPrimitive() && type.isPointer())
            builder.addStatement("$1LParam.set($2T.$3L, 0, _$1LAlias.getValue())",
                    getName(),
                    ValueLayout.class,
                    getValueLayoutPlain(type));
    }

    private void writeOutParameter(MethodSpec.Builder builder) {
        if (!p.isOutParameter())
            return;

        if (type != null) {
            PartialStatement payload;
            if (type.isPrimitive() || (target instanceof Alias a && a.type().isPrimitive())) {
                payload = PartialStatement.of("_" + getName() + "Out.get()");
                if (type.isBoolean())
                    payload.add(" ? 1 : 0");
            }
            else if (target instanceof FlaggedType)
                payload = PartialStatement.of("_" + getName() + "Out.get().getValue()");
            else
                payload = marshalJavaToNative("_" + getName() + "Out.get()");

            var stmt = PartialStatement.of(getName())
                    .add("Param.set($valueLayout:T.", "valueLayout", ValueLayout.class)
                    .add(getValueLayoutPlain(type))
                    .add(", 0, ")
                    .add(payload)
                    .add(");\n");
            builder.addNamedCode(stmt.format(), stmt.arguments());
        }
    }
}
