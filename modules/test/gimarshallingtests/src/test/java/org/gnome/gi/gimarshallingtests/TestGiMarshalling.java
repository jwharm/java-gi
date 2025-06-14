package org.gnome.gi.gimarshallingtests;

import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.shortReturnMin;
import static org.junit.jupiter.api.Assertions.*;

/*
 * Note: We can't express unsigned int8 in Java.
 * Casting G_MAXUINT8 to a signed byte == -1. Same for G_MAXUINT16, 32, etc.
 */
public class TestGiMarshalling {

    @Test
    void testBooleanReturnTrue() {
        assertTrue(booleanReturnTrue());
    }

    @Test
    void testBooleanReturnFalse() {
        assertFalse(booleanReturnFalse());
    }

    @Test
    void testBooleanInTrue() {
        booleanInTrue(true);
    }

    @Test
    void testBooleanInFalse() {
        booleanInFalse(false);
    }

    @Test
    void testBooleanOutTrue() {
        var v = new Out<Boolean>();
        booleanOutTrue(v);
        assertTrue(v.get());
    }

    @Test
    void testBooleanOutFalse() {
        var v = new Out<Boolean>();
        booleanOutFalse(v);
        assertFalse(v.get());
    }

    @Test
    void testBooleanOutUninitialized() {
        assertFalse(booleanOutUninitialized(null));
    }

    @Test
    void testBooleanInOutTrueFalse() {
        var v = new Out<>(true);
        booleanInoutTrueFalse(v);
        assertFalse(v.get());
    }

    @Test
    void testBooleanInOutFalseTrue() {
        var v = new Out<>(false);
        booleanInoutFalseTrue(v);
        assertTrue(v.get());
    }

    @Test
    void testInt8ReturnMax() {
        assertEquals(Byte.MAX_VALUE, int8ReturnMax());
    }

    @Test
    void testInt8ReturnMin() {
        assertEquals(Byte.MIN_VALUE, int8ReturnMin());
    }

    @Test
    void testInt8InMax() {
        int8InMax(Byte.MAX_VALUE);
    }

    @Test
    void testInt8InMin() {
        int8InMin(Byte.MIN_VALUE);
    }

    @Test
    void testInt8OutMax() {
        var v = new Out<>((byte) 0);
        int8OutMax(v);
        assertEquals(Byte.MAX_VALUE, v.get());
    }

    @Test
    void testInt8OutMin() {
        var v = new Out<>((byte) 0);
        int8OutMin(v);
        assertEquals(Byte.MIN_VALUE, v.get());
    }

    @Test
    void testInt8OutUninitialized() {
        var v = new Out<>((byte) 0);
        assertFalse(int8OutUninitialized(v));
        assertEquals((byte) 0, v.get());
    }

    @Test
    void testInt8OutMaxMin() {
        var v = new Out<>(Byte.MAX_VALUE);
        int8InoutMaxMin(v);
        assertEquals(Byte.MIN_VALUE, v.get());
    }

    @Test
    void testInt8OutMinMax() {
        var v = new Out<>(Byte.MIN_VALUE);
        int8InoutMinMax(v);
        assertEquals(Byte.MAX_VALUE, v.get());
    }

    @Test
    void testUint8Return() {
        assertEquals((byte) -1, uint8Return());
    }

    @Test
    void testUint8In() {
        uint8In((byte) -1);
    }

    @Test
    void testUint8Out() {
        var v = new Out<>((byte) 0);
        uint8Out(v);
        assertEquals((byte) -1, v.get());
    }

    @Test
    void testUint8OutUninitialized() {
        var v = new Out<>((byte) 0);
        assertFalse(uint8OutUninitialized(v));
        assertEquals((byte) 0, v.get());
    }

    @Test
    void testUint8Inout() {
        var v = new Out<>((byte) -1);
        uint8Inout(v);
        assertEquals((byte) 0, v.get());
    }

    @Test
    void testInt16ReturnMax() {
        assertEquals(Short.MAX_VALUE, int16ReturnMax());
    }

    @Test
    void testInt16ReturnMin() {
        assertEquals(Short.MIN_VALUE, int16ReturnMin());
    }

    @Test
    void testInt16InMax() {
        int16InMax(Short.MAX_VALUE);
    }

    @Test
    void testInt16InMin() {
        int16InMin(Short.MIN_VALUE);
    }

    @Test
    void testInt16OutMax() {
        var v = new Out<>((short) 0);
        int16OutMax(v);
        assertEquals(Short.MAX_VALUE, v.get());
    }

    @Test
    void testInt16OutMin() {
        var v = new Out<>((short) 0);
        int16OutMin(v);
        assertEquals(Short.MIN_VALUE, v.get());
    }

    @Test
    void testInt16OutUninitialized() {
        var v = new Out<>((short) 0);
        assertFalse(int16OutUninitialized(v));
        assertEquals((short) 0, v.get());
    }

    @Test
    void testInt16OutMaxMin() {
        var v = new Out<>(Short.MAX_VALUE);
        int16InoutMaxMin(v);
        assertEquals(Short.MIN_VALUE, v.get());
    }

    @Test
    void testInt16OutMinMax() {
        var v = new Out<>(Short.MIN_VALUE);
        int16InoutMinMax(v);
        assertEquals(Short.MAX_VALUE, v.get());
    }

    @Test
    void testUint16Return() {
        assertEquals((short) -1, uint16Return());
    }

    @Test
    void testUint16In() {
        uint16In((short) -1);
    }

    @Test
    void testUint16Out() {
        var v = new Out<>((short) 0);
        uint16Out(v);
        assertEquals((short) -1, v.get());
    }

    @Test
    void testUint16OutUninitialized() {
        var v = new Out<>((short) 0);
        assertFalse(uint16OutUninitialized(v));
        assertEquals((short) 0, v.get());
    }

    @Test
    void testUint16Inout() {
        var v = new Out<>((short) -1);
        uint16Inout(v);
        assertEquals((short) 0, v.get());
    }

    @Test
    void testInt32ReturnMax() {
        assertEquals(Integer.MAX_VALUE, int32ReturnMax());
    }

    @Test
    void testInt32ReturnMin() {
        assertEquals(Integer.MIN_VALUE, int32ReturnMin());
    }

    @Test
    void testInt32InMax() {
        int32InMax(Integer.MAX_VALUE);
    }

    @Test
    void testInt32InMin() {
        int32InMin(Integer.MIN_VALUE);
    }

    @Test
    void testInt32OutMax() {
        var v = new Out<>(0);
        int32OutMax(v);
        assertEquals(Integer.MAX_VALUE, v.get());
    }

    @Test
    void testInt32OutMin() {
        var v = new Out<>(0);
        int32OutMin(v);
        assertEquals(Integer.MIN_VALUE, v.get());
    }

    @Test
    void testInt32OutUninitialized() {
        var v = new Out<>(0);
        assertFalse(int32OutUninitialized(v));
        assertEquals(0, v.get());
    }

    @Test
    void testInt32OutMaxMin() {
        var v = new Out<>(Integer.MAX_VALUE);
        int32InoutMaxMin(v);
        assertEquals(Integer.MIN_VALUE, v.get());
    }

    @Test
    void testInt32OutMinMax() {
        var v = new Out<>(Integer.MIN_VALUE);
        int32InoutMinMax(v);
        assertEquals(Integer.MAX_VALUE, v.get());
    }

    @Test
    void testUint32Return() {
        assertEquals(-1, uint32Return());
    }

    @Test
    void testUint32In() {
        uint32In(-1);
    }

    @Test
    void testUint32Out() {
        var v = new Out<>(0);
        uint32Out(v);
        assertEquals(-1, v.get());
    }

    @Test
    void testUint32OutUninitialized() {
        var v = new Out<>(0);
        assertFalse(uint32OutUninitialized(v));
        assertEquals(0, v.get());
    }

    @Test
    void testUint32Inout() {
        var v = new Out<>(-1);
        uint32Inout(v);
        assertEquals(0, v.get());
    }

    @Test
    void testInt64ReturnMax() {
        assertEquals(Long.MAX_VALUE, int64ReturnMax());
    }

    @Test
    void testInt64ReturnMin() {
        assertEquals(Long.MIN_VALUE, int64ReturnMin());
    }

    @Test
    void testInt64InMax() {
        int64InMax(Long.MAX_VALUE);
    }

    @Test
    void testInt64InMin() {
        int64InMin(Long.MIN_VALUE);
    }

    @Test
    void testInt64OutMax() {
        var v = new Out<>(0L);
        int64OutMax(v);
        assertEquals(Long.MAX_VALUE, v.get());
    }

    @Test
    void testInt64OutMin() {
        var v = new Out<>(0L);
        int64OutMin(v);
        assertEquals(Long.MIN_VALUE, v.get());
    }

    @Test
    void testInt64OutUninitialized() {
        var v = new Out<>(0L);
        assertFalse(int64OutUninitialized(v));
        assertEquals(0L, v.get());
    }

    @Test
    void testInt64OutMaxMin() {
        var v = new Out<>(Long.MAX_VALUE);
        int64InoutMaxMin(v);
        assertEquals(Long.MIN_VALUE, v.get());
    }

    @Test
    void testInt64OutMinMax() {
        var v = new Out<>(Long.MIN_VALUE);
        int64InoutMinMax(v);
        assertEquals(Long.MAX_VALUE, v.get());
    }

    @Test
    void testUint64Return() {
        assertEquals(-1L, uint64Return());
    }

    @Test
    void testUint64In() {
        uint64In(-1L);
    }

    @Test
    void testUint64Out() {
        var v = new Out<>(0L);
        uint64Out(v);
        assertEquals(-1L, v.get());
    }

    @Test
    void testUint64OutUninitialized() {
        var v = new Out<>(0L);
        assertFalse(uint64OutUninitialized(v));
        assertEquals(0L, v.get());
    }

    @Test
    void testUint64Inout() {
        var v = new Out<>(-1L);
        uint64Inout(v);
        assertEquals(0L, v.get());
    }

    @Test
    void testShortReturnMax() {
        assertEquals(Short.MAX_VALUE, shortReturnMax());
    }

    @Test
    void testShortReturnMin() {
        assertEquals(Short.MIN_VALUE, shortReturnMin());
    }

    @Test
    void testShortInMax() {
        shortInMax(Short.MAX_VALUE);
    }

    @Test
    void testShortInMin() {
        shortInMin(Short.MIN_VALUE);
    }

    @Test
    void testShortOutMax() {
        var v = new Out<>((short) 0);
        shortOutMax(v);
        assertEquals(Short.MAX_VALUE, v.get());
    }

    @Test
    void testShortOutMin() {
        var v = new Out<>((short) 0);
        shortOutMin(v);
        assertEquals(Short.MIN_VALUE, v.get());
    }

    @Test
    void testShortOutUninitialized() {
        var v = new Out<>((short) 0);
        assertFalse(shortOutUninitialized(v));
        assertEquals((short) 0, v.get());
    }

    @Test
    void testShortOutMaxMin() {
        var v = new Out<>(Short.MAX_VALUE);
        shortInoutMaxMin(v);
        assertEquals(Short.MIN_VALUE, v.get());
    }

    @Test
    void testShortOutMinMax() {
        var v = new Out<>(Short.MIN_VALUE);
        shortInoutMinMax(v);
        assertEquals(Short.MAX_VALUE, v.get());
    }

    @Test
    void testUshortReturn() {
        assertEquals((short) -1, ushortReturn());
    }

    @Test
    void testUshortIn() {
        ushortIn((short) -1);
    }

    @Test
    void testUshortOut() {
        var v = new Out<>((short) 0);
        ushortOut(v);
        assertEquals((short) -1, v.get());
    }

    @Test
    void testUshortOutUninitialized() {
        var v = new Out<>((short) 0);
        assertFalse(ushortOutUninitialized(v));
        assertEquals((short) 0, v.get());
    }

    @Test
    void testUshortInout() {
        var v = new Out<>((short) -1);
        ushortInout(v);
        assertEquals((short) 0, v.get());
    }

    @Test
    void testIntReturnMax() {
        assertEquals(Integer.MAX_VALUE, intReturnMax());
    }

    @Test
    void testIntReturnMin() {
        assertEquals(Integer.MIN_VALUE, intReturnMin());
    }

    @Test
    void testIntInMax() {
        intInMax(Integer.MAX_VALUE);
    }

    @Test
    void testIntInMin() {
        intInMin(Integer.MIN_VALUE);
    }

    @Test
    void testIntOutMax() {
        var v = new Out<>(0);
        intOutMax(v);
        assertEquals(Integer.MAX_VALUE, v.get());
    }

    @Test
    void testIntOutMin() {
        var v = new Out<>(0);
        intOutMin(v);
        assertEquals(Integer.MIN_VALUE, v.get());
    }

    @Test
    void testIntOutUninitialized() {
        var v = new Out<>(0);
        assertFalse(intOutUninitialized(v));
        assertEquals(0, v.get());
    }

    @Test
    void testIntOutMaxMin() {
        var v = new Out<>(Integer.MAX_VALUE);
        intInoutMaxMin(v);
        assertEquals(Integer.MIN_VALUE, v.get());
    }

    @Test
    void testIntOutMinMax() {
        var v = new Out<>(Integer.MIN_VALUE);
        intInoutMinMax(v);
        assertEquals(Integer.MAX_VALUE, v.get());
    }

    @Test
    void testUintReturn() {
        assertEquals(-1, uintReturn());
    }

    @Test
    void testUintIn() {
        uintIn(-1);
    }

    @Test
    void testUintOut() {
        var v = new Out<>(0);
        uintOut(v);
        assertEquals(-1, v.get());
    }

    @Test
    void testUintOutUninitialized() {
        var v = new Out<>(0);
        assertFalse(uintOutUninitialized(v));
        assertEquals(0, v.get());
    }

    @Test
    void testUintInout() {
        var v = new Out<>(-1);
        uintInout(v);
        assertEquals(0, v.get());
    }

    // Native long values are cast to int in java-gi, because
    // long is not 64 bits on all supported platforms. So we
    // treat it as a 32 bit value to preserve cross-platform
    // compatibility.

    @Test
    void testLongReturnMax() {
        assertEquals(-1, longReturnMax());
    }

    @Test
    void testLongReturnMin() {
        assertEquals(0, longReturnMin());
    }

    // longInMax is not supported

    // longInMin is not supported

    @Test
    void testLongOutMax() {
        var v = new Out<>(0);
        longOutMax(v);
        assertEquals(-1, v.get());
    }

    @Test
    void testLongOutMin() {
        var v = new Out<>(0);
        longOutMin(v);
        assertEquals(0, v.get());
    }

    @Test
    void testLongOutUninitialized() {
        var v = new Out<>(0);
        assertFalse(longOutUninitialized(v));
        assertEquals(0, v.get());
    }

    // longInoutMaxMin is not supported

    // longInoutMinMax is not supported

    @Test
    void testUlongReturn() {
        assertEquals(-1, ulongReturn());
    }

    @Test
    void testUlongIn() {
        ulongIn(-1);
    }

    @Test
    void testUlongOut() {
        var v = new Out<>(0);
        ulongOut(v);
        assertEquals(-1, v.get());
    }

    @Test
    void testUlongOutUninitialized() {
        var v = new Out<>(0);
        assertFalse(ulongOutUninitialized(v));
        assertEquals(0, v.get());
    }

    // ulongInout is not supported

    @Test
    void testSsizeReturnMax() {
        assertEquals(Long.MAX_VALUE, ssizeReturnMax());
    }

    @Test
    void testSsizeReturnMin() {
        assertEquals(Long.MIN_VALUE, ssizeReturnMin());
    }

    @Test
    void testSsizeInMax() {
        ssizeInMax(Long.MAX_VALUE);
    }

    @Test
    void testSsizeInMin() {
        ssizeInMin(Long.MIN_VALUE);
    }

    @Test
    void testSsizeOutMax() {
        var v = new Out<>(0L);
        ssizeOutMax(v);
        assertEquals(Long.MAX_VALUE, v.get());
    }

    @Test
    void testSsizeOutMin() {
        var v = new Out<>(0L);
        ssizeOutMin(v);
        assertEquals(Long.MIN_VALUE, v.get());
    }

    @Test
    void testSsizeOutUninitialized() {
        var v = new Out<>(0L);
        assertFalse(ssizeOutUninitialized(v));
        assertEquals(0L, v.get());
    }

    @Test
    void testSsizeOutMaxMin() {
        var v = new Out<>(Long.MAX_VALUE);
        ssizeInoutMaxMin(v);
        assertEquals(Long.MIN_VALUE, v.get());
    }

    @Test
    void testSsizeOutMinMax() {
        var v = new Out<>(Long.MIN_VALUE);
        ssizeInoutMinMax(v);
        assertEquals(Long.MAX_VALUE, v.get());
    }

    @Test
    void testSizeReturn() {
        assertEquals(-1L, sizeReturn());
    }

    @Test
    void testSizeIn() {
        sizeIn(-1);
    }

    @Test
    void testSizeOut() {
        var v = new Out<>(0L);
        sizeOut(v);
        assertEquals(-1L, v.get());
    }

    @Test
    void testSizeOutUninitialized() {
        var v = new Out<>(0L);
        assertFalse(sizeOutUninitialized(v));
        assertEquals(0L, v.get());
    }

    @Test
    void testSizeInout() {
        var v = new Out<>(-1L);
        sizeInout(v);
        assertEquals(0L, v.get());
    }

    @Test
    void testFloatReturn() {
        assertEquals(Float.MAX_VALUE, floatReturn());
    }

    @Test
    void testFloatIn() {
        floatIn(Float.MAX_VALUE);
    }

    @Test
    void testFloatOut() {
        var v = new Out<>(0f);
        floatOut(v);
        assertEquals(Float.MAX_VALUE, v.get());
    }

    @Test
    void testFloatNoncanonicalNanOut() {
        var v = new Out<>(0f);
        floatNoncanonicalNanOut(v);
        assertEquals(Float.NaN, v.get());
    }

    @Test
    void testFloatOutUninitialized() {
        var v = new Out<>(0f);
        assertFalse(floatOutUninitialized(v));
        assertEquals(0f, v.get());
    }

    @Test
    void testFloatInout() {
        var v = new Out<>(Float.MAX_VALUE);
        floatInout(v);
        // Apparently G_MINFLOAT != Float.MIN_VALUE
        assertEquals(1.1754944E-38f, v.get());
    }

    @Test
    void testDoubleReturn() {
        assertEquals(Double.MAX_VALUE, doubleReturn());
    }

    @Test
    void testDoubleIn() {
        doubleIn(Double.MAX_VALUE);
    }

    @Test
    void testDoubleOut() {
        var v = new Out<>(0d);
        doubleOut(v);
        assertEquals(Double.MAX_VALUE, v.get());
    }

    @Test
    void testDoubleNoncanonicalNanOut() {
        var v = new Out<>(0d);
        doubleNoncanonicalNanOut(v);
        assertEquals(Double.NaN, v.get());
    }

    @Test
    void testDoubleOutUninitialized() {
        var v = new Out<>(0d);
        assertFalse(doubleOutUninitialized(v));
        assertEquals(0d, v.get());
    }

    @Test
    void testDoubleInout() {
        var v = new Out<>(Double.MAX_VALUE);
        doubleInout(v);
        // Apparently G_MINDOUBLE != Double.MIN_VALUE
        assertEquals(2.2250738585072014E-308d, v.get());
    }
}
