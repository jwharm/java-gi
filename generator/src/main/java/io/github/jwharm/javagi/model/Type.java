package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

public class Type extends GirElement {

    /** Pointer to the GirElement object */
    public RegisteredType girElementInstance;

    /** GIR element name: Class, Alias, Enumeration, Doc, Method, ... */
    public String girElementType;

    /** Example: gboolean, const char*, GdkRectangle* */
    public String cType;

    /** This is the type name from the gir file. For example: Gdk.Rectangle */
    public String qualifiedName;
    /** This type is used on the Java side. Example: boolean, java.lang.String, Rectangle */
    public String simpleJavaType;
    /** This type is used on the Java side. Example: boolean, java.lang.String, org.gdk.gtk.Rectangle */
    public String qualifiedJavaType;
    /** The name of the memory-address constructor. Example: Button.new, FileImpl.new */
    public String constructorName;

    /** Used when this type refers to another namespace. Excluding the trailing dot. Example: org.gnome.gdk */
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
        this.simpleJavaType = Conversions.convertToJavaType(name, false, getNamespace());
        this.qualifiedJavaType = Conversions.convertToJavaType(name, true, getNamespace());
        this.isPrimitive = Conversions.isPrimitive(simpleJavaType);
        
        if (girElementInstance != null && girElementInstance instanceof Record rec && rec.isGTypeStructFor != null) {
            qualifiedJavaType = Conversions.convertToJavaType(rec.isGTypeStructFor, true, girElementInstance.getNamespace());
            qualifiedJavaType += "." + simpleJavaType;
        }

        // Get constructor name from class, interface, and alias for class or interface
        this.constructorName = qualifiedJavaType + "::new";
        if (girElementInstance != null) {
            if (girElementInstance instanceof Record) {
                // no action needed
            } else if (girElementInstance instanceof Class cls) {
                this.constructorName = cls.getConstructorString();
            } else if (girElementInstance instanceof Interface iface) {
                this.constructorName = iface.getConstructorString();
            } else if (girElementInstance instanceof Alias alias) {
                if (alias.getTargetType() == Alias.TargetType.RECORD) {
                    // no action needed
                } else if (alias.getTargetType() == Alias.TargetType.CLASS) {
                    Class cls = (Class) alias.type.girElementInstance;
                    this.constructorName = cls.getConstructorString();
                } else if (alias.getTargetType() == Alias.TargetType.INTERFACE) {
                    Interface iface = (Interface) alias.type.girElementInstance;
                    this.constructorName = iface.getConstructorString();
                }
            }
        }
    }

    public boolean isAlias() {
        return "Alias".equals(girElementType);
    }

    public boolean isAliasForPrimitive() {
        return isAlias() && ((Alias) girElementInstance).getTargetType() == Alias.TargetType.VALUE;
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
        return "Class".equals(girElementType) || "Record".equals(girElementType);
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
        return cType != null && (cType.endsWith("*") || cType.endsWith("gpointer"));
    }

    /**
     * All classes and interfaces (and aliases for classes and interfaces) have a
     * gtype, but GTypeInstance has too, and that is a record.
     * @return whether it's possible to read a gtype for this type
     */
    public boolean hasGType() {
        return (! isRecord() || isUnion() || isAliasForPrimitive()) &&
                ("GTypeInstance".equals(cType) ||
                (isAlias() && (
                        ((Alias) girElementInstance).getTargetType() == Alias.TargetType.CLASS ||
                        ((Alias) girElementInstance).getTargetType() == Alias.TargetType.INTERFACE)));
    }
    
    public boolean isTypeClass() {
        return isRecord() && "TypeClass".equals(name);
    }
}
