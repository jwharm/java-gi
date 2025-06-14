/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 Jan-Willem Harmannij
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

package org.javagi.interop;

import org.javagi.base.Alias;
import org.javagi.base.Enumeration;
import org.javagi.base.Proxy;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Set;

import static org.javagi.interop.Interop.*;

/**
 * Generate a MethodHandle for a variadic function.
 */
record VarargsInvoker(MemorySegment symbol, FunctionDescriptor fdesc) {

    private static final MethodHandle METHOD_HANDLE;

    static {
        try {
            METHOD_HANDLE = MethodHandles.lookup().findVirtual(
                    VarargsInvoker.class,
                    "invoke",
                    MethodType.methodType(Object.class, Object[].class)
            );
        } catch (ReflectiveOperationException e) {
            throw new InteropException(e);
        }
    }

    /**
     * Create a MethodHandle with the base parameters and a placeholder for the
     * varargs.
     */
    static MethodHandle create(MemorySegment symbol,
                               FunctionDescriptor baseDesc) {
        VarargsInvoker invoker = new VarargsInvoker(symbol, baseDesc);
        MethodHandle handle = METHOD_HANDLE.bindTo(invoker)
                .asVarargsCollector(Object[].class);

        MethodType mtype = MethodType.methodType(
                baseDesc.returnLayout().isPresent()
                        ? carrier(baseDesc.returnLayout().get(), true)
                        : void.class);
        for (MemoryLayout layout : baseDesc.argumentLayouts())
            mtype = mtype.appendParameterTypes(carrier(layout, false));

        mtype = mtype.appendParameterTypes(Object[].class);
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
     * Invoked by METHOD_HANDLE.
     */
    @SuppressWarnings("unused")
    private Object invoke(Object[] args) throws Throwable {
        // The last argument is the array of vararg collector
        Object[] varargs = (Object[]) args[args.length - 1];

        // number of fixed and variable arguments
        int nNamedArgs = fdesc.argumentLayouts().size();
        int nVarargs = varargs.length;

        // create array of memory layouts
        int argsCount = nNamedArgs + nVarargs;
        MemoryLayout[] argLayouts = new MemoryLayout[argsCount];

        // Fill in the named memory layouts
        int pos;
        for (pos = 0; pos < nNamedArgs; pos++)
            argLayouts[pos] = fdesc.argumentLayouts().get(pos);

        /*
         * Create a memory allocation arena for marshaling Java arrays to native
         * arrays. The memory will be deallocated immediately after the function
         * call returned.
         */
        try (var arena = Arena.ofConfined()) {

            // Marshal the Java-GI types to a pointer or primitive value
            Object[] marshaledVarargs = new Object[nVarargs];
            for (int i = 0; i < nVarargs; i++)
                marshaledVarargs[i] = marshalArgument(varargs[i], arena);

            // Fill in the varargs memory layouts
            for (Object o : marshaledVarargs) {
                argLayouts[pos] = variadicLayout(o.getClass());
                pos++;
            }

            // Create the function descriptor
            FunctionDescriptor f = fdesc.returnLayout().map(
                    layout -> FunctionDescriptor.of(layout, argLayouts)).orElseGet(
                    ()     -> FunctionDescriptor.ofVoid(argLayouts));
            Linker.Option fva = Linker.Option.firstVariadicArg(nNamedArgs);
            MethodHandle mh = Interop.downcallHandle(symbol, f, fva);

            // Flatten the fixed and variadic arguments in one array
            Object[] allArgs = new Object[argsCount];
            System.arraycopy(args, 0, allArgs, 0, nNamedArgs);
            System.arraycopy(marshaledVarargs, 0, allArgs, nNamedArgs, nVarargs);

            // Create a handle that spreads the array into positional arguments
            var spreader = mh.asSpreader(Object[].class, argsCount);

            // Invoke the handle
            return spreader.invoke(allArgs);
        }
    }

    /*
     * Apply default argument promotions per C spec. Note that all primitives
     * are boxed, since they are passed through an Object[].
     */
    private static MemoryLayout variadicLayout(Class<?> c) {
        if (c == Boolean.class || c == Byte.class || c == Character.class
                || c == Short.class || c == Integer.class)
            return ValueLayout.JAVA_INT;

        if (c == Long.class)
            return ValueLayout.JAVA_LONG;

        if (c == Float.class || c == Double.class)
            return ValueLayout.JAVA_DOUBLE;

        if (MemorySegment.class.isAssignableFrom(c))
            return ValueLayout.ADDRESS;

        throw new InteropException("Unsupported variadic argument type: "
                + c.getTypeName());
    }

    /*
     * Marshal the Java-GI types to their memory address or primitive value.
     * Arrays are allocated to native memory as-is (no additional NULL is
     * appended: the caller must do this).
     */
    private Object marshalArgument(Object o, Arena arena) {
        return switch(o) {
            case null ->
                    MemorySegment.NULL;
            case MemorySegment[] arr ->
                    allocateNativeArray(arr, false, arena).address();
            case boolean[] arr ->
                    allocateNativeArray(arr, false, arena).address();
            case byte[] arr ->
                    allocateNativeArray(arr, false, arena).address();
            case char[] arr ->
                    allocateNativeArray(arr, false, arena).address();
            case double[] arr ->
                    allocateNativeArray(arr, false, arena).address();
            case float[] arr ->
                    allocateNativeArray(arr, false, arena).address();
            case int[] arr ->
                    allocateNativeArray(arr, false, arena).address();
            case long[] arr ->
                    allocateNativeArray(arr, false, arena).address();
            case short[] arr ->
                    allocateNativeArray(arr, false, arena).address();
            case Proxy[] arr ->
                    allocateNativeArray(arr, false, arena).address();
            case String[] arr ->
                    allocateNativeArray(arr, false, arena).address();
            case Boolean bool ->
                    bool ? 1 : 0;
            case String string ->
                    allocateNativeString(string, arena).address();
            case Alias<?> alias ->
                    alias.getValue();
            case Set<?> flags ->
                    enumSetToInt(flags);
            case Enumeration enumeration ->
                    enumeration.getValue();
            case Enumeration[] enumerations ->
                    getValues(enumerations);
            case Proxy proxy ->
                    proxy.handle();
            case Byte _, Character _, Double _, Float _, Integer _, Short _, Long _ ->
                    o;
            default ->
                    throw new InteropException("Unsupported variadic argument type: "
                            + o.getClass().getTypeName());
        };
    }

    /*
     * The only type of Set we can marshal to a native function parameter, is a
     * Set<Enum & Enumeration>. Any other Set argument will throw an exception.
     */
    private static int enumSetToInt(Set<?> set) {
        int bitfield = 0;
        for (var element : set)
            if (element instanceof Enum<?> && element instanceof Enumeration e)
                bitfield |= e.getValue();
            else
                throw new InteropException("Unsupported variadic argument type: Set of "
                        + element.getClass().getTypeName());
        return bitfield;
    }
}
