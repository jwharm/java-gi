package io.github.jwharm.javagi.test.gtk;

import org.gnome.graphene.Point;
import org.gnome.graphene.Rect;
import org.gnome.graphene.Size;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test creating, reading and writing nested structs
 */
public class NestedStructTest {

    @Test
    public void testGrapheneRect() {
        float x = 1.1f;
        float y = 2.22f;
        float w = 3.333f;
        float h = 4.4444f;
        float newX = 9.87654f;
        float newH = 100.0001f;

        try (var arena = Arena.ofConfined()) {
            var rect = new Rect(Point.alloc().init(x, y), Size.alloc().init(w, h), arena);

            assertEquals(x, rect.readOrigin().readX());
            assertEquals(y, rect.readOrigin().readY());
            assertEquals(w, rect.readSize().readWidth());
            assertEquals(h, rect.readSize().readHeight());

            rect.writeOrigin(Point.alloc().init(newX, y));
            rect.writeSize(Size.alloc().init(w, newH));

            assertEquals(newX, rect.readOrigin().readX());
            assertEquals(y, rect.readOrigin().readY());
            assertEquals(w, rect.readSize().readWidth());
            assertEquals(newH, rect.readSize().readHeight());
        }
    }
}
