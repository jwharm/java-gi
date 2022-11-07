package io.github.jwharm.javagi;

import java.lang.foreign.Addressable;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;

public class Interop {

    private final static MemorySession session;
    private final static SegmentAllocator implicitAllocator, sessionAllocator;
    private final static MemorySegment cbDestroyNotify_nativeSymbol;
    private final static SymbolLookup symbolLookup;
    private final static Linker linker = Linker.nativeLinker();
    
    public final static Layout_x86_64 valueLayout = new Layout_x86_64();

    /**
     * This map contains the callbacks used in g_signal_connect. The 
     * keys are the hashcodes of the callback objects. This hashcode is 
     * passed to g_signal_connect in the user_data parameter and passed 
     * as a parameter to the static callback functions. The static 
     * callback functions use the hashcode to retrieve the user-defined 
     * callback function from the signalRegistry map, and run it.
     */
    public final static HashMap<Integer, Object> signalRegistry = new HashMap<>();
    
    static {
        SymbolLookup loaderLookup = SymbolLookup.loaderLookup();
        symbolLookup = name -> loaderLookup.lookup(name).or(() -> linker.defaultLookup().lookup(name));
        
        // Initialize the memory session and an implicit allocator
        session = MemorySession.openConfined();
        implicitAllocator = SegmentAllocator.implicitAllocator();
        sessionAllocator = SegmentAllocator.newNativeArena(session);

        // Initialize upcall stub for DestroyNotify callback
        try {
            MethodType methodType = MethodType.methodType(void.class, MemoryAddress.class);
            MethodHandle methodHandle = MethodHandles.lookup().findStatic(Interop.class, "cbDestroyNotify", methodType);
            FunctionDescriptor descriptor = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS);
            cbDestroyNotify_nativeSymbol = Linker.nativeLinker().upcallStub(methodHandle, descriptor, session);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The method handle for g_signal_connect_data is used by all
     * generated signal methods.
     */
    public static final MethodHandle g_signal_connect_data = Interop.downcallHandle(
            "g_signal_connect_data",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT),
            false
    );

    /**
     * Get the active memory session
     * @return The memory session
     */
    public static MemorySession getScope() {
        return session;
    }

    /**
     * Get the memory allocator (ImplicitAllocator).
     * @return An instance of SegmentAllocator.implicitAllocator(). Memory segments 
     *         allocated by this allocator register a Cleaner, that automatically 
     *         frees the native memory segment when it runs.
     */
    public static SegmentAllocator getAllocator() {
        return implicitAllocator;
    }
    
    /**
     * Get a NativeArena memory allocator. This allocator allocates a new memory 
     * segment in the current memory session every time it is called. The memory 
     * segments are not released until the app is closed.
     * @return The NativeArena memory allocator.
     */
    public static SegmentAllocator getSessionAllocator() {
        return sessionAllocator;
    }

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
     * Register a callback in the signalRegistry map. The key is 
     * the hashcode of the callback.
     * @param callback Callback to save in the signalRegistry map
     * @return a native memory segment with the calculated hashcode.
     *         The memory segment is not automatically released.
     */
    public static Addressable registerCallback(Object callback) {
        int hash = callback.hashCode();
        signalRegistry.put(hash, callback);
        return sessionAllocator.allocate(ValueLayout.JAVA_INT, hash);
    }

    /**
     * Retrieves a memory address from the provided memory segment
     * @param pointer The memory segment that contains an address
     * @return the memory address
     */
    public static MemoryAddress dereference(MemorySegment pointer) {
        return pointer.get(ValueLayout.ADDRESS, 0);
    }

    /**
     * This callback function will remove a signal callback from the 
     * signalRegistry map.
     * @param data The hashcode of the callback
     */
    public static void cbDestroyNotify(MemoryAddress data) {
        int hash = data.get(ValueLayout.JAVA_INT, 0);
        signalRegistry.remove(hash);
    }

    /**
     * Return the cached native symbol for cbDestroyNotify(MemoryAddress).
     * @return the native symbol for cbDestroyNotify(MemoryAddress)
     */
    public static MemorySegment cbDestroyNotifySymbol() {
        return cbDestroyNotify_nativeSymbol;
    }

    /**
     * Allocate a native string using SegmentAllocator.allocateUtf8String(String).
     * @param string the string to allocate as a native string (utf8 char*)
     * @return the allocated MemorySegment
     */
    public static Addressable allocateNativeString(String string) {
        return implicitAllocator.allocateUtf8String(string);
    }
    
    /**
     * Returns a Java string from native memory using {@code MemoryAddress.getUtf8String()}.
     * If an error occurs or when the native address is NULL, null is returned.
     * @param address The memory address of the native String (\0-terminated char*).
     * @return A String or null
     */
    public static String getStringFrom(MemoryAddress address) {
        try {
            if (! MemoryAddress.NULL.equals(address)) {
                return address.getUtf8String(0);
            }
        } catch (Throwable t) {
        }
        return null;
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array 
     * of strings (NUL-terminated utf8 char*).
     * @param strings Array of Strings
     * @param zeroTerminated Whether to add a NUL at the end the array
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(String[] strings, boolean zeroTerminated) {
        int length = zeroTerminated ? strings.length : strings.length + 1;
        var memorySegment = implicitAllocator.allocateArray(ValueLayout.ADDRESS, length);
        for (int i = 0; i < strings.length; i++) {
            var cString = implicitAllocator.allocateUtf8String(strings[i]);
            memorySegment.setAtIndex(ValueLayout.ADDRESS, i, cString);
        }
        if (zeroTerminated) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, strings.length, MemoryAddress.NULL);
        }
        return memorySegment;
    }

    /**
     * Converts the boolean[] array into an int[] array, and calls allocateNativeArray(int, boolean).
     * Each boolean value "true" is converted 1, boolean value "false" to 0.
     * @param array Array of booleans
     * @param zeroTerminated TODO currently ignored
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(boolean[] array, boolean zeroTerminated) {
        int[] intArray = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            intArray[i] = array[i] ? 1 : 0;
        }
        return allocateNativeArray(intArray, zeroTerminated);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of bytes.
     * @param array The array of bytes
     * @param zeroTerminated TODO currently ignored
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(byte[] array, boolean zeroTerminated) {
        if (array == null || array.length == 0) {
            return null;
        }
        return implicitAllocator.allocateArray(ValueLayout.JAVA_BYTE, array);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of chars.
     * @param array The array of chars
     * @param zeroTerminated TODO currently ignored
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(char[] array, boolean zeroTerminated) {
        if (array == null || array.length == 0) {
            return null;
        }
        return implicitAllocator.allocateArray(ValueLayout.JAVA_CHAR, array);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of doubles.
     * @param array The array of doubles
     * @param zeroTerminated TODO currently ignored
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(double[] array, boolean zeroTerminated) {
        if (array == null || array.length == 0) {
            return null;
        }
        return implicitAllocator.allocateArray(ValueLayout.JAVA_DOUBLE, array);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of floats.
     * @param array The array of floats
     * @param zeroTerminated TODO currently ignored
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(float[] array, boolean zeroTerminated) {
        if (array == null || array.length == 0) {
            return null;
        }
        return implicitAllocator.allocateArray(ValueLayout.JAVA_FLOAT, array);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of floats.
     * @param array The array of floats
     * @param zeroTerminated TODO currently ignored
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(int[] array, boolean zeroTerminated) {
        if (array == null || array.length == 0) {
            return null;
        }
        return implicitAllocator.allocateArray(ValueLayout.JAVA_INT, array);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of longs.
     * @param array The array of longs
     * @param zeroTerminated TODO currently ignored
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(long[] array, boolean zeroTerminated) {
        if (array == null || array.length == 0) {
            return null;
        }
        return implicitAllocator.allocateArray(ValueLayout.JAVA_LONG, array);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of shorts.
     * @param array The array of shorts
     * @param zeroTerminated TODO currently ignored
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(short[] array, boolean zeroTerminated) {
        if (array == null || array.length == 0) {
            return null;
        }
        return implicitAllocator.allocateArray(ValueLayout.JAVA_SHORT, array);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array 
     * of pointers (from Proxy instances).
     * @param array The array of Proxy instances
     * @param zeroTerminated Whether to add an additional NUL to the array
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(Proxy[] array, boolean zeroTerminated) {
        Addressable[] addressArray = new Addressable[array.length];
        for (int i = 0; i < array.length; i++) {
            addressArray[i] = array[i].handle();
        }
        return allocateNativeArray(addressArray, zeroTerminated);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array 
     * of memory addresses.
     * @param array The array of addresses
     * @param zeroTerminated Whether to add an additional NUL to the array
     * @return The memory segment of the native array
     */
    public static Addressable allocateNativeArray(Addressable[] array, boolean zeroTerminated) {
        int length = zeroTerminated ? array.length : array.length + 1;
        var memorySegment = implicitAllocator.allocateArray(ValueLayout.ADDRESS, length);
        for (int i = 0; i < array.length; i++) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, i, array[i]);
        }
        if (zeroTerminated) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, array.length, MemoryAddress.NULL);
        }
        return memorySegment;
    }
    
    // Adapted from code that was generated by jextract
    private static class VarargsInvoker {
        private static final MethodHandle INVOKE_MH;
        private final MemorySegment symbol;
        private final FunctionDescriptor function;
        private final static SegmentAllocator THROWING_ALLOCATOR = (x, y) -> { throw new AssertionError("should not reach here"); };

        private VarargsInvoker(MemorySegment symbol, FunctionDescriptor function) {
            this.symbol = symbol;
            this.function = function;
        }

        static {
            try {
                INVOKE_MH = MethodHandles.lookup().findVirtual(VarargsInvoker.class, "invoke", MethodType.methodType(Object.class, SegmentAllocator.class, Object[].class));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
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
            if (o instanceof Proxy proxy) {
                return proxy.handle();
            }
            if (o instanceof Alias alias) {
                return alias.getValue();
            }
            if (o instanceof Bitfield bitfield) {
                return bitfield.getValue();
            }
            if (o instanceof Enumeration enumeration) {
                return enumeration.getValue();
            }
            if (o instanceof java.lang.String string) {
                return getSessionAllocator().allocateUtf8String(string).address();
            }
            return o;
        }
    }
}
