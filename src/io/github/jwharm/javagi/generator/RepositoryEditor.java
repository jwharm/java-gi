package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.Method;
import io.github.jwharm.javagi.model.RegisteredType;
import io.github.jwharm.javagi.model.Repository;

import java.util.Map;

public class RepositoryEditor {

    public static void applyPatches(Map<String, Repository> repositories) {
        // These types are defined in the GIR, but unavailable by default
        removeType(repositories, "Gsk", "BroadwayRenderer");
        removeType(repositories, "Gsk", "BroadwayRendererClass");
        // These types require mapping va_list (varargs) types
        removeType(repositories, "GObject", "VaClosureMarshal");
        removeType(repositories, "GObject", "SignalCVaMarshaller");

        // These methods override (or are overridden by) another method, but with a different return type
        renameMethod(repositories, "Gtk", "PrintUnixDialog", "get_settings", "get_print_settings");
        renameMethod(repositories, "Gio", "BufferedInputStream", "read_byte", "read_int");
        renameMethod(repositories, "Gtk", "MenuButton", "get_direction", "get_arrow_direction");
        renameMethod(repositories, "GObject", "TypeModule", "use", "use_type_module");

        // g_async_initable_new_finish is a method declaration in the interface AsyncInitable.
        // It is meant to be implemented as a constructor (actually, a static factory method).
        // However, Java does not allow a (non-static) method to be implemented/overridden by a
        // static method.
        // The solution is to remove the method from the interface. It is still available in the
        // implementing classes.
        removeMethod(repositories, "Gio", "AsyncInitable", "new_finish");
    }

    private static void removeMethod(Map<String, Repository> repositories,
                                     String ns,
                                     String type,
                                     String method) {
        Method m = findMethod(repositories, ns, type, method);
        if (m != null) {
            var parent = m.parent;
            parent.methodList.remove(m);
        }
    }

    private static void renameMethod(Map<String, Repository> repositories,
                                     String ns,
                                     String type,
                                     String oldName,
                                     String newName) {
        Method m = findMethod(repositories, ns, type, oldName);
        if (m != null) {
            m.name = newName;
        }
    }

    private static void removeType(Map<String, Repository> repositories,
                                   String ns,
                                   String type) {
        Repository gir = repositories.get(ns);
        if (gir != null) {
            gir.namespace.registeredTypeMap.remove(type);
        }
    }

    private static Method findMethod(Map<String, Repository> repositories,
                                     String ns,
                                     String type,
                                     String method) {
        try {
            RegisteredType t = repositories.get(ns).namespace.registeredTypeMap.get(type);
            for (Method m : t.methodList) {
                if (method.equals(m.name)) {
                    return m;
                }
            }
        } catch (NullPointerException npe) {
            return null;
        }
        return null;
    }
}
