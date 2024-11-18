package io.github.jwharm.javagi.interop;

import io.github.jwharm.javagi.base.Proxy;
import org.gnome.glib.GString;
import org.gnome.glib.OptionFlags;
import org.gnome.glib.Variant;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test conversion of strings and arrays to native memory and back
 */
public class MarshalTest {

    @Test
    void testString() {
        try (Arena arena = Arena.ofConfined()) {
            String input = "123 abc";
            MemorySegment allocation = Interop.allocateNativeString(input, arena);
            String output = Interop.getStringFrom(allocation);
            assertEquals(input, output);
        }
    }

    @Test
    void testStringArray() {
        try (Arena arena = Arena.ofConfined()) {
            String[] input = {"123 abc", "456 def", "789 ghi"};
            MemorySegment allocation = Interop.allocateNativeArray(input, false, arena);
            String[] output = Interop.getStringArrayFrom(allocation, 3, false);
            assertEquals(Arrays.toString(input), Arrays.toString(output));

            allocation = Interop.allocateNativeArray(input, true, arena);
            output = Interop.getStringArrayFrom(allocation, false);
            assertEquals(Arrays.toString(input), Arrays.toString(output));
        }
    }

    @Test
    void testPointerArray() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment[] input = {
                    arena.allocate(10),
                    arena.allocate(10),
                    arena.allocate(10)
            };
            MemorySegment allocation = Interop.allocateNativeArray(input, false, arena);
            MemorySegment[] output = Interop.getAddressArrayFrom(allocation, 3, false);
            for (int i = 0; i < input.length; i++)
                assertEquals(input[i].address(), output[i].address());

            allocation = Interop.allocateNativeArray(input, true, arena);
            output = Interop.getAddressArrayFrom(allocation, false);
            for (int i = 0; i < input.length; i++)
                assertEquals(input[i].address(), output[i].address());
        }
    }

    @Test
    void testBooleanArray() {
        try (Arena arena = Arena.ofConfined()) {
            boolean[] input = {true, false, true, true, false};
            MemorySegment allocation = Interop.allocateNativeArray(input, false, arena);
            boolean[] output = Interop.getBooleanArrayFrom(allocation, 5, arena, false);
            assertEquals(Arrays.toString(input), Arrays.toString(output));
        }
    }

    @Test
    void testByteArray() {
        try (Arena arena = Arena.ofConfined()) {
            byte[] input = "1234567890".getBytes();
            MemorySegment allocation = Interop.allocateNativeArray(input, false, arena);
            byte[] output = Interop.getByteArrayFrom(allocation, input.length, arena, false);
            assertEquals(Arrays.toString(input), Arrays.toString(output));

            allocation = Interop.allocateNativeArray(input, true, arena);
            output = Interop.getByteArrayFrom(allocation, arena, false);
            assertEquals(Arrays.toString(input), Arrays.toString(output));
        }
    }

    @Test
    void testCharArray() {
        try (Arena arena = Arena.ofConfined()) {
            char[] input = "1234567890".toCharArray();
            MemorySegment allocation = Interop.allocateNativeArray(input, false, arena);
            char[] output = Interop.getCharacterArrayFrom(allocation, input.length, arena, false);
            assertEquals(Arrays.toString(input), Arrays.toString(output));
        }
    }

    @Test
    void testDoubleArray() {
        try (Arena arena = Arena.ofConfined()) {
            double[] input = {1d, 2d, 3d, Math.PI, Double.MIN_VALUE, Double.MAX_VALUE};
            MemorySegment allocation = Interop.allocateNativeArray(input, false, arena);
            double[] output = Interop.getDoubleArrayFrom(allocation, input.length, arena, false);
            assertEquals(Arrays.toString(input), Arrays.toString(output));
        }
    }

    @Test
    void testFloatArray() {
        try (Arena arena = Arena.ofConfined()) {
            float[] input = {1.2f, 2.3f, 3.35f, Float.MIN_VALUE, Float.MAX_VALUE};
            MemorySegment allocation = Interop.allocateNativeArray(input, false, arena);
            float[] output = Interop.getFloatArrayFrom(allocation, input.length, arena, false);
            assertEquals(Arrays.toString(input), Arrays.toString(output));
        }
    }

    @Test
    void testIntArray() {
        try (Arena arena = Arena.ofConfined()) {
            int[] input = {1, 2, 3, 0, Integer.MIN_VALUE, Integer.MAX_VALUE};
            MemorySegment allocation = Interop.allocateNativeArray(input, false, arena);
            int[] output = Interop.getIntegerArrayFrom(allocation, input.length, arena, false);
            assertEquals(Arrays.toString(input), Arrays.toString(output));

            output = Interop.getIntegerArrayFrom(allocation, arena, false);
            assertEquals(3, output.length);
        }
    }

    @Test
    void testLongArray() {
        try (Arena arena = Arena.ofConfined()) {
            long[] input = {1L, 2L, 3L, Long.MIN_VALUE, Long.MAX_VALUE};
            MemorySegment allocation = Interop.allocateNativeArray(input, false, arena);
            long[] output = Interop.getLongArrayFrom(allocation, input.length, arena, false);
            assertEquals(Arrays.toString(input), Arrays.toString(output));
        }
    }

    @Test
    void testShortArray() {
        try (Arena arena = Arena.ofConfined()) {
            short[] input = {(short) 1, (short) 2, (short) 3,
                    Short.MIN_VALUE, Short.MAX_VALUE};
            MemorySegment allocation = Interop.allocateNativeArray(input, false, arena);
            short[] output = Interop.getShortArrayFrom(allocation, input.length, arena, false);
            assertEquals(Arrays.toString(input), Arrays.toString(output));
        }
    }

    @Test
    void testProxyArray() {
        try (Arena arena = Arena.ofConfined()) {
            Proxy[] input = {
                    Variant.int32(1),
                    Variant.int32(2),
                    Variant.int32(3)
            };
            MemorySegment allocation = Interop.allocateNativeArray(input, false, arena);
            Proxy[] output = Interop.getProxyArrayFrom(allocation, 3, Variant.class, Variant::new);
            for (int i = 0; i < input.length; i++)
                assertEquals(input[i], output[i]);

            allocation = Interop.allocateNativeArray(input, true, arena);
            output = Interop.getProxyArrayFrom(allocation, Variant.class, Variant::new);
            for (int i = 0; i < input.length; i++)
                assertEquals(input[i], output[i]);
        }
    }

    @Test
    void testStructArray() {
        try (Arena arena = Arena.ofConfined()) {
            GString[] input = {
                    new GString("abc"),
                    new GString((String) null),
                    new GString("12345 67890")
            };
            MemorySegment allocation = Interop.allocateNativeArray(input, GString.getMemoryLayout(), false, arena);
            GString[] output = Interop.getStructArrayFrom(allocation, 3, GString.class, GString::new, GString.getMemoryLayout());
            for (int i = 0; i < input.length; i++)
                assertTrue(input[i].equal(output[i]));
        }
    }

    @Test
    void testFlagsArray() {
        try (Arena arena = Arena.ofConfined()) {
            OptionFlags[] input = {
                    OptionFlags.IN_MAIN,
                    OptionFlags.FILENAME,
                    OptionFlags.NOALIAS
            };
            int[] values = Interop.getValues(input);
            MemorySegment allocation = Interop.allocateNativeArray(values, false, arena);
            OptionFlags[] output = Interop.getArrayFromIntPointer(allocation, 3, OptionFlags.class, OptionFlags::of);
            assertEquals(Arrays.toString(input), Arrays.toString(output));
        }
    }
}
