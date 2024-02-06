package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.GirElement;
import io.github.jwharm.javagi.gir.Method;
import io.github.jwharm.javagi.util.Patch;

/**
 * GVariant has a method "get_type" that clashes with the
 * "getType()" method that is generated in Java. Therefore,
 * it is renamed to "getVariantType()".
 */
public class GLibVariantGetType implements Patch {

    @Override
    public GirElement patch(GirElement element) {
        if (element instanceof Method m && "g_variant_get_type".equals(m.attrs().cIdentifier()))
            return rename(element, "get_variant_type");

        return element;
    }
}
