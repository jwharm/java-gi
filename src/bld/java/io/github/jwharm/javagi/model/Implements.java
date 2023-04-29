package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

public class Implements extends Type {

    public Implements(GirElement parent, String name) {
        super(parent, name, null);
    }

    /**
     * Get the fully qualified name of the Java class that is implemented
     * @return the name of the implemented Java class
     */
    public String getQualifiedJavaName() {
        return Conversions.toQualifiedJavaType(qualifiedName, getNamespace());
    }
}
