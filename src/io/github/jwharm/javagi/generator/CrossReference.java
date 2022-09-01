package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.*;

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
                }
                element = element.next;
            }
        }
    }
}
