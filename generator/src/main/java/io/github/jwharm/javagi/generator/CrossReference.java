package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.*;

import java.util.stream.Stream;

public class CrossReference {

    /**
     * This method does two things:
     * <ul>
     *     <li>Create a global map of all registered types in all repositories;</li>
     *     <li>
     *         Loop through all type references in all repositories, find the
     *         actual type instance in the parsed GI tree, and save a reference
     *         to that GirElement.
     *     </li>
     * </ul>
     */
    public static void link() {
        // Create the registeredTypeMap of all registered types of all GI repositories
        for (Repository r : Conversions.repositoriesLookupTable.values()) {
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
        for (Repository repository : Conversions.repositoriesLookupTable.values()) {
            GirElement element = repository;
            while (element != null) {

                if ((element instanceof Type t)
                        && (t.name != null)
                        && (! t.isPrimitive)
                        && (! t.name.equals("none"))
                        && (! t.name.equals("utf8"))
                        && (! t.name.equals("gpointer")
                        && (! t.name.equals("gconstpointer")))) {

                    Repository r = Conversions.repositoriesLookupTable.get(t.girNamespace);
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

    /**
     * Update {@code cIdentifierLookupTable} with current {@code repositoriesLookupTable}
     */
    public static void createIdLookupTable() {
        Conversions.cIdentifierLookupTable.clear();
        for (Repository repository : Conversions.repositoriesLookupTable.values()) {
            GirElement element = repository;
            while (element != null) {
                if (element instanceof Method m) {
                    Conversions.cIdentifierLookupTable.put(m.cIdentifier, m);
                } else if (element instanceof Member m) {
                    Conversions.cIdentifierLookupTable.put(m.cIdentifier, m);
                }
                element = element.next;
            }
        }
    }

    /**
     * Update {@code cTypeLookupTable} with current {@code repositoriesLookupTable}
     */
    public static void createCTypeLookupTable() {
        Conversions.cTypeLookupTable.clear();
        for (Repository gir : Conversions.repositoriesLookupTable.values()) {
            for (RegisteredType rt : gir.namespace.registeredTypeMap.values()) {
                Conversions.cTypeLookupTable.put(rt.cType, rt);
            }
        }
    }
}
