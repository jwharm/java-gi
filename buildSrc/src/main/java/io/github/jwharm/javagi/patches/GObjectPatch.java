package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.GirElement;
import io.github.jwharm.javagi.gir.Method;
import io.github.jwharm.javagi.gir.Namespace;
import io.github.jwharm.javagi.util.Patch;

public class GObjectPatch implements Patch {

    @Override
    public GirElement patch(GirElement element) {

        if (element instanceof Namespace ns
                && "GObject".equals(ns.name())) {
            /*
             * VaList parameters are excluded from the Java bindings.
             * Therefore, the VaList marshaller classes and the
             * "signal_set_va_marshaller" function are excluded too.
             */
            ns = removeType(ns,
                    "VaClosureMarshal",
                    "SignalCVaMarshaller"
            );
            return removeFunction(ns, "signal_set_va_marshaller");
        }

        /*
         * The method "g_type_module_use" overrides "g_type_plugin_use", but
         * with a different return type. This is not allowed in Java.
         * Therefore, it is renamed from "use" to "use_type_module".
         */
        if (element instanceof Method m
                && "g_type_module_use".equals(m.attrs().cIdentifier()))
            return rename(element, "use_type_module");

        return element;
    }
}
