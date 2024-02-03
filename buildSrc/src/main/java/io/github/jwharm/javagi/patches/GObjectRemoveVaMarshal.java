package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.GirElement;
import io.github.jwharm.javagi.gir.Namespace;
import io.github.jwharm.javagi.util.Patch;

public class GObjectRemoveVaMarshal implements Patch {

    @Override
    public GirElement patch(GirElement element) {
        if (element instanceof Namespace ns && "GObject".equals(ns.name())) {
            Namespace result = removeType(ns, "VaClosureMarshal", "SignalCVaMarshaller");
            return removeFunction(result, "signal_set_va_marshaller");
        }

        return element;
    }
}
