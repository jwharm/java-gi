package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.Patch;
import io.github.jwharm.javagi.util.Platform;

import java.util.List;

import static java.util.function.Predicate.not;

public class GLibPatch implements Patch {

    @Override
    public GirElement patch(GirElement element) {

        if (element instanceof Namespace ns
                && ns.name().equals("GLib")) {
            /*
             * Constant names are all uppercase in Java. GLib however defines
             * "CSET_A_2_Z" and "CSET_a_2_z". To prevent duplicate fields,
             * the second one is renamed to "CSET_a_2_z_lowercase".
             */
            ns = ns.withChildren(ns.children().stream().map(node -> {
                if (node instanceof Constant c
                        && "CSET_a_2_z".equals(c.name()))
                    return c.withAttribute("name", "CSET_a_2_z_lowercase");
                return node;
            }).toList());

            /*
             * g_clear_error has attribute throws="1" but no gerror** parameter
             * (or any other parameters) in the gir file.
             */
            ns = removeFunction(ns, "clear_error");

            /*
             * GPid is defined as gint on Unix vs gpointer on Windows. The
             * generated Java class is an int Alias, so we remove the Windows
             * support.
             */
            ns.registeredTypes().get("Pid")
                    .setPlatforms(Platform.LINUX | Platform.MACOS);

            return ns;
        }

        /*
         * GVariant has a method "get_type" that clashes with the "getType()"
         * method that is generated in Java. Therefore, it is renamed to
         * "getVariantType()".
         */
        if (element instanceof Method m
                && "g_variant_get_type".equals(m.attrs().cIdentifier()))
            return m.withAttribute("name", "get_variant_type");

        /*
         * The functions "g_main_context_query" and "g_main_context_check" have
         * GPollFD[] parameters. Because the size of GPollFD is unknown, it is
         * not possible to allocate the array with the correct size. For this
         * reason, both methods are excluded.
         */
        List<String> toRemove =
                List.of("g_main_context_query", "g_main_context_check");

        if (element instanceof Record rec
                && "GMainContext".equals(rec.cType()))
            return rec.withChildren(rec.children().stream().filter(not(node ->
                    node instanceof Method m
                            && toRemove.contains(m.attrs().cIdentifier())
            )).toList());

        return element;
    }
}
