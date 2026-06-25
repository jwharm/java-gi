/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2026 the Java-GI developers
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

import org.javagi.javapoet.CodeBlock;
import org.javagi.javapoet.MethodSpec;
import org.javagi.configuration.ClassNames;
import org.javagi.gir.*;
import org.javagi.gir.Class;
import org.javagi.gir.Record;

import java.lang.foreign.ValueLayout;
import java.util.List;

import static org.javagi.util.CollectionUtils.listOfNonNull;
import static org.javagi.util.Conversions.primitiveClassName;
import static org.javagi.util.Conversions.toJavaBaseType;

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
        propagateExceptions(builder);
        readPointer(builder);
        freeGBytes(builder);
        freeGString(builder);
        unrefGObject(builder);
        takeOwnership(builder);
        scope(builder);
        reinterpretReturnedSegment(builder);
    }

    public void generateUpcall(MethodSpec.Builder builder, boolean longAsInt) {
        writePrimitiveAliasPointer(builder);
        writeOutParameter(builder, longAsInt);
        refGObjectUpcall(builder);
        freeGBytesUpcall(builder);
        freeGStringUpcall(builder);
    }

    private void propagateExceptions(MethodSpec.Builder builder) {
        builder.addStatement("$T.propagateExceptions()", ClassNames.EXCEPTION_HANDLER);
    }

    private void readPointer(MethodSpec.Builder builder) {
        if (v instanceof Parameter p
                && !p.isDestroyNotifyParameter()
                && (p.isOutParameter())) {

            // Null-check
            builder.beginControlFlow("if ($1L != null)", getName());

            // Pointer to single value
            if (array == null) {
                if (type.isLong()) {
                    builder.beginControlFlow("if ($T.longAsInt())", ClassNames.INTEROP)
                            .addStatement("$1L.set(_$1LPointer.get($2T.JAVA_INT, 0))", getName(), ValueLayout.class)
                            .nextControlFlow("else")
                            .addStatement("$1L.set((int) _$1LPointer.get($2T.JAVA_LONG, 0))", getName(), ValueLayout.class)
                            .endControlFlow();
                } else {
                    var stmt = CodeBlock.builder().add("$L.$L",
                            getName(),
                            (target instanceof Alias a && a.isValueWrapper()) ? "setValue(" : "set(");

                    CodeBlock identifier = CodeBlock.of("_$LPointer.get($L, 0)", getName(), getValueLayout(type));
                    if ((target instanceof Alias a && a.isValueWrapper()) || (type.isPrimitive() && type.isPointer())) {
                        stmt.add(identifier);
                        if (type.isBoolean())
                            stmt.add(" != 0");
                    } else {
                        stmt.add(marshalNativeToJava(type, identifier));
                    }
                    stmt.add(")");
                    builder.addStatement(stmt.build());
                }

                // End null-check if-block
                builder.endControlFlow();
                return;
            }

            // Pointer to array
            var stmt = CodeBlock.builder().add("$L.set(", getName());
            String len = array.sizeExpression(false);

            // Caller-allocated out-parameter array with known length
            if (p.isOutParameter() && len != null && p.callerAllocates()) {
                if (array.anyType() instanceof Type t && t.isPrimitive())
                    stmt.add("$T.get$LArray(_$LPointer, $L, $L)",
                            ClassNames.INTEROP, primitiveClassName(toJavaBaseType(t.name())), getName(), len, transfer());
                else
                    stmt.add(marshalNativeToJava(CodeBlock.of("_$LPointer", getName()), false));
            }

            // Arrays with primitive values and known length: there's an extra
            // level of indirection
            else if (len != null && array.anyType() instanceof Type t && t.isPrimitive() && !t.isBoolean()) {
                CodeBlock varName = CodeBlock.of("_$LPointer", getName());
                if (array.cType() != null && array.cType().endsWith("**"))
                    varName = CodeBlock.of("$T.dereference(_$LPointer)", ClassNames.INTEROP, getName());
                stmt.add("$T.get$LArray($L, $L, $L)",
                        ClassNames.INTEROP, primitiveClassName(toJavaBaseType(t.name())), varName, len, transfer());
            }

            // GArray & GPtrArray
            else if (listOfNonNull("GLib.Array", "GLib.PtrArray").contains(array.name()) && p.callerAllocates()) {
                stmt.add(marshalNativeToJava(CodeBlock.of("_$LPointer", getName()), false));
            }

            // Other arrays
            else {
                var identifier = CodeBlock.of("_$LPointer.get($T.ADDRESS, 0)", getName(), ValueLayout.class);
                stmt.add(marshalNativeToJava(identifier, false));
            }

            stmt.add(")");
            builder.addStatement(stmt.build());

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
                builder.addStatement("$1T.freeGBytes(_$2LGBytes)", ClassNames.INTEROP, getName());
            } else if (v instanceof Parameter p
                    && p.isOutParameter()
                    && p.transferOwnership() != TransferOwnership.NONE) {
                builder.addStatement("$1T.freeGBytes(_$2LPointer.get(ValueLayout.ADDRESS, 0))",
                        ClassNames.INTEROP, getName());
            } else if (v instanceof ReturnValue rv
                    && rv.transferOwnership() != TransferOwnership.NONE) {
                builder.addStatement("$1T.freeGBytes(_result)", ClassNames.INTEROP);
            }
        }
    }

    // Generate a call to Interop.freeGString()
    private void freeGString(MethodSpec.Builder builder) {
        if (target != null && target.checkIsGString()) {
            if (v instanceof Parameter p
                    && !p.isOutParameter()
                    && p.transferOwnership() == TransferOwnership.NONE) {
                builder.addStatement("$1T.freeGString(_$2LGString)", ClassNames.INTEROP, getName());
            } else if (v instanceof ReturnValue rv
                    && rv.transferOwnership() != TransferOwnership.NONE) {
                builder.addStatement("$1T.freeGString(_result)", ClassNames.INTEROP);
            }
        }
    }

    private void unrefGObject(MethodSpec.Builder builder) {
        if (v instanceof ReturnValue rv
                && rv.transferOwnership() == TransferOwnership.FULL
                && target != null
                && target.checkIsGObject()) {
            if (target instanceof Class)
                builder.addStatement("$T.unrefUnownedUserDefinedInstance(_returnValue)", ClassNames.INSTANCE_CACHE);
            else
                builder.beginControlFlow("if (_returnValue instanceof $T _object)", ClassNames.G_OBJECT)
                        .addStatement("$T.unrefUnownedUserDefinedInstance(_object)", ClassNames.INSTANCE_CACHE)
                        .endControlFlow();
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

        if (target instanceof Record r
                && (r.foreign() || r.checkIsGBytes() || r.checkIsGString() || r.checkIsGList()))
            return;

        if (List.of("GTypeInstance", "GTypeClass", "GTypeInterface").contains(target.cType()))
            return;

        var paramName = switch (v) {
            case ReturnValue _ -> "_returnValue";
            case Parameter _ -> getName() + ".get()";
            default -> throw new IllegalStateException("Unexpected value: " + v);
        };

        // With ownership transfer: Don't copy/ref the struct
        if (v.transferOwnership() != TransferOwnership.NONE) {
            if (v instanceof Parameter)
                builder.beginControlFlow("if ($L != null && $L != null)", getName(), paramName);
            else
                builder.beginControlFlow("if ($L != null)", paramName);
        }

        // No ownership transfer: Copy/ref the struct
        else {
            // Lookup the copy/ref function and the memory layout
            var copyFunc = slt.copyFunction();
            var hasMemoryLayout = slt instanceof FieldContainer fc
                    && new MemoryLayoutGenerator().canGenerate(fc);

            // Don't automatically copy the return values of GLib functions
            boolean skipNamespace = !target.namespace().parent().isInScope("GObject");

            // No copy function, and unknown size: copying is impossible
            if (skipNamespace || (!hasMemoryLayout && copyFunc == null))
                return;

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
        builder.addStatement("$T.takeOwnership($L)", ClassNames.MEMORY_CLEANER, paramName);
        new RegisteredTypeGenerator(target).setFreeFunc(builder, paramName, target.typeName());

        // End null-check
        builder.endControlFlow();
    }

    // Mark arena for parameters with async or notified scope, ready to close
    private void scope(MethodSpec.Builder builder) {
        if (v instanceof Parameter p) {
            boolean notified = p.scope() == Scope.NOTIFIED && p.destroy() != null;
            boolean async = p.scope() == Scope.ASYNC && !p.isDestroyNotifyParameter();
            if (notified || async)
                builder.addStatement("$1T.readyToClose(_$2LScope)", ClassNames.ARENAS, getName());
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
        if (target instanceof Alias a && a.isValueWrapper()
                && type.isPointer()
                && !type.isUnannotatedReference()) {
            builder.addStatement("$1LParam.set($2L, 0, _$1LAlias.getValue())", getName(), getValueLayout(type));
        }
    }

    private void writeOutParameter(MethodSpec.Builder builder, boolean longAsInt) {
        var p = (Parameter) v;
        if (!p.isOutParameter())
            return;

        // This is already handled in writePrimitiveAliasPointer()
        if (target instanceof Alias a && a.isValueWrapper() && type.isPointer())
            return;

        if (type == null || type.isUnannotatedReference())
            return;

        if (type.isLong()) {
            if (longAsInt)
                builder.addStatement("$LParam.set($T.JAVA_INT, 0, $L)",
                        getName(),
                        ValueLayout.class,
                        marshalJavaToNative(CodeBlock.of("_$LOut.get()", getName())));
            else
                builder.addStatement("$LParam.set($T.JAVA_LONG, 0, (long) $L)",
                        getName(),
                        ValueLayout.class,
                        marshalJavaToNative(CodeBlock.of("_$LOut.get()", getName())));
        } else {
            builder.addStatement("$LParam.set($L, 0, $L)",
                    getName(),
                    getValueLayout(type),
                    marshalJavaToNative(CodeBlock.of("_$LOut.get()", getName())));
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
            builder.beginControlFlow("if ($L.get() instanceof $T _gobjectUpcall)", paramName, ClassNames.G_OBJECT)
                   .addStatement("_gobjectUpcall.ref()")
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
