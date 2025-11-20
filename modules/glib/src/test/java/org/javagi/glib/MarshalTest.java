package org.javagi.glib;

import org.javagi.base.Proxy;
import org.javagi.interop.Interop;
import org.gnome.glib.Date;
import org.gnome.glib.OptionFlags;
import org.gnome.glib.Variant;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.Set;

import static org.javagi.base.TransferOwnership.NONE;
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
            String[] output = Interop.getStringArrayFrom(allocation, 3, NONE);
            assertEquals(Arrays.toString(input), Arrays.toString(output));

            allocation = Interop.allocateNativeArray(input, true, arena);
            output = Interop.getStringArrayFrom(allocation, NONE);
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
            MemorySegment[] output = Interop.getAddressArrayFrom(allocation, 3, NONE);
            for (int i = 0; i < input.length; i++)
                assertEquals(input[i].address(), output[i].address());

            allocation = Interop.allocateNativeArray(input, true, arena);
            output = Interop.getAddressArrayFrom(allocation, NONE);
            for (int i = 0; i < input.length; i++)
                assertEquals(input[i].address(), output[i].address());
        }
    }

    @Test
    void testBooleanArray() {
        try (Arena arena = Arena.ofConfined()) {
            boolean[] input = {true, false, true, true, false};
            MemorySegment allocation = Interop.allocateNativeArray(input, false, arena);
            boolean[] output = Interop.getBooleanArrayFrom(allocation, 5, arena, NONE);
            assertEquals(Arrays.toString(input), Arrays.toString(output));
        }
    }

    @Test
    void testByteArray() {
        try (Arena arena = Arena.ofConfined()) {
            byte[] input = "1234567890".getBytes();
            MemorySegment allocation = Interop.allocateNativeArray(input, false, arena);
            byte[] output = Interop.getByteArrayFrom(allocation, input.length, arena, NONE);
            assertEquals(Arrays.toString(input), Arrays.toString(output));

            allocation = Interop.allocateNativeArray(input, true, arena);
            output = Interop.getByteArrayFrom(allocation, arena, NONE);
            assertEquals(Arrays.toString(input), Arrays.toString(output));
        }
    }

    @Test
    void testCharArray() {
        try (Arena arena = Arena.ofConfined()) {
            char[] input = "1234567890".toCharArray();
            MemorySegment allocation = Interop.allocateNativeArray(input, false, arena);
            char[] output = Interop.getCharacterArrayFrom(allocation, input.length, arena, NONE);
            assertEquals(Arrays.toString(input), Arrays.toString(output));
        }
    }

    @Test
    void testDoubleArray() {
        try (Arena arena = Arena.ofConfined()) {
            double[] input = {1d, 2d, 3d, Math.PI, Double.MIN_VALUE, Double.MAX_VALUE};
            MemorySegment allocation = Interop.allocateNativeArray(input, false, arena);
            double[] output = Interop.getDoubleArrayFrom(allocation, input.length, arena, NONE);
            assertEquals(Arrays.toString(input), Arrays.toString(output));
        }
    }

    @Test
    void testFloatArray() {
        try (Arena arena = Arena.ofConfined()) {
            float[] input = {1.2f, 2.3f, 3.35f, Float.MIN_VALUE, Float.MAX_VALUE};
            MemorySegment allocation = Interop.allocateNativeArray(input, false, arena);
            float[] output = Interop.getFloatArrayFrom(allocation, input.length, arena, NONE);
            assertEquals(Arrays.toString(input), Arrays.toString(output));
        }
    }

    @Test
    void testIntArray() {
        try (Arena arena = Arena.ofConfined()) {
            int[] input = {1, 2, 3, 0, Integer.MIN_VALUE, Integer.MAX_VALUE};
            MemorySegment allocation = Interop.allocateNativeArray(input, false, arena);
            int[] output = Interop.getIntegerArrayFrom(allocation, input.length, arena, NONE);
            assertEquals(Arrays.toString(input), Arrays.toString(output));

            output = Interop.getIntegerArrayFrom(allocation, arena, NONE);
            assertEquals(3, output.length);
        }
    }

    @Test
    void testLongArray() {
        try (Arena arena = Arena.ofConfined()) {
            long[] input = {1L, 2L, 3L, Long.MIN_VALUE, Long.MAX_VALUE};
            MemorySegment allocation = Interop.allocateNativeArray(input, false, arena);
            long[] output = Interop.getLongArrayFrom(allocation, input.length, arena, NONE);
            assertEquals(Arrays.toString(input), Arrays.toString(output));
        }
    }

    @Test
    void testShortArray() {
        try (Arena arena = Arena.ofConfined()) {
            short[] input = {(short) 1, (short) 2, (short) 3,
                    Short.MIN_VALUE, Short.MAX_VALUE};
            MemorySegment allocation = Interop.allocateNativeArray(input, false, arena);
            short[] output = Interop.getShortArrayFrom(allocation, input.length, arena, NONE);
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
            Date[] input = {
                    new Date(),
                    new Date(),
                    new Date()
            };
            input[0].setParse("01/01/2000");
            input[1].setParse("01/01/2001");
            input[2].setParse("01/01/2002");
            MemorySegment allocation = Interop.allocateNativeArray(input, Date.getMemoryLayout(), false, arena);
            Date[] output = Interop.getStructArrayFrom(allocation, 3, Date.class, Date::new, Date.getMemoryLayout());
            for (int i = 0; i < input.length; i++)
                assertEquals(input[i].getYear(), output[i].getYear());
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
            Set<?>[] output = Interop.getArrayFromIntPointer(allocation, 3, Set.class, OptionFlags::of);
            assertEquals(3, output.length);
            assertTrue(output[0].size() == 1 && output[0].contains(OptionFlags.IN_MAIN));
            assertTrue(output[1].size() == 1 && output[1].contains(OptionFlags.FILENAME));
            assertTrue(output[2].size() == 1 && output[2].contains(OptionFlags.NOALIAS));
        }
    }
}
