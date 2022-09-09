package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.Method;
import io.github.jwharm.javagi.model.RegisteredType;
import io.github.jwharm.javagi.model.Repository;

import java.util.Map;

public class RepositoryEditor {

    public static void applyPatches(Map<String, Repository> repositories) {
        removeBlacklistedTypes(repositories);
        update_BufferedInputStream_ReadByte(repositories);
        update_MenuButton_GetDirection(repositories);
        update_PrintUnixDialog_getSettings(repositories);
        update_TypeModule_use(repositories);
        remove_AsyncInitable_newFinish(repositories);
    }

    /**
     * g_async_initable_new_finish is a method declaration in the interface AsyncInitable.
     * It is meant to be implemented as a constructor (actually, a static factory method).
     * However, Java does not allow a (non-static) method to be implemented/overridden by a
     * static method.
     * The solution is to remove the method from the interface. It is still available in the
     * implementing classes.
     */
    private static void remove_AsyncInitable_newFinish(Map<String, Repository> repositories) {
        Method method = findMethod(repositories, "Gio", "AsyncInitable", "new_finish");
        if (method != null) {
            var parent = method.parent;
            parent.methodList.remove(method);
        }
    }

    private static void update_TypeModule_use(Map<String, Repository> repositories) {
        Method method = findMethod(repositories, "GObject", "TypeModule", "use");
        if (method != null) {
            method.name = "use_type_module";
        }
    }

    private static void update_BufferedInputStream_ReadByte(Map<String, Repository> repositories) {
        Method method = findMethod(repositories, "Gio", "BufferedInputStream", "read_byte");
        if (method != null) {
            method.name = "read_int";
        }
    }

    private static void update_MenuButton_GetDirection(Map<String, Repository> repositories) {
        Method method = findMethod(repositories, "Gtk", "MenuButton", "get_direction");
        if (method != null) {
            method.name = "get_arrow_direction";
        }
    }

    private static void update_PrintUnixDialog_getSettings(Map<String, Repository> repositories) {
        Method method = findMethod(repositories, "Gtk", "PrintUnixDialog", "get_settings");
        if (method != null) {
            method.name = "get_print_settings";
        }
    }

    private static void removeBlacklistedTypes(Map<String, Repository> repositories) {
        String[] blacklist = new String[] {
                // These types are defined in the GIR, but unavailable by default
                "Gsk.BroadwayRenderer",
                "Gsk.BroadwayRendererClass",
                // These types require mapping va_list (varargs) types
                "GObject.VaClosureMarshal",
                "GObject.SignalCVaMarshaller"
        };

        for (String qualifiedType : blacklist) {
            String[] parts = qualifiedType.split("\\.");

            String ns = parts[0];
            String simpleType = parts[1];

            Repository gir = repositories.get(ns);
            if (gir != null) {
                gir.namespace.registeredTypeMap.remove(simpleType);
            }
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
