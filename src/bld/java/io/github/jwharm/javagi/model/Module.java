package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.JavaGIOperation;
import io.github.jwharm.javagi.generator.Platform;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class Module {
    /**
     * Map to find repositories by their name
     */
    public final Map<String, Repository> repositoriesLookupTable = new HashMap<>();

    /**
     * Map to find java packages by namespaces
     */
    public final Map<String, String> nsLookupTable = new HashMap<>();

    /**
     * Map to find elements by their {@code c:identifier} attribute
     */
    public final Map<String, GirElement> cIdentifierLookupTable = new HashMap<>();

    /**
     * Map to find types by their {@code c:type} attribute
     */
    public final Map<String, RegisteredType> cTypeLookupTable = new HashMap<>();

    /**
     * Map to find parent types by a types qualified name
     */
    public final Map<String, String> superLookupTable = new HashMap<>();

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
    public void link() {

        // Create the registeredTypeMap of all registered types of all GI repositories
        for (Repository r : repositoriesLookupTable.values()) {
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
        for (Repository repository : repositoriesLookupTable.values()) {
            GirElement element = repository;
            while (element != null) {

                if ((element instanceof Type t)
                        && (t.name != null)
                        && (! t.isPrimitive)
                        && (! t.name.equals("none"))
                        && (! t.name.equals("utf8"))
                        && (! t.name.equals("gpointer")
                        && (! t.name.equals("gconstpointer")))) {

                    Repository r = repositoriesLookupTable.get(t.girNamespace);
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

                // Link length-parameters to the corresponding arrays
                if (element instanceof Array array) {
                    if (array.length != null && array.parent instanceof Parameter p) {
                        Parameter lp = p.getParameterAt(array.length);
                        lp.linkedArray = array;
                    }
                }

                element = element.next;
            }
        }
    }

    /**
     * This method will parse the sources for each platform, and filter out all platform-specific sources,
     * global types and global functions. The result will only contain sources and types that exist on all
     * platforms (lowest common denominator). This is used to create the common-api jar.
     * @param  sources Map of all sources for all platforms
     * @return a map of parsed sources that only contains sources and global types and functions that exist
     *         on all platforms
     * @throws ParserConfigurationException if an error occurs while parsing the sources
     * @throws SAXException if an error occurs while parsing the sources
     */
    public Map<String, JavaGIOperation.Parsed> getCommonApi(Map<Platform, JavaGIOperation.Source[]> sources) throws ParserConfigurationException, SAXException {

        Map<String, JavaGIOperation.Parsed> sourcesLin = null; // JavaGIOperation.parse(null, sources.get(Platform.LINUX));
        Map<String, JavaGIOperation.Parsed> sourcesWin = null; // JavaGIOperation.parse(null, sources.get(Platform.WINDOWS));
        Map<String, JavaGIOperation.Parsed> sourcesMac = null; //JavaGIOperation.parse(null, sources.get(Platform.MAC));

        // Remove platform-specific sources
        sourcesLin.entrySet().removeIf(entry -> {
            var name = entry.getKey();
            var parsed = entry.getValue();

            // Skip sources that are only included as dependencies, and are not generated
            if (! parsed.generate()) {
                return false;
            }

            // Check if the source exists on all platforms
            return (sourcesWin.get(name) == null || sourcesMac.get(name) == null);
        });

        // Remove platform-specific types and functions
        sourcesLin.forEach((name, parsed) -> {
            // Skip sources that are only included as dependencies, and are not generated
            if (parsed.generate()) {

                // Get the namespaces of all platforms
                var nsLin = parsed.repository().getNamespace();
                var nsWin = sourcesWin.get(name).repository().getNamespace();
                var nsMac = sourcesMac.get(name).repository().getNamespace();

                // Get the type dictionary for all platforms
                var typesLin = nsLin.registeredTypeMap;
                var typesWin = nsWin.registeredTypeMap;
                var typesMac = nsMac.registeredTypeMap;

                // Remove types that don't exist on all platforms
                typesLin.keySet().removeIf(typeName -> typesWin.get(typeName) == null || typesMac.get(typeName) == null);

                // Generate list of function specifications for the other platforms
                Set<String> funcsWin = new HashSet<>();
                for (Function function : nsWin.functionList) {
                    funcsWin.add(function.getMethodSpecification());
                }
                Set<String> funcsMac = new HashSet<>();
                for (Function function : nsMac.functionList) {
                    funcsMac.add(function.getMethodSpecification());
                }

                // Compare function specifications and remove the ones that don't exist on the other platforms
                nsLin.functionList.removeIf(function -> {
                    String spec = function.getMethodSpecification();
                    return ! (funcsWin.contains(spec) && funcsMac.contains(spec));
                });
            }
        });

        return sourcesLin;
    }

    /**
     * Update {@code cIdentifierLookupTable} with current {@code repositoriesLookupTable}
     */
    public void createIdLookupTable() {
        cIdentifierLookupTable.clear();
        for (Repository repository : repositoriesLookupTable.values()) {
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
    }

    /**
     * Update {@code cTypeLookupTable} with current {@code repositoriesLookupTable}
     */
    public void createCTypeLookupTable() {
        cTypeLookupTable.clear();
        for (Repository gir : repositoriesLookupTable.values()) {
            for (RegisteredType rt : gir.namespace.registeredTypeMap.values()) {
                cTypeLookupTable.put(rt.cType, rt);
            }
        }
    }
}
