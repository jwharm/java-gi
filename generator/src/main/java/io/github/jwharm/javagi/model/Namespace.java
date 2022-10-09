package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

import java.util.HashMap;
import java.util.Map;

public class Namespace extends GirElement {

    public final String packageName, pathName;
    public final Map<String, RegisteredType> registeredTypeMap = new HashMap<>();

    public Namespace(GirElement parent, String name, String pkg) {
        super(parent);
        this.name = name;
        packageName = pkg;
        Conversions.nsLookupTable.put(name.toLowerCase(), pkg);
        pathName = packageName.replace('.', '/') + '/';
    }
}
