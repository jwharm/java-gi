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
import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.util.PartialStatement;

import javax.lang.model.element.Modifier;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static io.github.jwharm.javagi.util.Conversions.toCamelCase;
import static io.github.jwharm.javagi.util.Conversions.toJavaIdentifier;

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
        for (GirElement node : f.parent().children()) {
            if (node instanceof AbstractCallable ct && methodName.equals(toJavaIdentifier(ct.name()))) {
                methodName += "_";
                break;
            }
        }
        return methodName;
    }

    public MethodSpec generateReadMethod() {
        // To read from ...** fields, you must provide the length of the array.
        boolean isArray = type != null && type.isActuallyAnArray();
        var spec = MethodSpec.methodBuilder(methodName(READ_PREFIX))
                .addModifiers(Modifier.PUBLIC)
                .returns(getType())
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
        if ((type != null) && (!type.isPointer()) && (target instanceof Class || target instanceof Interface)) {
            var stmt = PartialStatement.of("return ").add(marshalNativeToJava("handle().asSlice(_offset)", false));
            return spec.addStatement("long _offset = getMemoryLayout().byteOffset(MemoryLayout.PathElement.groupElement($S))", f.name())
                    .addNamedCode(stmt.format() + ";\n", stmt.arguments())
                    .build();
        }

        // Read a pointer or primitive value from the struct
        String memoryType = f.getMemoryType();
        if ("ARRAY".equals(memoryType)) memoryType = "java.lang.foreign.MemorySegment";
        var stmt = PartialStatement.of("return ").add(marshalNativeToJava("_result", false));
        return spec.addStatement("var _result = ($L) getMemoryLayout()$Z.varHandle($T.PathElement.groupElement($S)).get(handle())",
                        memoryType, MemoryLayout.class, f.name())
                .addNamedCode(stmt.format() + ";\n", stmt.arguments())
                .build();
    }

    public MethodSpec generateWriteMethod() {
        var spec = MethodSpec.methodBuilder(methodName(cb != null ? OVERRIDE_PREFIX : WRITE_PREFIX))
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("""
                        Write a value in the field {@code $1L}.
                        
                        @param $2L The new value for the field {@code $1L}
                        """, f.name(), getName());
        if (f.allocatesMemory())
            spec.addParameter(Arena.class, "_arena");
        spec.addParameter(getType(), getName());
        PartialStatement stmt = marshalJavaToNative(getName())
                .add(null,
                        "memoryLayout", MemoryLayout.class,
                        "memorySegment", MemorySegment.class,
                        "fieldName", f.name());
        if (checkNull())
            spec.addNamedCode("getMemoryLayout().varHandle($memoryLayout:T.PathElement.groupElement($fieldName:S))$Z"
                            + ".set(handle(), (" + getName() + " == null ? $memorySegment:T.NULL : " + stmt.format() + "));\n",
                    stmt.arguments());
        else
            spec.addNamedCode("getMemoryLayout().varHandle($memoryLayout:T.PathElement.groupElement($fieldName:S))$Z"
                    + ".set(handle(), " + stmt.format() + ");\n", stmt.arguments());
        return spec.build();
    }

    public MethodSpec generateOverrideMethod() {
        var spec = MethodSpec.methodBuilder(methodName(OVERRIDE_PREFIX))
                .addJavadoc("""
                        Override virtual method {@code $L}.
                                                
                        @param method the method to invoke
                        """, f.name())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Arena.class, "arena")
                .addParameter(java.lang.reflect.Method.class, "method")
                .addStatement("this._$LMethod = method", getName());
        new CallableGenerator(cb).generateFunctionDescriptor(spec);
        return spec.addStatement("$T _handle = $T.upcallHandle($T.lookup(), $T.class, $S, _fdesc)",
                        MethodHandle.class, ClassNames.INTEROP, MethodHandles.class, f.parent().typeName(), getName() + "Upcall")
                .addStatement("$T _address = $T.nativeLinker().upcallStub(_handle.bindTo(this), _fdesc, arena)",
                        MemorySegment.class, Linker.class)
                .addStatement("getMemoryLayout().varHandle($T.PathElement.groupElement($S))$Z.set(handle(), (method == null ? $T.NULL : _address))",
                        MemoryLayout.class, f.name(), MemorySegment.class)
                .build();
    }
}
