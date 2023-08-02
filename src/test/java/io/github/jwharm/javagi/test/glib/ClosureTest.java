package io.github.jwharm.javagi.test.glib;

import io.github.jwharm.javagi.annotations.Property;
import io.github.jwharm.javagi.types.Types;
import io.github.jwharm.javagi.util.JavaClosure;
import org.gnome.glib.Type;
import org.gnome.gobject.Binding;
import org.gnome.gobject.BindingFlags;
import org.gnome.gobject.GObject;
import org.gnome.gobject.Value;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test a GObject property binding with a transformation closure.
 */
public class ClosureTest {
    
    public interface MyInterface {
        public boolean timesTwo(MemorySegment p1, MemorySegment p2);
    }

    @Test
    public void methodReference() {
        // Create 2 objects, both with a simple "num" property of type int
        NumObject n1 = GObject.newInstance(NumObject.type);
        NumObject n2 = GObject.newInstance(NumObject.type);

        // Create a JavaClosure for the "timesTwo" method
        Method timesTwo = null;
        try {
            timesTwo = ClosureTest.class.getMethod("timesTwo", MemorySegment.class, MemorySegment.class);
        } catch (NoSuchMethodException ignored) {}
        JavaClosure closure = new JavaClosure(this, timesTwo);

        // Create a property binding to run "timesTwo" every time the "num" property on n1 or n2 is changed
        // Keep a reference to the Binding object instance alive, or else the property binding will be disconnected
        @SuppressWarnings("unused")
        Binding binding = n1.bindPropertyFull("num", n2, "num", BindingFlags.BIDIRECTIONAL, closure, closure);

        // Set the "num" property of n1 to 10
        n1.set("num", 10);

        // The "num" property of n2 should now be n1 times two
        assertEquals(20, n2.getNum());
    }

    @Test
    public void lambda() {
        // Create 2 objects, both with a simple "num" property of type int
        NumObject n1 = GObject.newInstance(NumObject.type);
        NumObject n2 = GObject.newInstance(NumObject.type);
        
        MyInterface tt = this::timesTwo;

        // Create a JavaClosure for the "timesTwo" method
        JavaClosure closure = new JavaClosure((MyInterface) this::timesTwo);

        // Create a property binding to run "timesTwo" every time the "num" property on n1 or n2 is changed
        // Keep a reference to the Binding object instance alive, or else the property binding will be disconnected
        @SuppressWarnings("unused")
        Binding binding = n1.bindPropertyFull("num", n2, "num", BindingFlags.BIDIRECTIONAL, closure, closure);

        // Set the "num" property of n1 to 10
        n1.set("num", -25);

        // The "num" property of n2 should now be n1 times two
        assertEquals(-50, n2.getNum());
    }

    // The method that is wrapped in a JavaClosure
    public boolean timesTwo(MemorySegment boxed1, MemorySegment boxed2) {
        System.out.println("Entering tt");
        Value src = new Value(boxed1);
        Value dest = new Value(boxed2);
        dest.setInt(src.getInt() * 2);
        return true;
    }

    // A simple GObject-derived class with a "num" property
    public static class NumObject extends GObject {
        public static Type type = Types.register(NumObject.class);
        public NumObject(MemorySegment address) {
            super(address);
        }

        private int num;

        @SuppressWarnings("unused")
        @Property(name="num") public void setNum(int num) {
            this.num = num;
        }
        @Property(name="num") public int getNum() {
            return this.num;
        }
    }
}
