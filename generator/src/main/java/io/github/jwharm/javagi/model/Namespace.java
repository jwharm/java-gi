package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

import java.util.HashMap;
import java.util.Map;

public class Namespace extends GirElement {

    public final String version;
    public final String sharedLibrary;
    public final String cIdentifierPrefix;
    public final String cSymbolPrefix;
    public final String packageName;
    public final String globalClassPackage;
    public final String globalClassName;
    public final String pathName;
    public final Map<String, RegisteredType> registeredTypeMap = new HashMap<>();

    public Namespace(GirElement parent, String name, String version, String sharedLibrary,
                     String cIdentifierPrefix, String cSymbolPrefix, String pkg) {
        super(parent);
        this.name = name;
        this.version = version;
        this.sharedLibrary = sharedLibrary;
        this.cIdentifierPrefix = cIdentifierPrefix;
        this.cSymbolPrefix = cSymbolPrefix;
        this.packageName = pkg;
        this.globalClassPackage = pkg;
        this.globalClassName = (name.equals("GObject") ? "GObjects" : name);
        Conversions.nsLookupTable.put(name.toLowerCase(), pkg);
        this.pathName = packageName.replace('.', '/') + '/';
    }
}
