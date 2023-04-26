# Registering new types

When you extend a Java class from an existing GObject-derived class, Java will treat it as a subclass of GObject. However, the GObject type system itself will not recognize it as its own class. Therefore, you need to register your class as a new "GType". You can do this manually by calling `GObjects.typeRegisterStaticSimple` and `GObjects.typeAddInterfaceStatic` (see the documentation [here](https://docs.gtk.org/gobject/func.type_register_static_simple.html) and [here](https://docs.gtk.org/gobject/func.type_add_interface_static.html)), but Java-GI offers an easy-to-use wrapper function: `Types.register(classname)`. This uses reflection to determine the name, parent class, implemented interfaces and overridden methods, and registers it as a new GType. To allow the reflection to work, you must export your package to the `org.gnome.glib` module in your `module-info.java` file:

```
exports [package.name] to org.gnome.glib;
```

It is recommended to register the new gtype in a method `public Type getType()` like this:

```java
public class MyObject extends GObject {

    private static Type type;
    
    public Type getType() {
        if (type == null)
            type = Types.register(MyObject.class);
        return type;
    }
```

When instantiating a new instance of the object with `GObject.newInstance`, you call `getType()` to register the new gtype and pass it to the GObject constructor:

```java
    public MyObject newInstance() {
        return GObject.newInstance(getType());
    }
```

Finally, add the default memory-address-constructor for Java-GI Proxy objects:

```java
    public MyObject(Addressable address) {
        super(address);
    }
}
```

This constructor exists in all Java-GI Proxy objects. It enables a Proxy class to be instantiated automatically for new instances returned from native function calls.

## Specifying the name of the GType

All GTypes have a unique name, like 'GtkLabel', 'GstObject' or 'GListModel'. You can query the name of a GType using `GObjects.typeName`. When a Java class is registered as a GType, the package and class name are used to generate a unique GType name. You can override this with a specific name using the `@RegisteredType` attribute:

```java
@RegisteredType(name="MyExampleObject")
public class MyObject extends GObject {
    ...
```

## Method overrides

When you override virtual methods from parent GObject classes (or implemented interfaces), the override will automatically be registered. You don't need to do this manually.

## Properties

You can define GObject properties with the `@Property` annotation on the getter and setter methods. Annotate both the getter and setter methods. The `@Property` annotation must always specify the `name` parameter; all other parameters need only be specified once (either on the getter or setter method).

Example definition of an `int` property with name `size`:

```java
    private int size;

    @Property(name="n-items", type=ParamSpecInt.class)
    public int getNItems() {
        return size;
    }

    @Property(name="n-items")
    public void setNItems(int nItems) {
        size = nItems;
    }
```

The `@Property` annotation accepts the following parameters:

| Parameter      | Type      | Default value |
|----------------|-----------|---------------|
| name           | Mandatory | n/a           |
| type           | ParamSpec | n/a           |
| readable       | Boolean   | true          |
| writable       | Boolean   | true          |
| construct      | Boolean   | false         |
| constructOnly  | Boolean   | false         |
| explicitNotify | Boolean   | false         |
| deprecated     | Boolean   | false         |

The type must be one of the subclasses of `GParamSpec`.  The boolean parameters are `GParamFlags` arguments, and are documented [here](https://docs.gtk.org/gobject/flags.ParamFlags.html).

## Class and instance init functions

To implement a custom class initializer or instance initializer function, use the `@ClassInit` and `@InstanceInit` attributes:

```java
    // (Optional) class initialization function    
    @ClassInit
    public static void classInit(GObject.ObjectClass typeClass) {
        ...
    }

    // (Optional) instance initialization function    
    @InstanceInit
    public static void init(MyObject instance) {
        ...
    }
```
