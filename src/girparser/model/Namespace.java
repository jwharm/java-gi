package girparser.model;

import girparser.generator.Conversions;

import java.util.HashMap;
import java.util.Map;

public class Namespace extends GirElement {

    public final String packageName, pathName;
    public final Map<String, RegisteredType> registeredTypeMap = new HashMap<>();

    public Namespace(GirElement parent, String name) {
        super(parent);
        this.name = name;
        packageName = Conversions.namespaceToJavaPackage(name);
        pathName = packageName.replace('.', '/') + '/';
    }
}
