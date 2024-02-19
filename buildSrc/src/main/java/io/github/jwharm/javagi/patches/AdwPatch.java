package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.GirElement;
import io.github.jwharm.javagi.gir.Method;
import io.github.jwharm.javagi.util.Patch;

public class AdwPatch implements Patch {

    @Override
    public GirElement patch(GirElement element, String namespace) {

        if (!"Adw".equals(namespace))
            return element;

        /*
         * SplitButton.getDirection() overrides Widget.getDirection() with a
         * different return type. Rename to getArrowDirection()
         */
        if (element instanceof Method m
                && "adw_split_button_get_direction".equals(m.attrs().cIdentifier()))
            return m.withAttribute("name", "get_arrow_direction");

        return element;
    }
}
