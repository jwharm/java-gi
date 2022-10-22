package io.github.jwharm.javagi;

import java.lang.foreign.*;
import java.lang.invoke.*;
import java.util.HashMap;

public class Interop {

    private final static MemorySession session;
    private final static SegmentAllocator allocator;
    private final static MemorySegment cbDestroyNotify_nativeSymbol;
    private final static SymbolLookup symbolLookup;
    private final static Linker linker = Linker.nativeLinker();

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
        allocator = SegmentAllocator.implicitAllocator();

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
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
    );

    /**
     * Get the active memory session
     * @return The memory session
     */
    public static MemorySession getScope() {
        return session;
    }

    /**
     * Get the memory allocator
     * @return An instance of SegmentAllocator.implicitAllocator(). Memory segments 
     *         allocated by this allocator register a Cleaner, that automatically 
     *         frees the native memory segment when it runs.
     */
    public static SegmentAllocator getAllocator() {
        return allocator;
    }

    /**
     * Creates a method handle that is used to call the native function with 
     * the provided name and function descriptor.
     * @param name Name of the native function
     * @param fdesc Function descriptor of the native function
     * @return the MethodHandle
     */
    public static MethodHandle downcallHandle(String name, FunctionDescriptor fdesc) {
        return symbolLookup.lookup(name).
                map(addr -> linker.downcallHandle(addr, fdesc)).
                orElse(null);
    }

    /**
     * Register a callback in the signalRegistry map. The key is 
     * the hashcode of the callback.
     * @param callback Callback to save in the signalRegistry map
     * @return the calculated hashcode
     */
    public static int registerCallback(Object callback) {
        int hash = callback.hashCode();
        signalRegistry.put(hash, callback);
        return hash;
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
        return allocator.allocateUtf8String(string);
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
        var memorySegment = allocator.allocateArray(ValueLayout.ADDRESS, length);
        for (int i = 0; i < strings.length; i++) {
            var cString = allocator.allocateUtf8String(strings[i]);
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
        return allocator.allocateArray(ValueLayout.JAVA_BYTE, array);
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
        return allocator.allocateArray(ValueLayout.JAVA_CHAR, array);
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
        return allocator.allocateArray(ValueLayout.JAVA_DOUBLE, array);
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
        return allocator.allocateArray(ValueLayout.JAVA_FLOAT, array);
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
        return allocator.allocateArray(ValueLayout.JAVA_INT, array);
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
        return allocator.allocateArray(ValueLayout.JAVA_LONG, array);
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
        return allocator.allocateArray(ValueLayout.JAVA_SHORT, array);
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
        var memorySegment = allocator.allocateArray(ValueLayout.ADDRESS, length);
        for (int i = 0; i < array.length; i++) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, i, array[i]);
        }
        if (zeroTerminated) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, array.length, MemoryAddress.NULL);
        }
        return memorySegment;
    }
}
