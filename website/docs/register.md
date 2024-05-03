# Registering new types

When you extend a Java class from an existing GObject-derived class, Java will treat it as a subclass of GObject:

```java
public class MyObject extends GObject {
```

However, the GObject type system itself will not recognize it as its own class. Therefore, you need to register your class as a new GType. To do this, Java-GI offers an easy-to-use wrapper function: `Types.register(classname)`. This will use reflection to determine the name, parent class, implemented interfaces and overridden methods, and will register it as a new GType.

It is recommended to register the new gtype in a static field `gtype` like this:

```java
    private static final Type gtype = Types.register(MyObject.class);
    
    public static Type getType() {
        return gtype;
    }
```

By declaring the `gtype` as a static field in this way, it will be registered immediately when the JVM classloader initializes the Java class.

When instantiating a new instance of the object, pass the `gtype` to `GObject::new()`:

```java
    public MyObject() {
        super(gtype, null);
    }
```

Alternatively, create a static factory method with a descriptive name like `create` or `newInstance` that calls `GObject::newInstance()`:

```java
    public static MyObject create() {
        return GObject.newInstance(gtype);
    }
```

Now, when you call `MyObject.create()`, you will have a Java object that is also instantiated as a native GObject instance.

If your class contains GObject class or instance initializer method (see below), the constructor **must** be a static factory method; a regular constructor that calls `super(gtype, null)` **will not work** correctly with GObject initializers.

Finally, add the default memory-address-constructor for Java-GI Proxy objects:

```java
    public MyObject(MemorySegment address) {
        super(address);
    }
}
```

This constructor should exist in all Java-GI proxy classes. It enables a Java class to be instantiated automatically for new instances returned from native function calls.

If your application is module-based, you must export your package to the `org.gnome.gobject` module in your `module-info.java` file, to allow the reflection to work:

```
module my.module.name {
    exports my.package.name to org.gnome.gobject;
}
```

## Specifying the name of the GType

A GType has a unique name, like 'GtkLabel', 'GstObject' or 'GListModel'. (You can query the name of a GType using `GObjects.typeName()`). When a Java class is registered as a GType, the package and class name are used to generate a unique GType name. You can override this with a specific name using the `@RegisteredType` attribute:

```java
@RegisteredType(name="MyExampleObject")
public class MyObject extends GObject {
    ...
```

If you don't intend to override the name of the GType, you can safely omit the `@RegisteredType` annotation.

## Method overrides

When you override virtual methods from parent GObject classes (or implemented interfaces), the override will automatically be registered by `Types.register(class)`. You don't need to do this manually.

### Chaining up

From inside the method body of an overridden method that is also available as a regular instance method, you cannot call `super.method()` to "chain up" to a parent (native GObject) virtual method, because Java-GI would invoke the instance method. The instance method would in many cases defer to the virtual function pointer of the derived class, resulting in an endless loop. To work around this problem, instead of `super`, call the `asParent()` method that is available on all GObject classes. So instead of `super.method()`, call `asParent().method()` to "chain up".

When a virtual method is not available as a regular instance method, you can safely use `super.method()` to "chain up". These virtual methods are easily recognizable, because they have `protected` visibility.

## Properties

You can define GObject properties with the `@Property` annotation on the getter and setter methods. You must annotate both the getter and setter methods (if applicable). The `@Property` annotation can optionally specify the `name` parameter; all other parameters are optional.

Example definition of an `int` property with name `n-items`:

```java
private int size;

@Property
public int getNItems() {
    return size;
}

@Property
public void setNItems(int nItems) {
    size = nItems;
}
```

The `@Property` annotation accepts the following parameters:

| Parameter      | Type      | Default value |
|----------------|-----------|---------------|
| name           | String    | inferred      |
| type           | ParamSpec | inferred      |
| readable       | Boolean   | true          |
| writable       | Boolean   | true          |
| construct      | Boolean   | false         |
| constructOnly  | Boolean   | false         |
| explicitNotify | Boolean   | false         |
| deprecated     | Boolean   | false         |

When the name is not specified, it will be inferred from the name of the method (provided that the method names follow the `getX()`/`setX(...)` pattern), stripping the "get" or "set" prefix and converting CamelCase to kebab-case. If you do specify a name, it must be present on **both** the getter and setter methods (otherwise Java-GI will create two properties, with different names).

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

## Signals

You can define custom signals in Java classes that extend GObject. For example:

```java
public class Counter extends GObject {

    // register the type
    private static final Type gtype = Types.register(Counter.class);
    
    // declare the signal
    @Signal
    public interface LimitReached {
        public void run(int limit);
    }

    public void count() {
        num++;
        if (num == limit) {
            // emit the signal
            emit("limit-reached", limit);
        }
    }
    
    ...
}
```

The "limit-reached" signal in the example is declared with a functional interface annotated as `@Signal`. The method signature of the functional interface is used to define the signal parameters and return value. The signal name is inferred from the interface too (converting CamelCase to kebab-case) but can be overridden.

You can connect to the custom signal, like this:

```java
counter.connect("limit-reached", (Counter.LimitReached) (limit) -> {
    System.out.println("Limit reached: " + limit);
});
```

Because the signal declaration is an ordinary functional interface, it is equally valid to extend from a standard functional interface like `Runnable`, `BooleanSupplier`, or any other one, like (in the above example) an `IntConsumer`:

```java
    @Signal
    public interface LimitReached extends IntConsumer {}
```

It is also possible to set a custom signal name and optional flags in the `@Signal` annotation, for example `@Signal(name="my-signal", detailed=true)` to define a detailed signal.

## Examples

In [this example application](https://github.com/jwharm/java-gi-examples/tree/main/PegSolitaire), the inner class `SolitairePeg` is registered as a GObject subclass that implements the `Paintable` interface.
