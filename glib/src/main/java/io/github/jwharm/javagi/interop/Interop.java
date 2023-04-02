package io.github.jwharm.javagi.interop;

import java.lang.foreign.*;
import java.lang.invoke.*;
import java.util.Arrays;

import org.gnome.glib.GLib;
import org.gnome.glib.Type;
import org.gnome.gobject.GObjects;

import io.github.jwharm.javagi.base.*;

/**
 * The Interop class contains functionality for interoperability with native code.
 */
public class Interop {

    private final static SymbolLookup symbolLookup;
    private final static Linker linker = Linker.nativeLinker();

    static {
        SymbolLookup loaderLookup = SymbolLookup.loaderLookup();
        symbolLookup = name -> loaderLookup.find(name).or(() -> linker.defaultLookup().find(name));
        
        // Ensure that the "gobject-2.0" library has been loaded.
        // This is required for the downcall handle to g_signal_connect.
        GObjects.javagi$ensureInitialized();
    }

    /**
     * Returns a runtime exception in case when the API jar is used at runtime.
     * @return an InteropException
     */
    public static InteropException apiError() {
        return new InteropException("Attempted to use API jar at runtime. Configure a platform-specific jar file.");
    }

    /**
     * Throws a runtime exception in case when the API jar is used at runtime.
     */
    public static void throwApiError() {
        throw apiError();
    }

    /**
     * Get the type of a GObject instance. Comparable to the G_TYPE_FROM_INSTANCE macro in C.
     * @param address the memory address of a GObject instance
     * @return the type (GType) of the object
     */
    public static Type getType(MemorySegment address) {
        if (address == null || address.equals(MemorySegment.NULL))
            return null;

        MemorySegment g_class = address.get(ValueLayout.ADDRESS, 0);

        if (g_class == null || g_class.equals(MemorySegment.NULL))
            return null;

        long g_type = g_class.get(ValueLayout.JAVA_LONG, 0);
        return new Type(g_type);
    }

    /**
     * The method handle for g_signal_connect_data is used by all
     * generated signal-connection methods.
     */
    public static final MethodHandle g_signal_connect_data = downcallHandle(
            "g_signal_connect_data",
            FunctionDescriptor.of(
                    ValueLayout.JAVA_LONG,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_INT
            ),
            false
    );

    /**
     * The method handle for g_signal_emit_by_name is used by all
     * generated signal-emission methods.
     */
    public static final MethodHandle g_signal_emit_by_name = Interop.downcallHandle(
            "g_signal_emit_by_name",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS),
            true
    );

    /**
     * The method handle for g_type_interface_peek is used by all virtual
     * method bindings in interfaces to retrieve the interface typestruct.
     */
    public static final MethodHandle g_type_interface_peek = Interop.downcallHandle(
            "g_type_interface_peek",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG),
            false
    );

    /**
     * Creates a method handle that is used to call the native function with 
     * the provided name and function descriptor.
     * @param name Name of the native function
     * @param fdesc Function descriptor of the native function
     * @param variadic Whether the function has varargs
     * @return the MethodHandle
     */
    public static MethodHandle downcallHandle(String name, FunctionDescriptor fdesc, boolean variadic) {
        // Copied from jextract-generated code
        return symbolLookup.find(name).map(addr -> {
            return variadic ? VarargsInvoker.make(addr, fdesc) : linker.downcallHandle(addr, fdesc);
        }).orElse(null);
    }
    
    public static MethodHandle downcallHandle(MemorySegment symbol, FunctionDescriptor fdesc) {
        return linker.downcallHandle(symbol, fdesc);
    }

    /**
     * Produce a method handle for a {@code upcall} method in the provided class.
     * @param klazz the callback class
     * @param descriptor the function descriptor for the native function
     * @return a method handle to use when creating an upcall stub
     */
    public static MethodHandle upcallHandle(MethodHandles.Lookup lookup, Class<?> klazz, FunctionDescriptor descriptor) {
        try {
            return lookup.findVirtual(klazz, "upcall", descriptor.toMethodType());
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Produce a method handle for a method in the provided class.
     * @param klazz the callback class
     * @param name the name of the callback method
     * @param descriptor the function descriptor for the native function
     * @return a method handle to use when creating an upcall stub
     */
    public static MethodHandle upcallHandle(MethodHandles.Lookup lookup, Class<?> klazz, String name, FunctionDescriptor descriptor) {
        try {
            return lookup.findVirtual(klazz, name, descriptor.toMethodType());
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Allocate a native string using SegmentAllocator.allocateUtf8String(String).
     * @param string the string to allocate as a native string (utf8 char*)
     * @param allocator the segment allocator to use
     * @return the allocated MemorySegment
     */
    public static MemorySegment allocateNativeString(String string, SegmentAllocator allocator) {
        return string == null ? MemorySegment.NULL : allocator.allocateUtf8String(string);
    }
    
    /**
     * Returns a Java string from native memory using {@code MemorySegment.getUtf8String()}.
     * If an error occurs or when the native address is NULL, null is returned.
     * @param address The memory address of the native String (\0-terminated char*).
     * @param free if the address must be freed
     * @return A String or null
     */
    public static String getStringFrom(MemorySegment address, boolean free) {
        try {
            if (!MemorySegment.NULL.equals(address)) {
                return address.getUtf8String(0);
            }
        } catch (Throwable ignored) {
        } finally {
            if (free) {
                GLib.free(address);
            }
        }
        return null;
    }

    /**
     * Read an array of Strings with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @param free if the strings and the array must be freed
     * @return Array of Strings
     */
    public static String[] getStringArrayFrom(MemorySegment address, int length, boolean free) {
        String[] result = new String[length];
        for (int i = 0; i < length; i++) {
            result[i] = address.getUtf8String(i * ValueLayout.ADDRESS.byteSize());
            if (free) {
                GLib.free(address.getAtIndex(ValueLayout.ADDRESS, i));
            }
        }
        if (free) {
            GLib.free(address);
        }
        return result;
    }

    /**
     * Read an array of pointers with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @param free if the addresses and the array must be freed
     * @return Array of pointers
     */
    public static MemorySegment[] getAddressArrayFrom(MemorySegment address, int length, boolean free) {
        MemorySegment[] result = new MemorySegment[length];
        for (int i = 0; i < length; i++) {
            result[i] = address.getAtIndex(ValueLayout.ADDRESS, i);
            if (free) {
                GLib.free(address.getAtIndex(ValueLayout.ADDRESS, i));
            }
        }
        if (free) {
            GLib.free(address);
        }
        return result;
    }

    /**
     * Read an array of booleans with the given length from native memory
     * The array is read from native memory as an array of integers with value 1 or 0,
     * and converted to booleans with 1 = true and 0 = false.
     * @param address address of the memory segment
     * @param length length of the array
     * @param scope the memory scope
     * @param free if the array must be freed
     * @return array of booleans
     */
    public static boolean[] getBooleanArrayFrom(MemorySegment address, long length, SegmentScope scope, boolean free) {
        int[] intArray = getIntegerArrayFrom(address, length, scope, free);
        boolean[] array = new boolean[intArray.length];
        for (int c = 0; c < intArray.length; c++)
            array[c] = (intArray[c] != 0);
        return array;
    }

    /**
     * Read an array of bytes with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @param scope the memory scope
     * @param free if the array must be freed
     * @return array of bytes
     */
    public static byte[] getByteArrayFrom(MemorySegment address, long length, SegmentScope scope, boolean free) {
        byte[] array = MemorySegment.ofAddress(address.address(), length, scope).toArray(ValueLayout.JAVA_BYTE);
        if (free) {
            GLib.free(address);
        }
        return array;
    }

    /**
     * Read an array of chars with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @param scope the memory scope
     * @param free if the array must be freed
     * @return array of chars
     */
    public static char[] getCharacterArrayFrom(MemorySegment address, long length, SegmentScope scope, boolean free) {
        char[] array = MemorySegment.ofAddress(address.address(), length, scope).toArray(ValueLayout.JAVA_CHAR);
        if (free) {
            GLib.free(address);
        }
        return array;
    }

    /**
     * Read an array of doubles with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @param scope the memory scope
     * @param free if the array must be freed
     * @return array of doubles
     */
    public static double[] getDoubleArrayFrom(MemorySegment address, long length, SegmentScope scope, boolean free) {
        double[] array = MemorySegment.ofAddress(address.address(), length, scope).toArray(ValueLayout.JAVA_DOUBLE);
        if (free) {
            GLib.free(address);
        }
        return array;
    }

    /**
     * Read an array of floats with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @param scope the memory scope
     * @param free if the array must be freed
     * @return array of floats
     */
    public static float[] getFloatArrayFrom(MemorySegment address, long length, SegmentScope scope, boolean free) {
        float[] array = MemorySegment.ofAddress(address.address(), length, scope).toArray(ValueLayout.JAVA_FLOAT);
        if (free) {
            GLib.free(address);
        }
        return array;
    }

    /**
     * Read an array of integers with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @param scope the memory scope
     * @param free if the array must be freed
     * @return array of integers
     */
    public static int[] getIntegerArrayFrom(MemorySegment address, long length, SegmentScope scope, boolean free) {
        int[] array = MemorySegment.ofAddress(address.address(), length, scope).toArray(ValueLayout.JAVA_INT);
        if (free) {
            GLib.free(address);
        }
        return array;
    }

    /**
     * Read an array of longs with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @param scope the memory scope
     * @param free if the array must be freed
     * @return array of longs
     */
    public static long[] getLongArrayFrom(MemorySegment address, long length, SegmentScope scope, boolean free) {
        long[] array = MemorySegment.ofAddress(address.address(), length, scope).toArray(ValueLayout.JAVA_LONG);
        if (free) {
            GLib.free(address);
        }
        return array;
    }

    /**
     * Read an array of shorts with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @param scope the memory scope
     * @param free if the array must be freed
     * @return array of shorts
     */
    public static short[] getShortArrayFrom(MemorySegment address, long length, SegmentScope scope, boolean free) {
        short[] array = MemorySegment.ofAddress(address.address(), length, scope).toArray(ValueLayout.JAVA_SHORT);
        if (free) {
            GLib.free(address);
        }
        return array;
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array 
     * of strings (NUL-terminated utf8 char*).
     * @param strings Array of Strings
     * @param zeroTerminated Whether to add a NUL at the end the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(String[] strings, boolean zeroTerminated, SegmentAllocator allocator) {
        int length = zeroTerminated ? strings.length + 1 : strings.length;
        var memorySegment = allocator.allocateArray(ValueLayout.ADDRESS, length);
        for (int i = 0; i < strings.length; i++) {
            var cString = strings[i] == null ? MemorySegment.NULL : allocator.allocateUtf8String(strings[i]);
            memorySegment.setAtIndex(ValueLayout.ADDRESS, i, cString);
        }
        if (zeroTerminated) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, strings.length, MemorySegment.NULL);
        }
        return memorySegment;
    }

    /**
     * Converts the boolean[] array into an int[] array, and calls {@link #allocateNativeArray(int[], boolean, SegmentAllocator)}.
     * Each boolean value "true" is converted 1, boolean value "false" to 0.
     * @param array Array of booleans
     * @param zeroTerminated When true, an (int) 0 is appended to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(boolean[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        int[] intArray = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            intArray[i] = array[i] ? 1 : 0;
        }
        return allocateNativeArray(intArray, zeroTerminated, allocator);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of bytes.
     * @param array The array of bytes
     * @param zeroTerminated When true, a (byte) 0 is appended to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(byte[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        byte[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return allocator.allocateArray(ValueLayout.JAVA_BYTE, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of chars.
     * @param array The array of chars
     * @param zeroTerminated When true, a (char) 0 is appended to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(char[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        char[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return allocator.allocateArray(ValueLayout.JAVA_CHAR, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of doubles.
     * @param array The array of doubles
     * @param zeroTerminated When true, a (double) 0 is appended to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(double[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        double[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return allocator.allocateArray(ValueLayout.JAVA_DOUBLE, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of floats.
     * @param array The array of floats
     * @param zeroTerminated When true, a (float) 0 is appended to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(float[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        float[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return allocator.allocateArray(ValueLayout.JAVA_FLOAT, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of floats.
     * @param array The array of floats
     * @param zeroTerminated When true, a (int) 0 is appended to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(int[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        int[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return allocator.allocateArray(ValueLayout.JAVA_INT, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of longs.
     * @param array The array of longs
     * @param zeroTerminated When true, a (long) 0 is appended to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(long[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        long[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return allocator.allocateArray(ValueLayout.JAVA_LONG, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of shorts.
     * @param array The array of shorts
     * @param zeroTerminated When true, a (short) 0 is appended to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(short[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        short[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return allocator.allocateArray(ValueLayout.JAVA_SHORT, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array 
     * of pointers (from Proxy instances).
     * @param array The array of Proxy instances
     * @param zeroTerminated Whether to add an additional NUL to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(Proxy[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        MemorySegment[] addressArray = new MemorySegment[array.length];
        for (int i = 0; i < array.length; i++) {
            addressArray[i] = array[i] == null ? MemorySegment.NULL : array[i].handle();
        }
        return allocateNativeArray(addressArray, zeroTerminated, allocator);
    }
    
    /**
     * Allocates and initializes an (optionally NULL-terminated) array
     * of structs (from Proxy instances). The actual memory segments (not 
     * the pointers) are copied into the array.
     * @param array The array of Proxy instances
     * @param layout The memory layout of the object type
     * @param zeroTerminated Whether to add an additional NUL to the array
     * @param allocator the allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(Proxy[] array, MemoryLayout layout, boolean zeroTerminated, SegmentAllocator allocator) {
        int length = zeroTerminated ? array.length + 1 : array.length;
        MemorySegment memorySegment = allocator.allocateArray(layout, length);
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null) {
                MemorySegment element = MemorySegment.ofAddress(array[i].handle().address(), layout.byteSize(), memorySegment.scope());
                memorySegment.asSlice(i * layout.byteSize()).copyFrom(element);
            } else {
                memorySegment.asSlice(i * layout.byteSize(), layout.byteSize()).fill((byte) 0);
            }
        }
        if (zeroTerminated) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, array.length, MemorySegment.NULL);
        }
        return memorySegment;
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array 
     * of memory addresses.
     * @param array The array of addresses
     * @param zeroTerminated Whether to add an additional NUL to the array
     * @param allocator the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(MemorySegment[] array, boolean zeroTerminated, SegmentAllocator allocator) {
        int length = zeroTerminated ? array.length + 1 : array.length;
        var memorySegment = allocator.allocateArray(ValueLayout.ADDRESS, length);
        for (int i = 0; i < array.length; i++) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, i, array[i] == null ? MemorySegment.NULL : array[i]);
        }
        if (zeroTerminated) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, array.length, MemorySegment.NULL);
        }
        return memorySegment;
    }

    // Adapted from code that was generated by jextract
    private static class VarargsInvoker {
        private static final MethodHandle INVOKE_MH;
        private final MemorySegment symbol;
        private final FunctionDescriptor function;
        private static final SegmentAllocator THROWING_ALLOCATOR = (x, y) -> { throw new AssertionError("should not reach here"); };

        private VarargsInvoker(MemorySegment symbol, FunctionDescriptor function) {
            this.symbol = symbol;
            this.function = function;
        }

        static {
            try {
                INVOKE_MH = MethodHandles.lookup().findVirtual(VarargsInvoker.class, "invoke", MethodType.methodType(Object.class, SegmentAllocator.class, Object[].class));
            } catch (ReflectiveOperationException e) {
                throw new InteropException(e);
            }
        }

        static MethodHandle make(MemorySegment symbol, FunctionDescriptor function) {
            VarargsInvoker invoker = new VarargsInvoker(symbol, function);
            MethodHandle handle = INVOKE_MH.bindTo(invoker).asCollector(Object[].class, function.argumentLayouts().size() + 1);
            MethodType mtype = MethodType.methodType(function.returnLayout().isPresent() ? carrier(function.returnLayout().get(), true) : void.class);
            for (MemoryLayout layout : function.argumentLayouts()) {
                mtype = mtype.appendParameterTypes(carrier(layout, false));
            }
            mtype = mtype.appendParameterTypes(Object[].class);
            if (mtype.returnType().equals(MemorySegment.class)) {
                mtype = mtype.insertParameterTypes(0, SegmentAllocator.class);
            } else {
                handle = MethodHandles.insertArguments(handle, 0, THROWING_ALLOCATOR);
            }
            return handle.asType(mtype);
        }

        static Class<?> carrier(MemoryLayout layout, boolean ret) {
            if (layout instanceof ValueLayout valLayout) {
                return (ret || valLayout.carrier() != MemorySegment.class) ?
                        valLayout.carrier() : MemorySegment.class;
            } else if (layout instanceof GroupLayout) {
                return MemorySegment.class;
            } else {
                throw new AssertionError("Cannot get here!");
            }
        }

        // This method is used from a MethodHandle (INVOKE_MH).
        @SuppressWarnings("unused")
        private Object invoke(SegmentAllocator allocator, Object[] args) throws Throwable {
            // one trailing Object[]
            int nNamedArgs = function.argumentLayouts().size();
            assert(args.length == nNamedArgs + 1);
            // The last argument is the array of vararg collector
            Object[] unnamedArgs = (Object[]) args[args.length - 1];

            int argsCount = nNamedArgs + unnamedArgs.length;
            MemoryLayout[] argLayouts = new MemoryLayout[nNamedArgs + unnamedArgs.length];

            int pos = 0;
            for (pos = 0; pos < nNamedArgs; pos++) {
                argLayouts[pos] = function.argumentLayouts().get(pos);
            }
            
            // Unwrap the java-gi types to their memory address or primitive value
            Object[] unwrappedArgs = new Object[unnamedArgs.length];
            for (int i = 0; i < unnamedArgs.length; i++) {
                unwrappedArgs[i] = unwrapJavagiTypes(unnamedArgs[i]);
            }

            assert pos == nNamedArgs;
            for (Object o: unwrappedArgs) {
                argLayouts[pos] = variadicLayout(normalize(o.getClass()));
                pos++;
            }
            assert pos == argsCount;

            FunctionDescriptor f = (function.returnLayout().isEmpty()) ?
                    FunctionDescriptor.ofVoid(argLayouts) :
                    FunctionDescriptor.of(function.returnLayout().get(), argLayouts);
            MethodHandle mh = linker.downcallHandle(symbol, f);
            if (mh.type().returnType() == MemorySegment.class) {
                mh = mh.bindTo(allocator);
            }
            // flatten argument list so that it can be passed to an asSpreader MH
            Object[] allArgs = new Object[nNamedArgs + unwrappedArgs.length];
            System.arraycopy(args, 0, allArgs, 0, nNamedArgs);
            System.arraycopy(unwrappedArgs, 0, allArgs, nNamedArgs, unwrappedArgs.length);

            return mh.asSpreader(Object[].class, argsCount).invoke(allArgs);
        }

        private static Class<?> unboxIfNeeded(Class<?> clazz) {
            if (clazz == Boolean.class) {
                return boolean.class;
            } else if (clazz == Void.class) {
                return void.class;
            } else if (clazz == Byte.class) {
                return byte.class;
            } else if (clazz == Character.class) {
                return char.class;
            } else if (clazz == Short.class) {
                return short.class;
            } else if (clazz == Integer.class) {
                return int.class;
            } else if (clazz == Long.class) {
                return long.class;
            } else if (clazz == Float.class) {
                return float.class;
            } else if (clazz == Double.class) {
                return double.class;
            } else {
                return clazz;
            }
        }

        private Class<?> promote(Class<?> c) {
            if (c == byte.class || c == char.class || c == short.class || c == int.class) {
                return long.class;
            } else if (c == float.class) {
                return double.class;
            } else {
                return c;
            }
        }

        private Class<?> normalize(Class<?> c) {
            c = unboxIfNeeded(c);
            if (c.isPrimitive()) {
                return promote(c);
            }
            if (MemorySegment.class.isAssignableFrom(c)) {
                return MemorySegment.class;
            }
            throw new IllegalArgumentException("Invalid type for ABI: " + c.getTypeName());
        }

        private MemoryLayout variadicLayout(Class<?> c) {
            if (c == long.class) {
                return ValueLayout.JAVA_LONG;
            } else if (c == double.class) {
                return ValueLayout.JAVA_DOUBLE;
            } else if (MemorySegment.class.isAssignableFrom(c)) {
                return ValueLayout.ADDRESS;
            } else {
                throw new IllegalArgumentException("Unhandled variadic argument class: " + c);
            }
        }
        
        // Unwrap the java-gi types to their memory address or primitive value.
        // Arrays are allocated to native memory as-is (no additional NUL is appended: the caller must do this)
        private Object unwrapJavagiTypes(Object o) {
            if (o == null) {
                return MemorySegment.NULL;
            }
            if (o instanceof MemorySegment[] addresses) {
                return Interop.allocateNativeArray(addresses, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof Boolean bool) {
                return bool ? 1 : 0;
            }
            if (o instanceof boolean[] values) {
                return Interop.allocateNativeArray(values, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof byte[] values) {
                return Interop.allocateNativeArray(values, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof char[] values) {
                return Interop.allocateNativeArray(values, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof double[] values) {
                return Interop.allocateNativeArray(values, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof float[] values) {
                return Interop.allocateNativeArray(values, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof int[] values) {
                return Interop.allocateNativeArray(values, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof long[] values) {
                return Interop.allocateNativeArray(values, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof short[] values) {
                return Interop.allocateNativeArray(values, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof Proxy proxy) {
                return proxy.handle();
            }
            if (o instanceof Proxy[] proxys) {
                return Interop.allocateNativeArray(proxys, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof Alias<?> alias) {
                return alias.getValue();
            }
            if (o instanceof Bitfield bitfield) {
                return bitfield.getValue();
            }
            if (o instanceof Bitfield[] bitfields) {
                return Bitfield.getValues(bitfields);
            }
            if (o instanceof Enumeration enumeration) {
                return enumeration.getValue();
            }
            if (o instanceof Enumeration[] enumerations) {
                return Enumeration.getValues(enumerations);
            }
            if (o instanceof java.lang.String string) {
                return Interop.allocateNativeString(string, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            if (o instanceof java.lang.String[] strings) {
                return Interop.allocateNativeArray(strings, false, SegmentAllocator.nativeAllocator(SegmentScope.auto())).address();
            }
            return o;
        }
    }
}
