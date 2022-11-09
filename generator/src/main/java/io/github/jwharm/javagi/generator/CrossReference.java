package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class CrossReference {

    // This method does two things:
    // 1. Create a global map of all registered types in all repositories;
    // 2. Loop through all type references in all repositories, find the 
    //    actual type instance in the parsed GI tree, and save a reference
    //    to that GirElement.
    public static void link(Map<String, Repository> repositories) {
        
        // Create the registeredTypeMap of all registered types of all GI repositories
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

        // Link all type references to the accompanying types
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
                    // If the repositories were loaded in the correct order, this will not change anything.
                    t.init(t.qualifiedName);
                }
                element = element.next;
            }
        }
    }

    // Create a map to find a type by its "c:identifier" attribute
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

    // Create a map to find a type by its "c:type" attribute
    public static Map<String, RegisteredType> createCTypeLookupTable(Map<String, Repository> repositories) {
        Map<String, RegisteredType> cTypeLookupTable = new HashMap<>();
        for (Repository gir : repositories.values()) {
            for (RegisteredType rt : gir.namespace.registeredTypeMap.values()) {
                cTypeLookupTable.put(rt.cType, rt);
            }
        }
        return cTypeLookupTable;
    }
}
