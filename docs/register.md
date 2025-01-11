# Registering new types

When you extend a Java class from an existing GObject-derived class, Java will treat it as a subclass of GObject:

=== "Java"

    ```java
    public class MyObject extends GObject {
    ```

=== "Kotlin"

    ```kotlin
    class MyObject : GObject {
    ```


However, the GObject type system itself will not recognize it as its own class. To do that, you need to register your class as a new GType. Java-GI offers an easy-to-use function to achieve this: `Types.register(classname)`. This will use reflection to determine the name, parent class, implemented interfaces and overridden methods, and will register it as a new GType.

It is recommended to register the new gtype in a static block like this:

=== "Java"

    ```java
        {
            Types.register(MyObject.class);
        }
    ```

=== "Kotlin"

    ```kotlin
        companion object {
            init {
                Types.register<MyObject, ObjectClass>(MyObject::class.java)
            }
        }
    ```

By using a static initializer, the GObject type will be registered immediately when the JVM classloader initializes the Java class. Similarly, you can register all your classes in your `main()` method.

When instantiating a new instance of the object, pass the class to `GObject::new()`:

=== "Java"

    ```java
        public MyObject() {
            super(MyObject.class);
        }
    ```

=== "Kotlin"

    ```kotlin
        constructor() : super(MyObject::class.java)
    ```

Alternatively, create a static factory method with a descriptive name like `create` or `newInstance` that calls `GObject::newInstance()`:

=== "Java"

    ```java
        public static MyObject create() {
            return GObject.newInstance(MyObject.class);
        }
    ```

=== "Kotlin"

    ```kotlin
        companion object {
            val gtype: Type = ...
            
            fun create(): MyObject {
                return newInstance(MyObject::class.java)
            }
        }
    ```

Now, when you call `MyObject.create()`, you will have a Java object that is also instantiated as a native GObject instance.

If your class contains GObject class or instance initializer method (see below), the constructor **must** be a static factory method; a regular constructor that calls `super(MyObject.class)` **will not work** correctly with GObject initializers.

Finally, add the default memory-address-constructor for Java-GI Proxy objects:

=== "Java"

    ```java
        public MyObject(MemorySegment address) {
            super(address);
        }
    }
    ```

=== "Kotlin"

    ```kotlin
        constructor(address: MemorySegment?) : super(address)
    }
    ```

Ignore warnings that the constructor appears unused: This constructor should exist in all Java-GI proxy classes. It enables a Java object to be instantiated automatically for GObject instances returned from native function calls.

If your Java application is module-based, you must export your package to the `org.gnome.gobject` module in your `module-info.java` file, to allow the reflection to work:

```
module my.module.name {
    exports my.package.name to org.gnome.gobject;
}
```

## Specifying the name of the GType

A GType has a unique name, like 'GtkLabel', 'GstObject' or 'GListModel'. (You can query the name of a GType using `GObjects.typeName()`). When a Java class is registered as a GType, the package and class name are used to generate a unique GType name. You can override this with a specific name using the `@RegisteredType` attribute:

=== "Java"

    ```java
    @RegisteredType(name="MyExampleObject")
    public class MyObject extends GObject {
        ...
    ```

=== "Kotlin"

    ```kotlin
    @RegisteredType(name="MyExampleObject")
    class MyObject : GObject {
        ...
    ```

To prefix all type names in a package with a shared namespace identifier, use the `@Namespace(name="...")` annotation in your `package-info.java` file.

If you don't intend to override the name of the GType, you can safely omit the `@RegisteredType` and `@Namespace` annotations.

## Method overrides

When you override virtual methods from parent GObject classes (or implemented interfaces), the override will automatically be registered by `Types.register(class)`. You don't need to do this manually.

### Chaining up

From inside the method body of an overridden method that is also available as a regular instance method, you cannot call `super.method()` to "chain up" to a parent (native GObject) virtual method, because Java-GI would invoke the instance method. The instance method would in many cases defer to the virtual function pointer of the derived class, resulting in an endless loop. To work around this problem, instead of `super`, call the `asParent()` method that is available on all GObject classes. So instead of `super.method()`, call `asParent().method()` to "chain up".

When a virtual method is not available as a regular instance method, you can safely use `super.method()` to "chain up". These virtual methods are easily recognizable, because they have `protected` visibility.

## Properties

When your class contains getter and setter method pairs, Java-GI will register them as GObject properties. For boolean properties, you can also use `isFoo()`/`setFoo()` pairs. Kotlin creates get- and set-methods for Kotlin properties automatically, so in practice a Kotlin property will be registered as a GObject property.

You can define GObject properties with the `@Property` annotation on other methods besides getter/setter pairs, or when you want to change the property name or change other parameters. When you use `@Property`, you must annotate both the getter and setter methods (if applicable).

Example definition of an `int` property with name `n-items`:

=== "Java"

    ```java
    private int size = 0;

    public int getNItems() {
        return size;
    }

    public void setNItems(int nItems) {
        size = nItems;
    }
    ```

=== "Kotlin"

    ```kotlin
    var nItems: Int = 0
    ```

### Property annotation parameters

The `@Property` annotation can be used to set a property name and other attributes. It can also be used to mark methods as properties that don't conform to the getter/setter convention. Finally, a `@Property(skip=true)` annotation can be used to prevent getter/setter methods getting registered as a GObject property.

For properties with a getter and setter method, either both or neither methods must be annotated with `@Property` (or else they will not be recognized by Java-GI as a pair). The annotation parameters must be specified on the getter. They can be specified on the setter too, but only the parameters on the getter are actually used.

The `@Property` annotation accepts the following parameters:

| Parameter      | Type      | Default value   |
|----------------|-----------|-----------------|
| name           | String    | inferred        |
| type           | ParamSpec | inferred        |
| readable       | Boolean   | true            |
| writable       | Boolean   | true            |
| construct      | Boolean   | false           |
| constructOnly  | Boolean   | false           |
| explicitNotify | Boolean   | false           |
| deprecated     | Boolean   | false           |
| minimumValue   | String    | type-dependent  |
| maximumValue   | String    | type-dependent  |
| defaultValue   | String    | type-dependent  |
| skip           | Boolean   | false           |

All `@Property` annotation parameters are optional.

When the name is not specified, it will be inferred from the name of the method (provided that the method names follow the `getX()`/`setX(...)` pattern), stripping the "get" or "set" prefix and converting CamelCase to kebab-case. If you do specify a name, it must be present on **both** the getter and setter methods (otherwise Java-GI will create two properties, with different names).

When the type is not specified, it will be inferred from the parameter or return-type of the method. When the type is specified, it must be one of the subclasses of `GParamSpec`. The boolean parameters are `GParamFlags` arguments, and are documented [here](https://docs.gtk.org/gobject/flags.ParamFlags.html).

The `@Property` parameters `minimumValue`, `maximumValue` and `defaultValue` expect String values. They are transformed by Java-GI to the proper type using `Boolean.parseBoolean()`, `Integer.parseInt()` etcetera.  Using these three parameters, you can set the minimum, maximum and default values on the `GParamSpec` of a property that was defined in Java.

* The minimum and maximum values are not enforced by Java-GI, so on the Java side the benefits are negligible. In most cases it is advisable to implement minimum and maximum property values in Java itself.
* The default value is returned by Java-GI for properties that have only a setter method in Java.

When the `skip` parameter is set, the method will *not* be registered as a GObject property.

## Class and instance init functions

To implement a custom class initializer or instance initializer function, use the `@ClassInit` and `@InstanceInit` attributes:

=== "Java"

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

=== "Kotlin"

    ```kotlin
    companion object {
        ...
        
        // (Optional) class initialization function    
        @ClassInit
        fun classInit(typeClass: ObjectClass) {
            ...
        }
    }

    // (Optional) instance initialization function    
    @InstanceInit
    fun init() {
        ...
    }
    ```

Be aware that the instance initializer will only run for objects constructed with a static factory method. A regular constructor that calls `super(MyObject.class)` will not work.

## Signals

You can define custom signals in Java classes that extend GObject. For example:

=== "Java"

    ```java
    public class Counter extends GObject {

        // register the type
        private static final Type gtype = Types.register(Counter.class);
        
        // declare the signal
        @Signal
        public interface LimitReached {
            void run(int limit);
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

=== "Kotlin"

    ```kotlin
    class Counter : GObject {

        // register the type
        companion object {
            val gtype: Type = Types.register<Counter, ObjectClass>(Counter::class.java)
        }
        
        // declare the signal
        @Signal
        interface LimitReached {
            fun run(limit: Int)
        }
        
        fun count() {
            num++
            if (num == limit) {
                // emit the signal
                emit("limit-reached", limit)
            }
        }
        
        ...
    }
    ```

The "limit-reached" signal in the example is declared with a functional interface annotated as `@Signal`. The method signature of the functional interface is used to define the signal parameters and return value. The signal name is inferred from the interface too (converting CamelCase to kebab-case) but can be overridden.

You can connect to the custom signal, like this:

=== "Java"

    ```java
    counter.connect("limit-reached", (Counter.LimitReached) (limit) -> {
        System.out.println("Limit reached: " + limit);
    });
    ```

=== "Kotlin"

    ```kotlin
    counter.connect("limit-reached") { limit: Int ->
        println("Limit reached: $limit")
    }
    ```

Because the signal declaration is an ordinary functional interface, it is equally valid to extend from a standard functional interface like `Runnable`, `BooleanSupplier`, or any other one, like (in the above example) an `IntConsumer`:

=== "Java"

    ```java
        @Signal
        public interface LimitReached extends IntConsumer {}
    ```

=== "Kotlin"

    ```kotlin
        @Signal
        interface LimitReached : IntConsumer
    ```

It is also possible to set a custom signal name and optional flags in the `@Signal` annotation, for example `@Signal(name="my-signal", detailed=true)` to define a detailed signal.

## Examples

In [this example application](https://github.com/jwharm/java-gi-examples/tree/main/PegSolitaire), the inner class `SolitairePeg` is registered as a GObject subclass that implements the `Paintable` interface.
