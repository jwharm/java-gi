package io.github.jwharm.javagi;

import java.lang.foreign.*;
import java.lang.invoke.*;
import java.util.HashMap;

public class Interop {

    private static boolean initialized = false;
    private static MemorySession session;
    private static SegmentAllocator allocator;
    private static MemorySegment cbDestroyNotify_nativeSymbol;
    private final static SymbolLookup symbolLookup;
    private final static Linker linker = Linker.nativeLinker();

    public final static HashMap<Integer, Object> signalRegistry = new HashMap<>();

    static {
        System.loadLibrary("adwaita-1");
        System.loadLibrary("gtk-4");
        System.loadLibrary("pangocairo-1.0");
        System.loadLibrary("pango-1.0");
        System.loadLibrary("harfbuzz");
        System.loadLibrary("gdk_pixbuf-2.0");
        System.loadLibrary("cairo-gobject");
        System.loadLibrary("cairo");
        System.loadLibrary("graphene-1.0");
        System.loadLibrary("gio-2.0");
        System.loadLibrary("gobject-2.0");
        System.loadLibrary("glib-2.0");
        SymbolLookup loaderLookup = SymbolLookup.loaderLookup();
        symbolLookup = name -> loaderLookup.lookup(name).or(() -> linker.defaultLookup().lookup(name));
    }

    public static final MethodHandle g_signal_connect_data = Interop.downcallHandle(
            "g_signal_connect_data",
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
    );

    private static void initialize() {
        session = MemorySession.openConfined();
        allocator = SegmentAllocator.newNativeArena(session);
        initialized = true;

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

    public static MemorySession getScope() {
        if (!initialized) {
            initialize();
        }
        return session;
    }

    public static SegmentAllocator getAllocator() {
        if (!initialized) {
            initialize();
        }
        return allocator;
    }

    public static MethodHandle downcallHandle(String name, FunctionDescriptor fdesc) {
        return symbolLookup.lookup(name).
                map(addr -> linker.downcallHandle(addr, fdesc)).
                orElse(null);
    }

    public static int registerCallback(int hash, Object callback) {
        signalRegistry.put(hash, callback);
        return hash;
    }

    public static void cbDestroyNotify(MemoryAddress data) {
        int hash = data.get(ValueLayout.JAVA_INT, 0);
        signalRegistry.remove(hash);
    }

    public static MemorySegment cbDestroyNotifySymbol() {
        if (!initialized) {
            initialize();
        }
        return cbDestroyNotify_nativeSymbol;
    }

    public static MemorySegmentReference allocateNativeString(String string) {
        if (!initialized) {
            initialize();
        }
        return new MemorySegmentReference(allocator.allocateUtf8String(string));
    }

    /**
     * Allocates and initializes a NULL-terminated array of strings (NUL-terminated utf8 char*).
     */
    public static MemorySegmentReference allocateNativeArray(String[] strings) {
        if (!initialized) {
            initialize();
        }
        var memorySegment = allocator.allocateArray(ValueLayout.ADDRESS, strings.length + 1);
        for (int i = 0; i < strings.length; i++) {
            var cString = allocator.allocateUtf8String(strings[i]);
            memorySegment.setAtIndex(ValueLayout.ADDRESS, i, cString);
        }
        memorySegment.setAtIndex(ValueLayout.ADDRESS, strings.length, MemoryAddress.NULL);
        return new MemorySegmentReference(memorySegment);
    }

    public static MemorySegmentReference allocateNativeArray(boolean[] array) {
        int[] intArray = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            intArray[i] = array[i] ? 1 : 0;
        }
        return allocateNativeArray(intArray);
    }

    public static MemorySegmentReference allocateNativeArray(byte[] array) {
        if (!initialized) {
            initialize();
        }
        if (array == null || array.length == 0) {
            return null;
        }
        return new MemorySegmentReference(
                allocator.allocateArray(ValueLayout.JAVA_BYTE, array)
        );
    }

    public static MemorySegmentReference allocateNativeArray(char[] array) {
        if (!initialized) {
            initialize();
        }
        if (array == null || array.length == 0) {
            return null;
        }
        return new MemorySegmentReference(
                allocator.allocateArray(ValueLayout.JAVA_CHAR, array)
        );
    }

    public static MemorySegmentReference allocateNativeArray(double[] array) {
        if (!initialized) {
            initialize();
        }
        if (array == null || array.length == 0) {
            return null;
        }
        return new MemorySegmentReference(
                allocator.allocateArray(ValueLayout.JAVA_DOUBLE, array)
        );
    }

    public static MemorySegmentReference allocateNativeArray(float[] array) {
        if (!initialized) {
            initialize();
        }
        if (array == null || array.length == 0) {
            return null;
        }
        return new MemorySegmentReference(
                allocator.allocateArray(ValueLayout.JAVA_FLOAT, array)
        );
    }

    public static MemorySegmentReference allocateNativeArray(int[] array) {
        if (!initialized) {
            initialize();
        }
        if (array == null || array.length == 0) {
            return null;
        }
        return new MemorySegmentReference(
                allocator.allocateArray(ValueLayout.JAVA_INT, array)
        );
    }

    public static MemorySegmentReference allocateNativeArray(long[] array) {
        if (!initialized) {
            initialize();
        }
        if (array == null || array.length == 0) {
            return null;
        }
        return new MemorySegmentReference(
                allocator.allocateArray(ValueLayout.JAVA_LONG, array)
        );
    }

    public static MemorySegmentReference allocateNativeArray(short[] array) {
        if (!initialized) {
            initialize();
        }
        if (array == null || array.length == 0) {
            return null;
        }
        return new MemorySegmentReference(
                allocator.allocateArray(ValueLayout.JAVA_SHORT, array)
        );
    }

    /**
     * Allocates and initializes a NULL-terminated array of pointers (from NativeAddress instances).
     */
    public static MemorySegmentReference allocateNativeArray(Proxy[] array) {
        if (!initialized) {
            initialize();
        }
        var memorySegment = allocator.allocateArray(ValueLayout.ADDRESS, array.length + 1);
        for (int i = 0; i < array.length; i++) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, i, array[i].handle());
        }
        memorySegment.setAtIndex(ValueLayout.ADDRESS, array.length, MemoryAddress.NULL);
        return new MemorySegmentReference(memorySegment);
    }

    /**
     * Allocates and initializes a NULL-terminated array of pointers (MemoryAddress instances).
     */
    public static MemorySegmentReference allocateNativeArray(MemoryAddress[] array) {
        if (!initialized) {
            initialize();
        }
        var memorySegment = allocator.allocateArray(ValueLayout.ADDRESS, array.length + 1);
        for (int i = 0; i < array.length; i++) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, i, array[i]);
        }
        memorySegment.setAtIndex(ValueLayout.ADDRESS, array.length, MemoryAddress.NULL);
        return new MemorySegmentReference(memorySegment);
    }
}
