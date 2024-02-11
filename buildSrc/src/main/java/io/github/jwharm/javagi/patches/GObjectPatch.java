package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.Patch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
            ns = remove(ns, Function.class, "name", "signal_set_va_marshaller");

            /*
             * GLib and GObject both define gtype as an alias to gsize. We
             * change the Java class for the gtype declaration in GObject to
             * inherit from the GLib gtype, so the instances of both classes
             * can be used interchangeably in most cases.
             */
            var gtype = ns.registeredTypes().get("Type");
            var children = new ArrayList<>(removeType(ns, "Type").children());
            Type type = new Type(
                    Map.of("name", "GLib.Type", "c:type", "gtype"),
                    Collections.emptyList()
            );
            children.add(gtype.withChildren(List.of(gtype.infoElements().doc(), type)));
            return ns.withChildren(children);
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
        if (element instanceof Record r
                && "GWeakRef".equals(r.cType()))
            return r.withAttribute("java-gi-generic", "1");

        /*
         * Change GInitiallyUnownedClass struct to refer to GObjectClass. Both
         * structs are identical, so this has no practical consequences,
         * besides convincing the bindings generator that
         * GObject.InitiallyUnownedClass is not a fundamental type class, but
         * extends GObject.ObjectClass.
         */
        if (element instanceof Record r
                && "GInitiallyUnownedClass".equals(r.cType())) {
            Type type = new Type(
                    Map.of("name", "GObject.ObjectClass", "c:type", "GObjectClass"),
                    Collections.emptyList()
            );
            Field field = new Field(
                    Map.of("name", "parent_class"),
                    List.of(type)
            );
            return r.withChildren(List.of(r.infoElements().doc(), field));
        }

        return element;
    }
}
