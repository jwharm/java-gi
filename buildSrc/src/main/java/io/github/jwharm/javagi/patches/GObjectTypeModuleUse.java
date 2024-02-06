package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.GirElement;
import io.github.jwharm.javagi.gir.Method;
import io.github.jwharm.javagi.util.Patch;

public class GObjectTypeModuleUse implements Patch {

    @Override
    public GirElement patch(GirElement element) {
        if (element instanceof Method m && "g_type_module_use".equals(m.attrs().cIdentifier()))
            return rename(element, "use_type_module");

        return element;
    }
}
