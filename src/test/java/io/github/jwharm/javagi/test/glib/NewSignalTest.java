package io.github.jwharm.javagi.test.glib;

import io.github.jwharm.javagi.annotations.GType;
import io.github.jwharm.javagi.annotations.Property;
import io.github.jwharm.javagi.annotations.RegisteredType;
import io.github.jwharm.javagi.annotations.Signal;
import io.github.jwharm.javagi.types.Types;
import org.gnome.glib.Type;
import org.gnome.gobject.GObject;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test user-defined signals:
 * <ul>
 * <li>Defining a custom signal on a GObject-derived class
 * <li>Connecting to the custom signal
 * <li>Emitting the custom signal
 * </ul>
 */
public class NewSignalTest {

    @Test
    void registerSignal() {
        // Create a Counter object (see below) with limit 10
        Counter counter = GObject.newInstance(Counter.getType(), "limit", 10);
        AtomicBoolean success = new AtomicBoolean(false);

        // Connect to the "limit-reached" signal
        counter.connect("limit-reached", (Counter.LimitReached) (max) -> success.set(true));

        // First count to 9, this should not run the callback.
        for (int a = 0; a < 9; a++)
            counter.count();
        assertFalse(success.get());

        // Now increase the count to 10, the limit. This should run the callback.
        counter.count();
        assertTrue(success.get());
    }

    /**
     * Simple GObject-derived class that can count up to a predefined maximum number.
     * When the maximum number is reached, the "limit-reached" signal is emitted.
     * The class exposes two properties: the current count ("count") and the limit ("limit").
     */
    @RegisteredType(name="TestCounter")
    public static class Counter extends GObject {
        private static final Type gtype = Types.register(Counter.class);

        @GType
        public static Type getType() {
            return gtype;
        }

        public Counter(MemorySegment address) {
            super(address);
        }

        @Signal
        public interface LimitReached extends IntConsumer {}

        private int num = 0;
        private int limit;

        @Property(name="count", writable=false)
        public int getCount() {
            return num;
        }

        @Property(name="limit")
        public void setLimit(int limit) {
            this.limit = limit;
        }

        @Property(name="limit")
        public int getLimit() {
            return this.limit;
        }

        public void count() {
            num++;
            if (num == limit) {
                emit("limit-reached", limit);
            }
        }
    }
}
