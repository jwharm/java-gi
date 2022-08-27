package girparser.model;

import girparser.generator.Conversions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Namespace extends GirElement {

    public String packageName, pathName;
    public Map<String, RegisteredType> registeredTypeMap = new HashMap<>();

    public Namespace(GirElement parent, String name) {
        super(parent);
        this.name = name;
        packageName = Conversions.namespaceToJavaPackage(name);
        pathName = packageName.replace('.', '/') + '/';
    }
}
