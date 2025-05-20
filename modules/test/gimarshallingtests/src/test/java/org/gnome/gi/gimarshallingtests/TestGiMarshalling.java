package org.gnome.gi.gimarshallingtests;

import io.github.jwharm.javagi.base.Out;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

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
        var v = new Out<Boolean>();
        booleanOutUninitialized(v);
        assertFalse(v.get());
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
}
