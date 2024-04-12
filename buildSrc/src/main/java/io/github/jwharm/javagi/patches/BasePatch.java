package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.GirElement;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.Patch;

/**
 * This patch will apply to all GIR files
 */
public class BasePatch implements Patch {

    @Override
    public GirElement patch(GirElement element, String namespace) {

        /*
         * Do not generate record types named "...Private", except for GPrivate
         */
        if (element instanceof Record rec
                && (! "GPrivate".equals(rec.cType()))
                && rec.name() != null
                && rec.name().endsWith("Private"))
            return element.withAttribute("java-gi-skip", "1");

        return element;
    }
}
