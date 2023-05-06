package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.*;
import io.github.jwharm.javagi.model.Class;
import io.github.jwharm.javagi.model.Module;

import java.util.*;

/**
 * Merge platform-specific modules into one multi-platform module.
 * Registers for all repositories, types and methods/functions on which
 * platform they exist.
 */
public class Merge {

    /**
     * Merge the platform-specific modules into one merged module
     * @param platformModules the platform-specific modules
     * @return the merged module
     */
    public Module merge(Module... platformModules) {
        return merge(Arrays.asList(platformModules));
    }

    /**
     * Merge the platform-specific modules into one merged module
     * @param platformModules the platform-specific modules
     * @return the merged module
     */
    public Module merge(List<Module> platformModules) {
        Module merged = new Module(null);

        // Filter empty modules
        List<Module> modules = platformModules.stream().filter(Objects::nonNull).toList();

        // Merge repositories
        mergeRepositories(merged, modules);

        // Merge registered types
        merged.repositories.forEach((name, repository) -> mergeRegisteredTypes(repository.namespace,
                modules.stream().map(module -> module.repositories.get(name).namespace).toList()));

        // Loop through the repositories, and merge globally defined methods
        merged.repositories.forEach((name, repository) -> mergeMethods(repository.namespace,
                modules.stream().map(module -> module.repositories.get(name).namespace).toList()));

        // Loop through the repositories, loop through the registered types, and merge methods
        merged.repositories.forEach((name, repository) -> {
            List<Map<String, RegisteredType>> typeMaps = modules.stream().map(module ->
                    module.repositories.get(name).namespace.registeredTypeMap).toList();

            repository.namespace.registeredTypeMap.forEach((typeName, registeredType) ->
                    mergeMethods(registeredType, typeMaps.stream().map(tm -> tm.get(typeName)).toList()));
        });

        return merged;
    }

    /**
     * Add all repositories from the platform modules to the merged module, and
     * for each repository, check on which platform it exists
     */
    private void mergeRepositories(Module merged, List<Module> platformModules) {
        for (var module : platformModules) {
            module.repositories.forEach((name, repository) -> merged.repositories.put(name, repository.copy()));
        }
        for (String name : merged.repositories.keySet()) {
            for (var module : platformModules) {
                if (module.repositories.containsKey(name)) {
                    merged.repositories.get(name).platforms.add(module.platform);
                }
            }
        }
    }

    /**
     * Add all registered types (Class, Interface, Record, Union, Callback, Bitfield,
     * Enumeration, Alias) to the merged namespace, and for each type, check on which
     * platform it exists, and override long values to int (for Windows compatibility)
     * @param multi the merged namespace
     * @param namespaces a list of three namespaces (linux, windows and mac)
     */
    private void mergeRegisteredTypes(Namespace multi, List<Namespace> namespaces) {
        for (var ns : namespaces) {
            multi.registeredTypeMap.putAll(ns.registeredTypeMap);
        }
        for (String name : multi.registeredTypeMap.keySet()) {
            for (var ns : namespaces) {
                if (ns.registeredTypeMap.containsKey(name)) {
                    var rt = multi.registeredTypeMap.get(name);
                    rt.platforms.add(ns.module().platform);
                    if (ns.module().platform == Platform.WINDOWS) {
                        if (rt instanceof Class cls) {
                            overrideLongValues(cls); // Convert glong/gulong properties to gint/guint
                        } else if (rt instanceof Alias alias) {
                            overrideLongValues(alias); // Aliases (typedefs) for glong/gulong primitive values
                        }
                    }
                }
            }
        }
    }

    /**
     * Add all methods (Methods, VirtualMethods or Functions) to the merged class
     * (avoiding duplicate signatures) and for each method, check on which platforms it exists
     * @param multi the merged class
     * @param registeredTypes a list of three classes (linux, windows and mac)
     */
    private void mergeMethods(GirElement multi, List<? extends GirElement> registeredTypes) {
        for (var rt : registeredTypes) {
            if (rt == null) {
                continue;
            }
            mergeMethods(multi, multi.methodList, rt.methodList);
            mergeMethods(multi, multi.virtualMethodList, rt.virtualMethodList);
            mergeMethods(multi, multi.functionList, rt.functionList);
            mergeMethods(multi, multi.constructorList, rt.constructorList);
        }
        setMethodPlatforms(multi.methodList, registeredTypes);
        setMethodPlatforms(multi.virtualMethodList, registeredTypes);
        setMethodPlatforms(multi.functionList, registeredTypes);
        setMethodPlatforms(multi.constructorList, registeredTypes);

        overrideLongValues(multi.methodList, registeredTypes);
        overrideLongValues(multi.virtualMethodList, registeredTypes);
        overrideLongValues(multi.functionList, registeredTypes);
        overrideLongValues(multi.constructorList, registeredTypes);
    }

    /**
     * Add the methods (Methods, VirtualMethods or Functions) into the merged list of methods,
     * detecting duplicate signatures
     * @param multi the merged class
     * @param methods the list of methods to add
     */
    private <T extends Method> void mergeMethods(GirElement parent, List<T> multi, List<T> methods) {
        Set<String> signatures = new HashSet<>(multi.stream().map(Method::getMethodSpecification).toList());
        Set<String> cIdentifiers = new HashSet<>(multi.stream().map(method -> method.cIdentifier).toList());
        for (T method : methods) {
            var signature = method.getMethodSpecification();
            var cIdentifier = method.cIdentifier;
            if (cIdentifier != null && cIdentifiers.contains(cIdentifier)) {
                continue;
            }
            if (signatures.contains(signature)) {
                continue;
            }
            multi.add(method);
            signatures.add(signature);
            cIdentifiers.add(cIdentifier);
            method.parent = parent;
        }
    }

    /**
     * For each platform, check if the method exists
     * @param methods the merged list of methods (Methods, VirtualMethods or Functions)
     * @param registeredTypes a list of three classes (linux, windows and mac)
     */
    private void setMethodPlatforms(List<? extends Method> methods, List<? extends GirElement> registeredTypes) {
        for (Method method : methods) {
            String signature = method.getMethodSpecification();
            for (var rt : registeredTypes) {
                if (rt == null) {
                    continue;
                }
                if (rt.methodList.stream().map(Method::getMethodSpecification).anyMatch(signature::equals)) {
                    method.platforms.add(rt.module().platform);
                }
                if (rt.virtualMethodList.stream().map(Method::getMethodSpecification).anyMatch(signature::equals)) {
                    method.platforms.add(rt.module().platform);
                }
                if (rt.functionList.stream().map(Method::getMethodSpecification).anyMatch(signature::equals)) {
                    method.platforms.add(rt.module().platform);
                }
                if (rt.constructorList.stream().map(Method::getMethodSpecification).anyMatch(signature::equals)) {
                    method.platforms.add(rt.module().platform);
                }
            }
        }
    }

    /**
     * Override long values to int (for Windows compatibility)
     * @param methods the merged list of methods (Methods, VirtualMethods or Functions)
     * @param registeredTypes a list of three classes (linux, windows and mac)
     */
    private void overrideLongValues(List<? extends Method> methods, List<? extends GirElement> registeredTypes) {
        for (Method method : methods) {
            String signature = method.getMethodSpecification();
            for (var rt : registeredTypes) {
                if (rt == null) {
                    continue;
                }
                if (rt.methodList.stream().map(Method::getMethodSpecification).anyMatch(signature::equals)) {
                    if (rt.module().platform == Platform.WINDOWS) {
                        overrideLongValues(method); // Convert glong/gulong parameters to gint/guint
                    }
                }
                if (rt.virtualMethodList.stream().map(Method::getMethodSpecification).anyMatch(signature::equals)) {
                    if (rt.module().platform == Platform.WINDOWS) {
                        overrideLongValues(method); // Convert glong/gulong parameters to gint/guint
                    }
                }
                if (rt.functionList.stream().map(Method::getMethodSpecification).anyMatch(signature::equals)) {
                    if (rt.module().platform == Platform.WINDOWS) {
                        overrideLongValues(method); // Convert glong/gulong parameters to gint/guint
                    }
                }
                if (rt.constructorList.stream().map(Method::getMethodSpecification).anyMatch(signature::equals)) {
                    if (rt.module().platform == Platform.WINDOWS) {
                        overrideLongValues(method); // Convert glong/gulong parameters to gint/guint
                    }
                }
            }
        }
    }

    /**
     * Override "glong" and "gulong" property types to "int" values in Java, for Windows compatibility.
     * @param cls the (merged) class for which to override the property types
     */
    private void overrideLongValues(Class cls) {
        for (Property property : cls.propertyList) {
            if (property.type != null) {
                property.type.overrideLongType();
            }
        }
    }

    private void overrideLongValues(Alias alias) {
        if (alias.getTargetType() == Alias.TargetType.VALUE && alias.type != null) {
            alias.type.overrideLongType();
        }
    }

    /**
     * Override "glong" and "gulong" types to "int" values in Java, for Windows compatibility.
     * @param method the (merged) method for which to override the value types
     */
    private void overrideLongValues(Method method) {
        if (method.returnValue.type != null) {
            method.returnValue.type.overrideLongType();
        } else if (method.returnValue.array != null && method.returnValue.array.type != null) {
            method.returnValue.array.type.overrideLongType();
        }
        if (method.parameters != null) {
            for (Parameter parameter : method.parameters.parameterList) {
                if (parameter.type != null) {
                    parameter.type.overrideLongType();
                } else if (parameter.array != null && parameter.array.type != null) {
                    parameter.array.type.overrideLongType();
                }
            }
        }
    }
}
