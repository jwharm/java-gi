package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.Constant;
import io.github.jwharm.javagi.gir.GirElement;
import io.github.jwharm.javagi.gir.Namespace;
import io.github.jwharm.javagi.util.Patch;

import java.util.List;

public class GLibConstants implements Patch {

    @Override
    public GirElement patch(GirElement element) {
        if (element instanceof Namespace ns && "GLib".equals(ns.name())) {
            List<GirElement> children = ns.children().stream().map(node -> {
                if (node instanceof Constant c && "CSET_a_2_z".equals(c.name()))
                    return changeAttribute(c, "name", "CSET_a_2_z_lowercase");
                return node;
            }).toList();
            return new Namespace(ns.attributes(), children, ns.platforms());
        }

        return element;
    }
}
