package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.util.Patch;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public class GstPatch implements Patch {

    // Utility function quickly create a <type name="gint" c:type="gint"/>
    private static Type gintType() {
        return new Type(Map.of("name", "gint", "c:type", "gint"), emptyList());
    }

    @Override
    public GirElement patch(GirElement element, String namespace) {

        /*
         * GstMapFlags is an "extendable" bitfield type. Flags values are added
         * in other namespaces. Java doesn't allow extending an enum, so we
         * generate integer constants instead.
         */

        // Replace all references to GstMapFlags with integers
        if (element instanceof Type t && "GstMapFlags".equals(t.cType()))
            return gintType();

        if (!"Gst".equals(namespace))
            return element;

        if (element instanceof Namespace ns) {
            // Add integer constants for all GstMapFlags members
            ns = add(ns, new Constant(
                    Map.of("name", "MAP_READ", "value", "1"),
                    List.of(new Doc(emptyMap(), "map for read access"),
                            gintType()),
                    ns.platforms()));
            ns = add(ns, new Constant(
                    Map.of("name", "MAP_WRITE", "value", "2"),
                    List.of(new Doc(emptyMap(), "map for write access"),
                            gintType()),
                    ns.platforms()));
            ns = add(ns, new Constant(
                    Map.of("name", "MAP_FLAG_LAST", "value", "65536"),
                    List.of(new Doc(emptyMap(), "first flag that can be used for custom purposes"),
                            gintType()),
                    ns.platforms()));

            // Remove GstMapFlags
            return remove(ns, Bitfield.class, "name", "MapFlags");
        }

        return element;
    }
}
