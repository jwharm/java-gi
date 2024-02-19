package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.Patch;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GObjectPatch implements Patch {

    @Override
    public GirElement patch(GirElement element, String namespace) {

        if (!"GObject".equals(namespace))
            return element;

        if (element instanceof Namespace ns) {
            /*
             * VaList parameters are excluded from the Java bindings.
             * Therefore, the VaList marshaller classes and the
             * "signal_set_va_marshaller" function are excluded too.
             */
            ns = remove(ns, Callback.class, "name", "VaClosureMarshal");
            ns = remove(ns, Alias.class, "name", "SignalCVaMarshaller");
            ns = remove(ns, Function.class, "name", "signal_set_va_marshaller");
            return ns;
        }

        /*
         * GLib and GObject both define gtype as an alias to gsize. We replace
         * the gtype declaration in GObject with an alias for the GLib gtype,
         * so it will inherit in Java and the instances of both classes can be
         * used interchangeably in many cases.
         */
        if (element instanceof Alias a && "Type".equals(a.name())) {
            Type type = new Type(
                    Map.of("name", "GLib.Type", "c:type", "gtype"),
                    Collections.emptyList()
            );
            return a.withChildren(a.infoElements().doc(), type);
        }

        /*
         * The method "g_type_module_use" overrides "g_type_plugin_use", but
         * with a different return type. This is not allowed in Java.
         * Therefore, it is renamed from "use" to "use_type_module".
         */
        if (element instanceof Method m
                && "g_type_module_use".equals(m.attrs().cIdentifier()))
            return m.withAttribute("name", "use_type_module");

        /*
         * Make GWeakRef generic (replacing all GObject arguments with generic
         * type {@code <T extends GObject>}.
         */
        if (element instanceof Record r && "WeakRef".equals(r.name()))
            return r.withAttribute("java-gi-generic", "1");

        /*
         * Change GInitiallyUnownedClass struct to refer to GObjectClass. Both
         * structs are identical, so this has no practical consequences,
         * besides convincing the bindings generator that
         * GObject.InitiallyUnownedClass is not a fundamental type class, but
         * extends GObject.ObjectClass.
         */
        if (element instanceof Record r && "InitiallyUnownedClass".equals(r.name())) {
            Type type = new Type(
                    Map.of("name", "GObject.ObjectClass", "c:type", "GObjectClass"),
                    Collections.emptyList()
            );
            Field field = new Field(
                    Map.of("name", "parent_class"),
                    List.of(type)
            );
            return r.withChildren(r.infoElements().doc(), field);
        }

        /*
         * GObject.notify() is defined as a virtual method with an invoker
         * method, but the parameters are different. Remove the invoker
         * attribute, so they will be treated as separate methods.
         */
        if (element instanceof VirtualMethod vm
                && "notify".equals(vm.name())
                && "Object".equals(vm.parameters().instanceParameter().type().name()))
            return new VirtualMethod(
                    Map.of("name", "notify"),
                    vm.children(),
                    vm.platforms()
            );

        return element;
    }
}
