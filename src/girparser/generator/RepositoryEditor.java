package girparser.generator;

import girparser.model.Method;
import girparser.model.RegisteredType;
import girparser.model.Repository;

import java.util.Map;

public class RepositoryEditor {

    public static void applyPatches(Map<String, Repository> repositories) {
        removeBlacklistedTypes(repositories);
        update_BufferedInputStream_ReadByte(repositories);
        update_MenuButton_GetDirection(repositories);
        update_PrintUnixDialog_getSettings(repositories);
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
        // These types are defined in the GIR, but unavailable by default
        String[] blacklist = new String[] {
                "Gsk.BroadwayRenderer",
                "Gsk.BroadwayRendererClass"
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
