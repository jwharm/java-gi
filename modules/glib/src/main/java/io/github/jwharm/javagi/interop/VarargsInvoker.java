/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 Jan-Willem Harmannij
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

package io.github.jwharm.javagi.interop;

import io.github.jwharm.javagi.base.Alias;
import io.github.jwharm.javagi.base.Enumeration;
import io.github.jwharm.javagi.base.Proxy;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static io.github.jwharm.javagi.interop.Interop.*;

/**
 * Generate a MethodHandle for a variadic function.
 */
record VarargsInvoker(MemorySegment symbol, FunctionDescriptor function) {

    private static final MethodHandle METHOD_HANDLE;
    private static final SegmentAllocator THROW = (_, _) -> {
        throw new AssertionError("should not reach here");
    };

    static {
        try {
            METHOD_HANDLE = MethodHandles.lookup().findVirtual(
                    VarargsInvoker.class,
                    "invoke",
                    MethodType.methodType(Object.class,
                            SegmentAllocator.class,
                            Object[].class)
            );
        } catch (ReflectiveOperationException e) {
            throw new InteropException(e);
        }
    }

    /**
     * Create a MethodHandle with the correct signature
     */
    static MethodHandle create(MemorySegment symbol,
                               FunctionDescriptor function) {
        VarargsInvoker invoker = new VarargsInvoker(symbol, function);
        int arrayLength = function.argumentLayouts().size() + 1;
        MethodHandle handle = METHOD_HANDLE.bindTo(invoker)
                .asCollector(Object[].class, arrayLength);

        MethodType mtype = MethodType.methodType(
                function.returnLayout().isPresent()
                        ? carrier(function.returnLayout().get(), true)
                        : void.class);
        for (MemoryLayout layout : function.argumentLayouts()) {
            mtype = mtype.appendParameterTypes(carrier(layout, false));
        }
        mtype = mtype.appendParameterTypes(Object[].class);

        boolean needsAllocator = function.returnLayout().isPresent()
                && function.returnLayout().get() instanceof GroupLayout;

        if (needsAllocator)
            mtype = mtype.insertParameterTypes(0, SegmentAllocator.class);
        else
            handle = MethodHandles.insertArguments(handle, 0, THROW);

        return handle.asType(mtype);
    }

    /*
     * Get the carrier associated with this layout. For GroupLayouts and the
     * return layout, the carrier is always MemorySegment.class.
     */
    private static Class<?> carrier(MemoryLayout layout, boolean ret) {
        if (layout instanceof ValueLayout valLayout)
            return (ret || valLayout.carrier() != MemorySegment.class)
                    ? valLayout.carrier()
                    : MemorySegment.class;
        else if (layout instanceof GroupLayout)
            return MemorySegment.class;
        throw new AssertionError("Cannot get here!");
    }

    /*
     * This method is used from a MethodHandle (INVOKE_MH).
     */
    @SuppressWarnings("unused")
    private Object invoke(SegmentAllocator allocator, Object[] args)
            throws Throwable {

        // one trailing Object[]
        int nNamedArgs = function.argumentLayouts().size();
        // The last argument is the array of vararg collector
        Object[] unnamedArgs = (Object[]) args[args.length - 1];

        int argsCount = nNamedArgs + unnamedArgs.length;
        MemoryLayout[] argLayouts = new MemoryLayout[argsCount];

        int pos;
        for (pos = 0; pos < nNamedArgs; pos++) {
            argLayouts[pos] = function.argumentLayouts().get(pos);
        }

        // Unwrap the Java-GI types to their address or primitive value
        Object[] unwrappedArgs = new Object[unnamedArgs.length];
        for (int i = 0; i < unnamedArgs.length; i++) {
            unwrappedArgs[i] = unwrapJavagiTypes(unnamedArgs[i]);
        }

        for (Object o : unwrappedArgs) {
            argLayouts[pos] = variadicLayout(normalize(o.getClass()));
            pos++;
        }

        FunctionDescriptor f = function.returnLayout().map(
                layout -> FunctionDescriptor.of(layout, argLayouts)).orElseGet(
                ()     -> FunctionDescriptor.ofVoid(argLayouts));
        MethodHandle mh = Interop.downcallHandle(symbol, f);
        boolean needsAllocator = function.returnLayout().isPresent()
                && function.returnLayout().get() instanceof GroupLayout;

        if (needsAllocator)
            mh = mh.bindTo(allocator);

        /*
         * Flatten argument list so that it can be passed to an asSpreader
         * MethodHandle.
         */
        int length = unwrappedArgs.length;
        Object[] allArgs = new Object[nNamedArgs + length];
        System.arraycopy(args, 0, allArgs, 0, nNamedArgs);
        System.arraycopy(unwrappedArgs, 0, allArgs, nNamedArgs, length);

        return mh.asSpreader(Object[].class, argsCount).invoke(allArgs);
    }

    private static Class<?> unboxIfNeeded(Class<?> c) {
        if (c == Boolean.class)   return boolean.class;
        if (c == Void.class)      return void.class;
        if (c == Byte.class)      return byte.class;
        if (c == Character.class) return char.class;
        if (c == Short.class)     return short.class;
        if (c == Integer.class)   return int.class;
        if (c == Long.class)      return long.class;
        if (c == Float.class)     return float.class;
        if (c == Double.class)    return double.class;
        return c;
    }

    private Class<?> promote(Class<?> c) {
        if (c == byte.class
                || c == char.class
                || c == short.class
                || c == int.class)
            return long.class;
        if (c == float.class)
            return double.class;
        return c;
    }

    private Class<?> normalize(Class<?> c) {
        c = unboxIfNeeded(c);
        if (c.isPrimitive())
            return promote(c);
        if (MemorySegment.class.isAssignableFrom(c))
            return MemorySegment.class;
        throw new IllegalArgumentException("Invalid type for ABI: " + c.getTypeName());
    }

    private MemoryLayout variadicLayout(Class<?> c) {
        if (c == long.class)
            return ValueLayout.JAVA_LONG;
        if (c == double.class)
            return ValueLayout.JAVA_DOUBLE;
        if (MemorySegment.class.isAssignableFrom(c))
            return ValueLayout.ADDRESS;
        throw new IllegalArgumentException("Unhandled variadic argument class: " + c);
    }

    /*
     * Unwrap the Java-GI types to their memory address or primitive value.
     * Arrays are allocated to native memory as-is (no additional NULL is
     * appended: the caller must do this).
     */
    private Object unwrapJavagiTypes(Object o) {
        return switch(o) {
            case null -> MemorySegment.NULL;
            case MemorySegment[] arr ->
                    allocateNativeArray(arr, false, Arena.ofAuto()).address();
            case boolean[] arr ->
                    allocateNativeArray(arr, false, Arena.ofAuto()).address();
            case byte[] arr ->
                    allocateNativeArray(arr, false, Arena.ofAuto()).address();
            case char[] arr ->
                    allocateNativeArray(arr, false, Arena.ofAuto()).address();
            case double[] arr ->
                    allocateNativeArray(arr, false, Arena.ofAuto()).address();
            case float[] arr ->
                    allocateNativeArray(arr, false, Arena.ofAuto()).address();
            case int[] arr ->
                    allocateNativeArray(arr, false, Arena.ofAuto()).address();
            case long[] arr ->
                    allocateNativeArray(arr, false, Arena.ofAuto()).address();
            case short[] arr ->
                    allocateNativeArray(arr, false, Arena.ofAuto()).address();
            case Proxy[] arr ->
                    allocateNativeArray(arr, false, Arena.ofAuto()).address();
            case String[] arr ->
                    allocateNativeArray(arr, false, Arena.ofAuto()).address();
            case Boolean bool ->
                    bool ? 1 : 0;
            case String string ->
                    allocateNativeString(string, Arena.ofAuto()).address();
            case Alias<?> alias ->
                    alias.getValue();
            case Enumeration enumeration ->
                    enumeration.getValue();
            case Enumeration[] enumerations ->
                    getValues(enumerations);
            case Proxy proxy ->
                    proxy.handle();
            default -> o;
        };
    }
}
