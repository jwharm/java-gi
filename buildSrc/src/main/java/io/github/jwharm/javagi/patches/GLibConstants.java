package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.Constant;
import io.github.jwharm.javagi.gir.GirElement;
import io.github.jwharm.javagi.gir.Namespace;
import io.github.jwharm.javagi.util.Patch;

import java.util.List;

/**
 * Constant names are all uppercase in Java. GLib however defines
 * "CSET_A_2_Z" and "CSET_a_2_z". To prevent duplicate fields, the
 * second one is renamed to "CSET_a_2_z_lowercase".
 */
public class GLibConstants implements Patch {

    @Override
    public GirElement patch(GirElement element) {
        if (element instanceof Namespace ns && "GLib".equals(ns.name())) {
            List<GirElement> children = ns.children().stream().map(node -> {
                if (node instanceof Constant c && "CSET_a_2_z".equals(c.name()))
                    return rename(c, "CSET_a_2_z_lowercase");
                return node;
            }).toList();
            return ns.withChildren(children);
        }

        return element;
    }
}
