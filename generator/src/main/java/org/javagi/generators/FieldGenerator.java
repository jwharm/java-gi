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
import com.squareup.javapoet.TypeName;
import org.javagi.configuration.ClassNames;
import org.javagi.gir.*;
import org.javagi.util.PartialStatement;
import org.javagi.gir.Class;

import javax.lang.model.element.Modifier;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static org.javagi.util.Conversions.*;

public class FieldGenerator extends TypedValueGenerator {

    private static final String READ_PREFIX = "read";
    private static final String WRITE_PREFIX = "write";
    private static final String OVERRIDE_PREFIX = "override";

    private final Field f;
    private final Callback cb;

    public FieldGenerator(Field f) {
        super(f);
        this.f = f;
        this.cb = f.callback();
    }

    private String methodName(String prefix) {
        String methodName = prefix + toCamelCase(f.name(), true);
        for (Node node : f.parent().children()) {
            if (node instanceof Callable func
                    && methodName.equals(toJavaIdentifier(func.name()))) {
                methodName += "_";
                break;
            }
        }
        return methodName;
    }

    public MethodSpec generateReadMethod() {
        // To read from ...** fields, you must provide the length of the array.
        boolean isArray = type != null && type.isActuallyAnArray();

        // Override the type of long values
        boolean isLong = type != null && type.isLong();
        TypeName typeName = isLong ? TypeName.INT : getType();

        var spec = MethodSpec.methodBuilder(methodName(READ_PREFIX))
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName)
                .addJavadoc("Read the value of the field {@code $L}.\n\n", f.name());
        if (isArray)
            spec.addJavadoc("@param length the number of {@code $L} to read", f.name());
        spec.addJavadoc("@return The value of the field {@code $L}", f.name());
        if (isArray)
            spec.addParameter(int.class, "length");

        // Allocator for array field getters
        if (f.allocatesMemory())
            spec.addStatement("$1T _arena = $1T.ofAuto()", Arena.class);

        // Read the memory segment of an embedded field from the struct (not a pointer)
        if ((type != null)
                && (!type.isPointer())
                && (target instanceof Class || target instanceof Interface)) {
            var calcOffset = "long _offset = getMemoryLayout().byteOffset($T.PathElement.groupElement($S))";
            var returnSlice = PartialStatement.of("return ")
                    .add(marshalNativeToJava("handle().asSlice(_offset)", false))
                    .add(";\n");
            return spec.addStatement(calcOffset, MemoryLayout.class, f.name())
                    .addNamedCode(returnSlice.format(), returnSlice.arguments())
                    .build();
        }

        // Read a pointer or primitive value from the struct
        TypeName carrierType = getCarrierTypeName(f.anyType(), true);
        spec.addStatement("$T _varHandle = getMemoryLayout().varHandle($T.PathElement.groupElement($S))",
                VarHandle.class, MemoryLayout.class, f.name());
        var getResult = PartialStatement.of("$carrierType:T _result = ", "carrierType", carrierType);
        if (isLong)
            getResult.add("$interop:T.longAsInt() ? (int) _varHandle.get(handle(), 0) : (int) (long) _varHandle.get(handle(), 0);\n",
                    "interop", ClassNames.INTEROP);
        else
            getResult.add("($carrierType:T) _varHandle.get(handle(), 0);\n");
        var returnResult = PartialStatement.of("return ")
                .add(marshalNativeToJava("_result", false))
                .add(";\n");
        return spec.addNamedCode(getResult.format(), getResult.arguments())
                .addNamedCode(returnResult.format(), returnResult.arguments())
                .build();
    }

    public MethodSpec generateWriteMethod() {
        var spec = MethodSpec.methodBuilder(
                        methodName(cb != null ? OVERRIDE_PREFIX : WRITE_PREFIX))
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("""
                        Write a value in the field {@code $1L}.
                        
                        @param $2L The new value for the field {@code $1L}
                        """, f.name(), getName());

        // Override the type of long values
        boolean isLong = type != null && type.isLong();
        TypeName typeName = isLong ? TypeName.INT : getType();
        spec.addParameter(typeName, getName());

        if (f.allocatesMemory())
            spec.addJavadoc("@param _arena to control the memory allocation scope\n")
                .addParameter(Arena.class, "_arena");

        var stmt = PartialStatement.of(
                "$varHandle:T _varHandle = getMemoryLayout().varHandle($memoryLayout:T.PathElement.groupElement($fieldName:S));\n",
                "varHandle", VarHandle.class,
                "memoryLayout", MemoryLayout.class,
                "fieldName", f.name());
        spec.addNamedCode(stmt.format(), stmt.arguments());

        if (isLong) {
            spec.beginControlFlow("if ($T.longAsInt())", ClassNames.INTEROP);
            spec.addStatement("_varHandle.set(handle(), 0, $L)", getName());
            spec.nextControlFlow("else");
            spec.addStatement("_varHandle.set(handle(), 0, (long) $L)", getName());
            spec.endControlFlow();
        }

        else {
            stmt = PartialStatement.of(null, "memorySegment", MemorySegment.class);
            if (type != null && type.isPointer() && (type.isPrimitive() || (target instanceof EnumType))) {
                // Pointer to a primitive value is an opaque MemorySegment
                stmt.add(getName());
            } else {
                stmt.add(marshalJavaToNative(getName()));
            }

            if (checkNull())
                spec.addNamedCode("_varHandle.set(handle(), 0, (" + getName() + " == null ? $memorySegment:T.NULL : "
                                + stmt.format() + "));\n", stmt.arguments());
            else
                spec.addNamedCode("_varHandle.set(handle(), 0, " + stmt.format() + ");\n", stmt.arguments());
        }

        return spec.build();
    }

    public MethodSpec generateReadCopyMethod() {
        return MethodSpec.methodBuilder(methodName(READ_PREFIX))
                .addModifiers(Modifier.PUBLIC)
                .returns(getType())
                .addJavadoc("""
                        Read the value of the field {@code $1L}.
                        
                        @return The value of the field {@code $1L}
                        """, f.name())
                .addStatement("long _offset = getMemoryLayout().byteOffset($T.PathElement.groupElement($S))",
                        MemoryLayout.class, f.name())
                .addStatement("$T _slice = handle().asSlice(_offset, $T.getMemoryLayout())",
                        MemorySegment.class, getType())
                .addStatement("return new $T(handle().asSlice(_offset))",
                        getType())
                .build();
    }

    public MethodSpec generateWriteCopyMethod() {
        var spec = MethodSpec.methodBuilder(
                        methodName(cb != null ? OVERRIDE_PREFIX : WRITE_PREFIX))
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("""
                        Write a value in the field {@code $1L}.
                        
                        @param $2L The new value for the field {@code $1L}
                        """, f.name(), getName())
                .addParameter(getType(), getName())
                .addStatement("long _offset = getMemoryLayout().byteOffset($T.PathElement.groupElement($S))",
                        MemoryLayout.class, f.name())
                .addStatement("$T _slice = handle().asSlice(_offset, $T.getMemoryLayout())",
                        MemorySegment.class, getType());

        if (checkNull())
            spec.beginControlFlow("if ($L == null)", getName())
                    .addStatement("_slice.fill((byte) 0)")
                    .nextControlFlow("else");

        spec.addStatement("_slice.copyFrom($L.handle())", getName());

        if (checkNull())
            spec.endControlFlow();

        return spec.build();
    }

    public MethodSpec generateReadArrayMethod() {
        var calcOffset = "long _offset = getMemoryLayout().byteOffset($T.PathElement.groupElement($S))";
        var returnSlice = PartialStatement.of("return ")
                .add(marshalNativeToJava("handle().asSlice(_offset)", false))
                .add(";\n");

        return MethodSpec.methodBuilder(methodName(READ_PREFIX))
                .addModifiers(Modifier.PUBLIC)
                .returns(getType())
                .addJavadoc("""
                        Read the value of the field {@code $1L}.
                        
                        @return The value of the field {@code $1L}
                        """, f.name())
                .beginControlFlow("try ($1T _arena = $1T.ofConfined())", Arena.class)
                .addStatement(calcOffset, MemoryLayout.class, f.name())
                .addNamedCode(returnSlice.format(), returnSlice.arguments())
                .endControlFlow()
                .build();
    }

    public MethodSpec generateWriteArrayMethod() {
        var stmt = PartialStatement.of("$memorySegment:T _" + getName() + "Array = ",
                "memorySegment", MemorySegment.class)
                .add(marshalJavaToNative(getName()))
                .add(";\n");

        var spec = MethodSpec.methodBuilder(
                        methodName(cb != null ? OVERRIDE_PREFIX : WRITE_PREFIX))
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("""
                        Write a value in the field {@code $1L}.
                        
                        @param $2L The new value for the field {@code $1L}
                        """, f.name(), getName())
                .addParameter(getType(), getName())
                .addParameter(Arena.class, "_arena")
                .addStatement("$T _path = $T.PathElement.groupElement($S)",
                        MemoryLayout.PathElement.class, MemoryLayout.class, f.name())
                .addStatement("long _offset = getMemoryLayout().byteOffset(_path)")
                .addNamedCode(stmt.format(), stmt.arguments())
                .addStatement("$T _slice = handle().asSlice(_offset, getMemoryLayout().select(_path))",
                        MemorySegment.class);

        if (checkNull())
            spec.beginControlFlow("if ($L == null)", getName())
                    .addStatement("_slice.fill((byte) 0)")
                    .nextControlFlow("else");

        spec.addStatement("_slice.copyFrom(_$LArray)", getName());

        if (checkNull())
            spec.endControlFlow();

        return spec.build();
    }

    public MethodSpec generateOverrideMethod() {
        return MethodSpec.methodBuilder(methodName(OVERRIDE_PREFIX))
                .addJavadoc("""
                        Override virtual method {@code $L}.
                        
                        @param method the method to invoke
                        """, f.name())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Arena.class, "arena")
                .addParameter(java.lang.reflect.Method.class, "method")
                .addStatement("this._$LMethod = method", getName())
                .addCode(new CallableGenerator(cb)
                                .generateFunctionDescriptorDeclaration())
                .addStatement("$T _handle = $T.upcallHandle($T.lookup(), $T.class, $S, _fdesc)",
                        MethodHandle.class,
                        ClassNames.INTEROP,
                        MethodHandles.class,
                        f.parent().typeName(),
                        getName() + "Upcall")
                .addStatement("$T _address = $T.nativeLinker().upcallStub(_handle.bindTo(this), _fdesc, arena)",
                        MemorySegment.class, Linker.class)
                .addStatement("getMemoryLayout().varHandle($T.PathElement.groupElement($S))$Z"
                                + ".set(handle(), 0, (method == null ? $T.NULL : _address))",
                        MemoryLayout.class,
                        f.name(),
                        MemorySegment.class)
                .build();
    }
}
