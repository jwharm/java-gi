package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.util.Patch;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public class GstPatch implements Patch {

    private static final Type TYPE_INT =
            new Type(Map.of("name", "gint", "c:type", "gint"), emptyList());

    @Override
    public GirElement patch(GirElement element, String namespace) {

        if (element instanceof Type t && "GstMapFlags".equals(t.cType()))
            return TYPE_INT;

        if (!"Gst".equals(namespace))
            return element;

        if (element instanceof Namespace ns) {
            ns = add(ns, new Constant(
                    Map.of("name", "MAP_READ", "value", "1"),
                    List.of(new Doc(emptyMap(), "map for read access"),
                            TYPE_INT),
                    ns.platforms()));
            ns = add(ns, new Constant(
                    Map.of("name", "MAP_WRITE", "value", "2"),
                    List.of(new Doc(emptyMap(), "map for write access"),
                            TYPE_INT),
                    ns.platforms()));
            ns = add(ns, new Constant(
                    Map.of("name", "MAP_FLAG_LAST", "value", "65536"),
                    List.of(new Doc(emptyMap(), "first flag that can be used for custom purposes"),
                            TYPE_INT),
                    ns.platforms()));

            return remove(ns, Bitfield.class, "name", "MapFlags");
        }

        return element;
    }
}
