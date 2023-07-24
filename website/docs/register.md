# Registering new types

When you extend a Java class from an existing GObject-derived class, Java will treat it as a subclass of GObject:

```java
public class MyObject extends GObject {
```

However, the GObject type system itself will not recognize it as its own class. Therefore, you need to register your class as a new "GType". You can do this manually by calling `GObjects.typeRegisterStaticSimple` and `GObjects.typeAddInterfaceStatic` (see the documentation [here](https://docs.gtk.org/gobject/func.type_register_static_simple.html) and [here](https://docs.gtk.org/gobject/func.type_add_interface_static.html)), but Java-GI offers an easy-to-use wrapper function: `Types.register(classname)`. This uses reflection to determine the name, parent class, implemented interfaces and overridden methods, and registers it as a new GType.

It is recommended to register the new gtype in a field `gtype` like this:

```java
    private static final Type gtype = Types.register(MyObject.class);
    
    public Type getType() {
        return gtype;
    }
```

By declaring the `gtype` as a static field in this way, it will be registered immediately when the JVM classloader initializes the Java class.

When instantiating a new instance of the object, pass the `gtype` field to `GObject.newInstance`. You can simplify this with a static factory method with a descriptive name like `create` or `newInstance`:

```java
    public static MyObject create() {
        return GObject.newInstance(gtype);
    }
```

Now, when you call `MyObject.create()`, you will have a Java object that is also instantiated as a native GObject instance.

The constructor **must** be a static factory method; a regular constructor that calls `super(gtype, null)` **will not work** correctly.

Finally, add the default memory-address-constructor for Java-GI Proxy objects:

```java
    public MyObject(MemorySegment address) {
        super(address);
    }
}
```

This constructor must exist in all Java-GI Proxy objects. It enables a Proxy class to be instantiated automatically for new instances returned from native function calls.

If your application is module-based, you must export your package to the `org.gnome.glib` module in your `module-info.java` file, to allow the reflection to work:

```
exports [package.name] to org.gnome.glib;
```

## Specifying the name of the GType

All GTypes have a unique name, like 'GtkLabel', 'GstObject' or 'GListModel'. (You can query the name of a GType using `GObjects.typeName`). When a Java class is registered as a GType, the package and class name are used to generate a unique GType name. You can override this with a specific name using the `@RegisteredType` attribute:

```java
@RegisteredType(name="MyExampleObject")
public class MyObject extends GObject {
    ...
```

If you don't intend to override the name of the GType, you can safely omit the `@RegisteredType` annotation.

## Method overrides

When you override virtual methods from parent GObject classes (or implemented interfaces), the override will automatically be registered by `Types.register(class)`. You don't need to do this manually.

### Chaining up

From inside the method body of an overridden method, you cannot call `super.method()` to "chain up" to a parent (native GObject) virtual method. Because virtual method invocations call the function pointer that is installed in the GObject typeclass, "chaining up" requires a lookup of the typeclass of the parent type first. Java-GI will do this when you call the `parent()` method that is available on all 
GObject classes. So instead of `super.method()`, call `parent().method` to "chain up". This is similar to [the `parent_class` pointer in native GObject code](https://developer-old.gnome.org/gobject/stable/howto-gobject-chainup.html).

For example:

```java
// Override the GObject.finalize_() virtual method
@Override
public void finalize_() {
    ... do cleanup work here ...
    
    // Chain up:
    parent().finalize_();
}
```

## Properties

You can define GObject properties with the `@Property` annotation on the getter and setter methods. You must annotate both the getter and setter methods (if applicable). The `@Property` annotation must always specify the `name` parameter; all other parameters are optional.

Example definition of an `int` property with name `n-items`:

```java
private int size;

@Property(name="n-items")
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
| type           | ParamSpec | inferred      |
| readable       | Boolean   | true          |
| writable       | Boolean   | true          |
| construct      | Boolean   | false         |
| constructOnly  | Boolean   | false         |
| explicitNotify | Boolean   | false         |
| deprecated     | Boolean   | false         |

When the type is not specified, it will be inferred from the parameter or return-type of the method. When the type is specified, it must be one of the subclasses of `GParamSpec`. The boolean parameters are `GParamFlags` arguments, and are documented [here](https://docs.gtk.org/gobject/flags.ParamFlags.html).

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
public void init() {
    ...
}
```

## Examples

In [this example application](https://github.com/jwharm/java-gi-examples/tree/main/PegSolitaire), the inner class `SolitairePeg` is registered as a GObject subclass that implements the `Paintable` interface.
