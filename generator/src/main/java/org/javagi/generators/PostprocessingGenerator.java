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

package org.javagi.generators;

import com.squareup.javapoet.MethodSpec;
import org.javagi.configuration.ClassNames;
import org.javagi.gir.*;
import org.javagi.gir.Class;
import org.javagi.util.PartialStatement;
import org.javagi.gir.Record;

import java.lang.foreign.ValueLayout;
import java.util.List;

import static org.javagi.util.CollectionUtils.listOfNonNull;

public class PostprocessingGenerator extends TypedValueGenerator {

    private final Callable func;

    public PostprocessingGenerator(TypedValue value) {
        super(value);
        func = (Callable) switch (v) {
            case ReturnValue _ -> v.parent();
            case Parameter _ -> v.parent().parent();
            default -> throw new IllegalStateException("Unexpected value: " + v);
        };
    }

    public void generate(MethodSpec.Builder builder) {
        readPointer(builder);
        refGObject(builder);
        freeGBytes(builder);
        freeGString(builder);
        takeOwnership(builder);
        scope(builder);
        reinterpretReturnedSegment(builder);
    }

    public void generateUpcall(MethodSpec.Builder builder) {
        writePrimitiveAliasPointer(builder);
        writeOutParameter(builder);
        refGObjectUpcall(builder);
        freeGBytesUpcall(builder);
        freeGStringUpcall(builder);
    }

    private void readPointer(MethodSpec.Builder builder) {
        if (v instanceof Parameter p
                && !p.isDestroyNotifyParameter()
                && (p.isOutParameter()
                    || (type != null
                        && type.isPointer()
                        && target instanceof Alias a
                        && a.isValueWrapper()))) {

            // Pointer to single value
            if (array == null) {
                var stmt = PartialStatement.of(null, "valueLayout", ValueLayout.class);

                stmt.add(getName())
                    .add((target instanceof Alias a && a.isValueWrapper()) ? ".setValue(" : ".set(");

                var layout = getValueLayout(type);
                String identifier = "_%sPointer.get(%s, 0)".formatted(getName(), layout.format());

                if ((target instanceof Alias a && a.isValueWrapper())
                        || (type.isPrimitive() && type.isPointer())) {
                    stmt.add(identifier);
                    if (type.isBoolean())
                        stmt.add(" != 0");
                } else {
                    stmt.add(marshalNativeToJava(type, identifier));
                }
                stmt.add(");\n");

                // Null-check
                if (checkNull())
                    builder.beginControlFlow("if ($1L != null)", getName())
                           .addNamedCode(stmt.format(), stmt.arguments())
                           .endControlFlow();
                else
                    builder.addNamedCode(stmt.format(), stmt.arguments());

                return;
            }

            // Null-check
            builder.beginControlFlow("if ($1L != null)", getName());

            // Pointer to array
            String len = array.sizeExpression(false);
            PartialStatement payload;

            // Caller-allocated out-parameter array with known length
            if (p.isOutParameter() && len != null && p.callerAllocates()) {
                payload = array.anyType() instanceof Type t && t.isPrimitive()
                        ? PartialStatement.of("_$name:LPointer.toArray(", "name", getName())
                            .add(getValueLayout(t))
                            .add(")")
                        : marshalNativeToJava("_%sPointer".formatted(getName()), false);
            }

            // Arrays with primitive values and known length: there's an extra
            // level of indirection
            else if (len != null
                        && array.anyType() instanceof Type t
                        && t.isPrimitive()
                        && !t.isBoolean()) {
                String varName = "_$name:LPointer";
                if (array.cType() != null && array.cType().endsWith("**"))
                    varName = "$interop:T.dereference(" + varName + ")";
                payload = PartialStatement.of(varName + "$Z.reinterpret(" + len + " * ",
                                "interop", ClassNames.INTEROP,
                                "name", getName(),
                                "valueLayout", ValueLayout.class)
                        .add(getValueLayout(t))
                        .add(".byteSize(), _arena, null)$Z.toArray(")
                        .add(getValueLayout(t))
                        .add(")");
            }

            // GArray & GPtrArray
            else if (listOfNonNull("GLib.Array", "GLib.PtrArray").contains(array.name())
                    && p.callerAllocates()) {
                payload = marshalNativeToJava("_$name:LPointer", false)
                        .add(null, "name", getName(), "valueLayout", ValueLayout.class);
            }

            // Other arrays
            else {
                payload = marshalNativeToJava("_$name:LPointer.get($valueLayout:T.ADDRESS, 0)", false)
                        .add(null, "name", getName(), "valueLayout", ValueLayout.class);
            }

            var stmt = PartialStatement.of(getName())
                    .add(".set(")
                    .add(payload)
                    .add(");\n");
            builder.addNamedCode(stmt.format(), stmt.arguments());

            // End null-check if-block
            builder.endControlFlow();
        }
    }

    // Generate a call to Interop.freeGBytes() --> g_bytes_unref()
    private void freeGBytes(MethodSpec.Builder builder) {
        if (target != null && target.checkIsGBytes()) {
            if (v instanceof Parameter p
                    && !p.isOutParameter()
                    && p.transferOwnership() == TransferOwnership.NONE) {
                builder.addStatement("$1T.freeGBytes(_$2LGBytes)",
                        ClassNames.INTEROP, getName());
            } else if (v instanceof Parameter p
                    && p.isOutParameter()
                    && p.transferOwnership() != TransferOwnership.NONE) {
                builder.addStatement("$1T.freeGBytes(_$2LPointer.get(ValueLayout.ADDRESS, 0))",
                        ClassNames.INTEROP, getName());
            } else if (v instanceof ReturnValue rv
                    && rv.transferOwnership() != TransferOwnership.NONE) {
                builder.addStatement("$1T.freeGBytes($2L)",
                        ClassNames.INTEROP, "_result");
            }
        }
    }

    // Generate a call to Interop.freeGString()
    private void freeGString(MethodSpec.Builder builder) {
        if (target != null && target.checkIsGString()) {
            if (v instanceof Parameter p
                    && !p.isOutParameter()
                    && p.transferOwnership() == TransferOwnership.NONE) {
                builder.addStatement("$1T.freeGString(_$2LGString)",
                        ClassNames.INTEROP, getName());
            } else if (v instanceof Parameter p
                    && p.isOutParameter()
                    && p.transferOwnership() != TransferOwnership.NONE) {
                builder.addStatement("$1T.freeGString(_$2LPointer.get(ValueLayout.ADDRESS, 0))",
                        ClassNames.INTEROP, getName());
            } else if (v instanceof ReturnValue rv
                    && rv.transferOwnership() != TransferOwnership.NONE) {
                builder.addStatement("$1T.freeGString($2L)",
                        ClassNames.INTEROP, "_result");
            }
        }
    }

    // Ref GObject when ownership is not transferred
    private void refGObject(MethodSpec.Builder builder) {
        if (v instanceof ReturnValue rv
                && target != null
                && target.checkIsGObject()
                && rv.transferOwnership() == TransferOwnership.NONE
                // don't call ref() from ref() itself
                && (! "ref".equals(func.name()))
                && (! "ref_sink".equals(func.name()))) {
            if (target instanceof Class) {
                if (checkNull())
                    builder.addStatement("if (_returnValue != null) _returnValue.ref()");
                else
                    builder.addStatement("_returnValue.ref()");
            } else {
                // For interfaces and aliases, check if it's actually a GObject instance
                builder.beginControlFlow("if (_returnValue instanceof $T _gobject)", ClassNames.G_OBJECT)
                       .addStatement("_gobject.ref()")
                       .endControlFlow();
            }
        }
    }

    // Add cleaner to a struct/boxed/union pointer that we "own".
    // * Only for return values and out-parameters
    // * Exclude foreign types
    // * GBytes and GString have their own postprocessing
    // * GList/GSList have their own cleaner
    // * GTypeInstance/Class/Interface are special cases
    private void takeOwnership(MethodSpec.Builder builder) {
        if (v instanceof Parameter p && !p.isOutParameter())
            return;

        if (! (target instanceof StandardLayoutType slt)) // Record, Union or Boxed
            return;

        if (target instanceof Record r)
            if (r.foreign()
                    || r.checkIsGBytes()
                    || r.checkIsGString()
                    || r.checkIsGList())
                return;

        if (List.of("GTypeInstance", "GTypeClass", "GTypeInterface")
                .contains(target.cType()))
            return;

        var paramName = switch (v) {
            case ReturnValue _ -> "_returnValue";
            case Parameter _ -> getName() + ".get()";
            default -> throw new IllegalStateException("Unexpected value: " + v);
        };

        // With ownership transfer: Don't copy/ref the struct
        if (v.transferOwnership() != TransferOwnership.NONE) {
            builder.beginControlFlow("if ($1L != null)", paramName);
        }

        // No ownership transfer: Copy/ref the struct
        else {
            // Lookup the copy/ref function and the memory layout
            var copyFunc = slt.copyFunction();
            var hasMemoryLayout = new MemoryLayoutGenerator().canGenerate(slt);

            // Don't automatically copy the return values of GLib functions
            boolean skipNamespace = !target.namespace().parent().isInScope("GObject");

            // No copy function, and unknown size: copying is impossible
            if (skipNamespace || (!hasMemoryLayout && copyFunc == null)) {
                return;
            }

            builder.beginControlFlow("if ($1L != null)", paramName);

            // Don't copy the result of ref(), ref_sink() or copy()
            if (List.of("ref", "ref_sink", "copy").contains(func.name())
                    || (copyFunc != null && copyFunc.name().equals(func.name()))) {
            }

            // GValue: Call ValueUtil.copy()
            else if (slt.checkIsGValue()) {
                if (v instanceof ReturnValue)
                    builder.addStatement("$1L = $2T.copy($1L)",
                            paramName, ClassNames.VALUE_UTIL);
                else
                    builder.addStatement("$1L.set($2T.copy($3L))",
                            getName(), ClassNames.VALUE_UTIL, paramName);

                // Don't register the copy with the memory cleaner. It has
                // been allocated with Arena.ofAuto().
                builder.endControlFlow();
                return;
            }

            // No copy function, but it has a get-type function: Copy the
            // segment using BoxedUtil.copy(). It will fallback to
            // Interop.copy() when the type is not boxed.
            else if (copyFunc == null && slt.getTypeFunc() != null) {
                if (v instanceof ReturnValue) {
                    // reassign _returnValue to boxed copy
                    builder.addStatement("$1L = new $3T($2T.copy($3T.getType(), $1L,$W$3T.getMemoryLayout().byteSize()))",
                            paramName, ClassNames.BOXED_UTIL, v.anyType().typeName());
                } else {
                    // set out-parameter to boxed copy
                    builder.addStatement("$1L.set(new $3T($2T.copy($3T.getType(), $1L.get(),$W$3T.getMemoryLayout().byteSize())))",
                            getName(), ClassNames.BOXED_UTIL, v.anyType().typeName());
                }
            }

            // Copy function is an instance method
            else if (copyFunc instanceof Method m) {
                if (v instanceof ReturnValue)
                    builder.addStatement("$1L = $1L.$2L()",
                            paramName, MethodGenerator.getName(m));
                else
                    builder.addStatement("$1L.set($2L.$3L())",
                            getName(), paramName, MethodGenerator.getName(m));
            }

            // Copy function is a function (static method)
            else if (copyFunc instanceof Function f && f.parent() instanceof RegisteredType rt) {
                if (v instanceof ReturnValue)
                    builder.addStatement("$1L = $2T.$3L($1L)",
                            paramName, rt.typeName(), MethodGenerator.getName(f));
                else
                    builder.addStatement("$1L.set($2T.$3L($4L))",
                            getName(), rt.typeName(), MethodGenerator.getName(f), paramName);
            }
        }

        // Register the returned instance with the memory cleaner
        builder.addStatement("$T.takeOwnership($L)",
                ClassNames.MEMORY_CLEANER, paramName);
        new RegisteredTypeGenerator(target)
                .setFreeFunc(builder, paramName, target.typeName());

        // End null-check
        builder.endControlFlow();
    }

    // Mark arena for parameters with async or notified scope, ready to close
    private void scope(MethodSpec.Builder builder) {
        if (v instanceof Parameter p) {
            boolean notified = p.scope() == Scope.NOTIFIED && p.destroy() != null;
            boolean async = p.scope() == Scope.ASYNC && !p.isDestroyNotifyParameter();
            if (notified || async)
                builder.addStatement("$1T.readyToClose(_$2LScope)",
                        ClassNames.ARENAS, getName());
        }
    }

    private void reinterpretReturnedSegment(MethodSpec.Builder builder) {
        var cIdentifier = func.callableAttrs().cIdentifier();
        if (cIdentifier == null)
            return;

        if (! (v instanceof  ReturnValue))
            return;

        if (List.of("g_malloc", "g_malloc0").contains(cIdentifier))
            builder.addStatement("_returnValue = _returnValue.reinterpret(nBytes)");

        else if (List.of("g_malloc_n", "g_malloc0_n").contains(cIdentifier))
            builder.addStatement("_returnValue = _returnValue.reinterpret(nBlocks * nBlockBytes)");

        else if ("g_boxed_copy".equals(cIdentifier))
            builder.addStatement("if (srcBoxed != null) _returnValue = _returnValue.reinterpret(srcBoxed.byteSize())");
    }

    private void writePrimitiveAliasPointer(MethodSpec.Builder builder) {
        if (target instanceof Alias a && a.isValueWrapper() && type.isPointer()) {
            var stmt = PartialStatement.of("$name:LParam.set(")
                    .add(getValueLayout(type))
                    .add(", 0, _$name:LAlias.getValue());\n", "name", getName());
            builder.addNamedCode(stmt.format(), stmt.arguments());
        }
    }

    private void writeOutParameter(MethodSpec.Builder builder) {
        var p = (Parameter) v;
        if (!p.isOutParameter())
            return;

        if (type != null) {
            var stmt = PartialStatement.of("$name:LParam.set(", "name", getName())
                    .add(getValueLayout(type))
                    .add(", 0, ")
                    .add(marshalJavaToNative("_" + getName() + "Out.get()"))
                    .add(");\n");
            builder.addNamedCode(stmt.format(), stmt.arguments());
        }
    }

    // Ref GObject when ownership is transferred to a callback out-parameter
    private void refGObjectUpcall(MethodSpec.Builder builder) {
        if (v instanceof Parameter p
                && target != null
                && target.checkIsGObject()
                && p.transferOwnership() == TransferOwnership.FULL
                && p.isOutParameter()) {
            String paramName = "_" + getName() + "Out";
            builder.beginControlFlow("if ($L.get() instanceof $T _gobject)", paramName, ClassNames.G_OBJECT)
                   .addStatement("_gobject.ref()")
                   .endControlFlow();
        }
    }

    private void freeGBytesUpcall(MethodSpec.Builder builder) {
        if (v instanceof Parameter p
                && target != null
                && target.checkIsGBytes()
                && p.transferOwnership() != TransferOwnership.NONE) {
            builder.addStatement("$1T.freeGBytes($2L)", ClassNames.INTEROP, getName());
        }
    }

    private void freeGStringUpcall(MethodSpec.Builder builder) {
        if (v instanceof Parameter p
                && target != null
                && target.checkIsGString()
                && p.transferOwnership() != TransferOwnership.NONE) {
            builder.addStatement("$1T.freeGString($2L)", ClassNames.INTEROP, getName());
        }
    }
}
