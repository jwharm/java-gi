package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

public class Type extends GirElement {

    /** Pointer to the GirElement object */
    public RegisteredType girElementInstance;

    /** GIR element name: Class, Alias, Enumeration, Doc, Method, ... */
    public String girElementType;

    /** Example: gboolean, const char*, GdkRectangle* */
    public final String cType;

    /** This is the type name from the gir file. For example: Gdk.Rectangle */
    public String qualifiedName;
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
        this.cType = cType;
        init(name);
    }

    public void init(String name) {
        if (name != null) {
            if (name.contains(".")) {
                this.girNamespace = name.substring(0, name.lastIndexOf('.'));
                this.name = name.substring(name.lastIndexOf('.') + 1);
            } else {
                this.girNamespace = getNamespace().name;
                this.name = name;
            }
            if (name.equals("GType")) {
                this.girNamespace = "GLib";
                this.name = "Type";
            }
        } else {
        	// If the type does not have a name, it's possibly undefined in GI, so use gpointer as a generic fallback.
        	name = "gpointer";
        }
        this.qualifiedName = name;
        this.simpleJavaType = Conversions.convertToJavaType(name, false, getNamespace().packageName);
        this.qualifiedJavaType = Conversions.convertToJavaType(name, true, getNamespace().packageName);
        this.namespacePath = Conversions.getJavaPackageName(name);
        this.isPrimitive = Conversions.isPrimitive(simpleJavaType);
    }

    public boolean isAlias() {
        return "Alias".equals(girElementType);
    }

    public boolean isAliasForPrimitive() {
        return isAlias() && ((Alias) girElementInstance).aliasFor() == Alias.VALUE_ALIAS;
    }

    public boolean isBitfield() {
        return "Bitfield".equals(girElementType);
    }
    
    public boolean isBoolean() {
        return isPrimitive && "gboolean".equals(name) && (! "_Bool".equals(cType));
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
        return "none".equals(name);
    }
    
    public boolean isPointer() {
        return cType != null && cType.endsWith("*");
    }
}
