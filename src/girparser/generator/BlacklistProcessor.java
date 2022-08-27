package girparser.generator;

import girparser.model.Repository;

public class BlacklistProcessor {

    public static void filterBlacklistedTypes(Repository gir, String[] blacklist) {
        for (String qualifiedType : blacklist) {
            String[] parts = qualifiedType.split("\\.");

            String ns = parts[0];
            String simpleType = parts[1];

            if (gir.namespace.name.equals(ns)) {
                gir.namespace.registeredTypeMap.remove(simpleType);
            }
        }
    }
}
