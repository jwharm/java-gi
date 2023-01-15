package io.github.jwharm.javagi;

import org.gtk.glib.Type;
import org.gtk.gobject.GObjects;

import java.lang.foreign.*;
import java.lang.invoke.*;
import java.util.*;

/**
 * The Interop class contains functionality for interoperability with native code.
 */
public class Interop {

    private final static SymbolLookup symbolLookup;
    private final static Linker linker = Linker.nativeLinker();
    private final static Map<Type, Marshal<Addressable, ? extends Proxy>> typeRegister;

    /**
     * Configure the layout of native data types here.<br>
     * On Linux, this should be set to {@link Layout_LP64}.<br>
     * On Windows, this should be set to {@link Layout_LLP64}.
     * @see <a href="https://en.wikipedia.org/wiki/64-bit_computing#64-bit_data_models">
     *     this Wikipedia text</a> about the difference between 64-bit data models.
     */
    public static final Layout_LP64 valueLayout = new Layout_LP64();

    static {
        typeRegister = new HashMap<>();
        SymbolLookup loaderLookup = SymbolLookup.loaderLookup();
        symbolLookup = name -> loaderLookup.lookup(name).or(() -> linker.defaultLookup().lookup(name));
        
        // Ensure that the "gobject-2.0" library has been loaded.
        // This is required for the downcall handle to g_signal_connect.
        GObjects.javagi$ensureInitialized();
    }

    /**
     * Get the type of a GObject instance. Comparable to the G_TYPE_FROM_INSTANCE macro in C.
     * @param address the memory address of a GObject instance
     * @return the type (GType) of the object
     */
    public static Type getType(MemoryAddress address) {
        MemoryAddress g_class = address.get(Interop.valueLayout.ADDRESS, 0);
        long g_type = g_class.get(Interop.valueLayout.C_LONG, 0);
        return new Type(g_type);
    }

    /**
     * Get the marshal function from the type registry. If it is not found, register the provided
     * fallback marshal for this type, and return it.
     * @param address Address of Proxy object to obtain the type from
     * @param fallback Marshal function to use, if not found in the type register
     * @return the marshal function
     */
    public static Marshal<Addressable, ? extends Proxy> register(MemoryAddress address, Marshal<Addressable, ? extends Proxy> fallback) {
        if (address.equals(MemoryAddress.NULL)) return fallback;
        Type type = getType(address);
        typeRegister.putIfAbsent(type, fallback);
        return typeRegister.get(type);
    }

    /**
     * Register the provided marshal function for the provided type
     * @param type Type to use as key in the type register
     * @param marshal Marshal function for this type
     */
    public static void register(Type type, Marshal<Addressable, ? extends Proxy> marshal) {
        typeRegister.put(type, marshal);
    }

    /**
     * The method handle for g_signal_connect_data is used by all
     * generated signal methods.
     */
    public static final MethodHandle g_signal_connect_data = downcallHandle(
            "g_signal_connect_data",
            FunctionDescriptor.of(
                    valueLayout.C_LONG,
                    valueLayout.ADDRESS,
                    valueLayout.ADDRESS,
                    valueLayout.ADDRESS,
                    valueLayout.ADDRESS,
                    valueLayout.ADDRESS,
                    valueLayout.C_INT
            ),
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
        return symbolLookup.lookup(name).map(addr -> {
            return variadic ? VarargsInvoker.make(addr, fdesc) : linker.downcallHandle(addr, fdesc);
        }).orElse(null);
    }

    /**
     * Allocate a native string using SegmentAllocator.allocateUtf8String(String).
     * @param string the string to allocate as a native string (utf8 char*)
     * @param scope the segment scope for memory allocation
     * @return the allocated MemorySegment
     */
    public static Addressable allocateNativeString(String string, MemorySession scope) {
        return string == null ? MemoryAddress.NULL : scope.allocateUtf8String(string);
    }
    
    /**
     * Returns a Java string from native memory using {@code MemoryAddress.getUtf8String()}.
     * If an error occurs or when the native address is NULL, null is returned.
     * @param address The memory address of the native String (\0-terminated char*).
     * @return A String or null
     */
    public static String getStringFrom(MemoryAddress address) {
        try {
            if (!MemoryAddress.NULL.equals(address)) {
                return address.getUtf8String(0);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Read an array of Strings with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @return Array of Strings
     */
    public static String[] getStringArrayFrom(MemoryAddress address, int length) {
        String[] result = new String[length];
        for (int i = 0; i < length; i++)
            result[i] = address.getUtf8String(i * valueLayout.ADDRESS.byteSize());
        return result;
    }

    /**
     * Read an array of pointers with the given length from native memory
     * @param address address of the memory segment
     * @param length length of the array
     * @return Array of pointers
     */
    public static MemoryAddress[] getAddressArrayFrom(MemoryAddress address, int length) {
        MemoryAddress[] result = new MemoryAddress[length];
        for (int i = 0; i < length; i++)
            result[i] = address.getAtIndex(Interop.valueLayout.ADDRESS, i);
        return result;
    }

    /**
     * Produce a method handle for a {@code upcall} method in the provided class.
     * @param klazz the callback class
     * @param descriptor the function descriptor for the native function
     * @return a method handle to use when creating an upcall stub
     */
    public static MethodHandle getHandle(MethodHandles.Lookup lookup, Class<?> klazz, FunctionDescriptor descriptor) {
        try {
            return lookup.findVirtual(klazz, "upcall", Linker.upcallType(descriptor));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array 
     * of strings (NUL-terminated utf8 char*).
     * @param strings Array of Strings
     * @param zeroTerminated Whether to add a NUL at the end the array
     * @param scope the segment scope for memory allocation
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(String[] strings, boolean zeroTerminated, MemorySession scope) {
        int length = zeroTerminated ? strings.length + 1 : strings.length;
        var memorySegment = scope.allocateArray(valueLayout.ADDRESS, length);
        for (int i = 0; i < strings.length; i++) {
            var cString = scope.allocateUtf8String(strings[i]);
            memorySegment.setAtIndex(valueLayout.ADDRESS, i, cString);
        }
        if (zeroTerminated) {
            memorySegment.setAtIndex(valueLayout.ADDRESS, strings.length, MemoryAddress.NULL);
        }
        return memorySegment;
    }

    /**
     * Converts the boolean[] array into an int[] array, and calls {@link #allocateNativeArray(int[], boolean, MemorySession)}.
     * Each boolean value "true" is converted 1, boolean value "false" to 0.
     * @param array Array of booleans
     * @param zeroTerminated When true, an (int) 0 is appended to the array
     * @param scope the segment scope for memory allocation
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(boolean[] array, boolean zeroTerminated, MemorySession scope) {
        int[] intArray = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            intArray[i] = array[i] ? 1 : 0;
        }
        return allocateNativeArray(intArray, zeroTerminated, scope);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of bytes.
     * @param array The array of bytes
     * @param zeroTerminated When true, a (byte) 0 is appended to the array
     * @param scope the segment scope for memory allocation
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(byte[] array, boolean zeroTerminated, MemorySession scope) {
        byte[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return scope.allocateArray(valueLayout.C_BYTE, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of chars.
     * @param array The array of chars
     * @param zeroTerminated When true, a (char) 0 is appended to the array
     * @param scope the segment scope for memory allocation
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(char[] array, boolean zeroTerminated, MemorySession scope) {
        char[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return scope.allocateArray(valueLayout.C_CHAR, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of doubles.
     * @param array The array of doubles
     * @param zeroTerminated When true, a (double) 0 is appended to the array
     * @param scope the segment scope for memory allocation
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(double[] array, boolean zeroTerminated, MemorySession scope) {
        double[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return scope.allocateArray(valueLayout.C_DOUBLE, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of floats.
     * @param array The array of floats
     * @param zeroTerminated When true, a (float) 0 is appended to the array
     * @param scope the segment scope for memory allocation
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(float[] array, boolean zeroTerminated, MemorySession scope) {
        float[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return scope.allocateArray(valueLayout.C_FLOAT, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of floats.
     * @param array The array of floats
     * @param zeroTerminated When true, a (int) 0 is appended to the array
     * @param scope the segment scope for memory allocation
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(int[] array, boolean zeroTerminated, MemorySession scope) {
        int[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return scope.allocateArray(valueLayout.C_INT, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of longs.
     * @param array The array of longs
     * @param zeroTerminated When true, a (long) 0 is appended to the array
     * @param scope the segment scope for memory allocation
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(long[] array, boolean zeroTerminated, MemorySession scope) {
        long[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return scope.allocateArray(valueLayout.C_LONG, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of shorts.
     * @param array The array of shorts
     * @param zeroTerminated When true, a (short) 0 is appended to the array
     * @param scope the segment scope for memory allocation
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(short[] array, boolean zeroTerminated, MemorySession scope) {
        short[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return scope.allocateArray(valueLayout.C_SHORT, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array 
     * of pointers (from Proxy instances).
     * @param array The array of Proxy instances
     * @param zeroTerminated Whether to add an additional NUL to the array
     * @param scope the segment scope for memory allocation
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(Proxy[] array, boolean zeroTerminated, MemorySession scope) {
        Addressable[] addressArray = new Addressable[array.length];
        for (int i = 0; i < array.length; i++) {
            addressArray[i] = array[i].handle();
        }
        return allocateNativeArray(addressArray, zeroTerminated, scope);
    }
    
    /**
     * Allocates and initializes an (optionally NULL-terminated) array
     * of structs (from Proxy instances). The actual memory segments (not 
     * the pointers) are copied into the array.
     * @param array The array of Proxy instances
     * @param layout The memory layout of the object type
     * @param zeroTerminated Whether to add an additional NUL to the array
     * @param scope the segment scope for memory allocation
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(Proxy[] array, MemoryLayout layout, boolean zeroTerminated, MemorySession scope) {
        int length = zeroTerminated ? array.length + 1 : array.length;
        MemorySegment memorySegment = scope.allocateArray(layout, length);
        for (int i = 0; i < array.length; i++) {
            MemorySegment element = MemorySegment.ofAddress((MemoryAddress) array[i].handle(), layout.byteSize(), scope);
            memorySegment.asSlice(i * layout.byteSize()).copyFrom(element);
        }
        if (zeroTerminated) {
            memorySegment.setAtIndex(valueLayout.ADDRESS, array.length, MemoryAddress.NULL);
        }
        return memorySegment;
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array 
     * of memory addresses.
     * @param array The array of addresses
     * @param zeroTerminated Whether to add an additional NUL to the array
     * @param scope the segment scope for memory allocation
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(Addressable[] array, boolean zeroTerminated, MemorySession scope) {
        int length = zeroTerminated ? array.length + 1 : array.length;
        var memorySegment = scope.allocateArray(valueLayout.ADDRESS, length);
        for (int i = 0; i < array.length; i++) {
            memorySegment.setAtIndex(valueLayout.ADDRESS, i, array[i]);
        }
        if (zeroTerminated) {
            memorySegment.setAtIndex(valueLayout.ADDRESS, array.length, MemoryAddress.NULL);
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
            if (layout instanceof ValueLayout valueLayout) {
                return (ret || valueLayout.carrier() != MemoryAddress.class) ?
                        valueLayout.carrier() : Addressable.class;
            } else if (layout instanceof GroupLayout) {
                return MemorySegment.class;
            } else {
                throw new AssertionError("Cannot get here!");
            }
        }

        // This method is used from a MethodHandle (INVOKE_MH).
        private Object invoke(SegmentAllocator allocator, Object[] args) throws Throwable {
            // one trailing Object[]
            int nNamedArgs = function.argumentLayouts().size();
            assert(args.length == nNamedArgs + 1);
            // The last argument is the array of vararg collector
            Object[] unnamedArgs = (Object[]) args[args.length - 1];

            int argsCount = nNamedArgs + unnamedArgs.length;
            Class<?>[] argTypes = new Class<?>[argsCount];
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
            if (MemoryAddress.class.isAssignableFrom(c)) {
                return MemoryAddress.class;
            }
            if (MemorySegment.class.isAssignableFrom(c)) {
                return MemorySegment.class;
            }
            throw new IllegalArgumentException("Invalid type for ABI: " + c.getTypeName());
        }

        private MemoryLayout variadicLayout(Class<?> c) {
            if (c == long.class) {
                return valueLayout.C_LONG;
            } else if (c == double.class) {
                return valueLayout.C_DOUBLE;
            } else if (MemoryAddress.class.isAssignableFrom(c)) {
                return valueLayout.ADDRESS;
            } else {
                throw new IllegalArgumentException("Unhandled variadic argument class: " + c);
            }
        }
        
        // Unwrap the java-gi types to their memory address or primitive value.
        private Object unwrapJavagiTypes(Object o) {
            if (o == null) {
                return MemoryAddress.NULL;
            }
            if (o instanceof Boolean bool) {
                return bool.booleanValue() ? 1 : 0;
            }
            if (o instanceof Proxy proxy) {
                return proxy.handle();
            }
            if (o instanceof Alias<?> alias) {
                return alias.getValue();
            }
            if (o instanceof Bitfield bitfield) {
                return bitfield.getValue();
            }
            if (o instanceof Enumeration enumeration) {
                return enumeration.getValue();
            }
            if (o instanceof java.lang.String string) {
                return MemorySession.openImplicit().allocateUtf8String(string).address();
            }
            return o;
        }
    }
}
