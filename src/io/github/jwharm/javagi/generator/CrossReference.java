package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class CrossReference {

    public static void link(Map<String, Repository> repositories) {
        for (Repository r : repositories.values()) {
            Namespace ns = r.namespace;
            Stream<? extends RegisteredType> registeredTypes = Stream.of(
                    ns.aliasList.stream(),
                    ns.bitfieldList.stream(),
                    ns.callbackList.stream(),
                    ns.classList.stream(),
                    ns.enumerationList.stream(),
                    ns.interfaceList.stream(),
                    ns.recordList.stream(),
                    ns.callbackList.stream(),
                    ns.unionList.stream()
            ).reduce(Stream::concat).orElseGet(Stream::empty);

            registeredTypes.forEach(rt -> ns.registeredTypeMap.put(rt.name, rt));
        }

        for (Repository repository : repositories.values()) {
            GirElement element = repository;
            while (element != null) {

                if ((element instanceof Type t)
                        && (t.name != null)
                        && (! t.isPrimitive)
                        && (! t.name.equals("none"))
                        && (! t.name.equals("utf8"))
                        && (! t.name.equals("gpointer")
                        && (! t.name.equals("gconstpointer")))) {

                    Repository r = repositories.get(t.girNamespace);
                    if (r != null) {
                        t.girElementInstance = r.namespace.registeredTypeMap.get(t.name);
                        if (t.girElementInstance != null) {
                            t.girElementType = t.girElementInstance.getClass().getSimpleName();
                        }
                    }
                    // Redo the initialization, now that all repositories have loaded.
                    t.init(t.qualifiedName);
                }
                element = element.next;
            }
        }
    }

    public static Map<String, GirElement> createIdLookupTable(Map<String, Repository> repositories) {
        Map<String, GirElement> cIdentifierLookupTable = new HashMap<>();
        for (Repository repository : repositories.values()) {
            GirElement element = repository;
            while (element != null) {
                if (element instanceof Method m) {
                    cIdentifierLookupTable.put(m.cIdentifier, m);
                } else if (element instanceof Member m) {
                    cIdentifierLookupTable.put(m.cIdentifier, m);
                }
                element = element.next;
            }
        }
        return cIdentifierLookupTable;
    }

    public static Map<String, RegisteredType> createCTypeLookupTable(Map<String, Repository> repositories) {
        Map<String, RegisteredType> cIdentifierLookupTable = new HashMap<>();
        for (Repository gir : repositories.values()) {
            for (RegisteredType rt : gir.namespace.registeredTypeMap.values()) {
                cIdentifierLookupTable.put(rt.cType, rt);
            }
        }
        return cIdentifierLookupTable;
    }
}
