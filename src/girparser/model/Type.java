package girparser.model;

import girparser.generator.Conversions;

public class Type extends GirElement {

    /** Pointer to the GirElement object */
    public RegisteredType girElementInstance;

    /** GIR element name: Class, Alias, Enumeration, Doc, Method, ... */
    public String girElementType;

    /** Example: gboolean, const char*, GdkRectangle* */
    public String cType;

    /** This type is used on the Java side. Example: boolean, java.lang.String, Rectangle */
    public String simpleJavaType;
    /** This type is used on the Java side. Example: boolean, java.lang.String, org.gdk.gtk.Rectangle */
    public String qualifiedJavaType;

    /** Used when this type refers to another namespace. Excluding the trailing dot. Example: org.gdk */
    public String namespacePath;
    public String girNamespace;

    /** Only true if this type is represented by a primitive type on the Java side */
    public boolean isPrimitive;

    public Type(GirElement parent, String name, String cType) {
        super(parent);
        if (name != null) {
            if (name.contains(".")) {
                this.girNamespace = name.substring(0, name.lastIndexOf('.'));
                this.name = name.substring(name.lastIndexOf('.') + 1);
            } else {
                this.girNamespace = getNamespace().name;
                this.name = name;
            }
        }
        this.cType = cType;
        this.simpleJavaType = Conversions.convertToJavaType(name, false);
        this.qualifiedJavaType = Conversions.convertToJavaType(name, true);
        this.namespacePath = Conversions.getJavaPackageName(name);

        this.isPrimitive = Conversions.isPrimitive(simpleJavaType);
    }

    public boolean isAlias() {
        return "Alias".equals(girElementType);
    }

    public boolean isBitfield() {
        return "Bitfield".equals(girElementType);
    }

    public boolean isCallback() {
        return "Callback".equals(girElementType);
    }

    public boolean isClass() {
        return "Class".equals(girElementType)
                || "Record".equals(girElementType)
                || qualifiedJavaType.startsWith("org.gtk.gobject.");
    }

    public boolean isEnum() {
        return "Enumeration".equals(girElementType);
    }

    public boolean isInterface() {
        return "Interface".equals(girElementType);
    }

    public boolean isRecord() {
        return "Record".equals(girElementType);
    }

    public boolean isUnion() {
        return "Union".equals(girElementType);
    }

    public boolean isVoid() {
        return name.equals("none");
    }
}
