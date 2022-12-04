# java-gi

**java-gi** is a **very experimental, work-in-progress** tool for generating GObject-Introspection bindings for Java. The generated bindings use the "Panama" Foreign Function & Memory API (JEP 424) to directly access native resources from inside the JVM, and add wrapper classes based on GObject-Introspection to offer an elegant API. The included Gradle build scripts generate bindings for GTK4 and LibAdwaita.

Java-gi bindings are automatically generated from GI data (gir files).
Panama allows for relatively easy interoperability with native code. It is possible to generate Java bindings from header files using the `jextract` tool, but these binding classes are very difficult to use directly with GTK.
C functions like `gtk_button_set_icon_name(GtkButton* button, const char* icon_name)` are mapped to a static Java method `gtk_button_set_icon_name(MemoryAddress button, MemoryAddress icon_name)`.
Using GObject-Introspection, it is possible to generate a wrapper API that includes "proxy" classes that mirror native types and functions, with automatically marshalled parameters and return values: `button.setIconName(iconName)`.
Java-gi tries to achieve this.

## Quickstart

- First, download and install [JDK 19](https://jdk.java.net/19/).
- Download [gtk4-0.2.jar](https://github.com/jwharm/java-gi/releases/download/v0.2/gtk4-0.2.jar) and add it to the Java module path. The 0.2 release contains bindings for GTK version 4.8.2.
- Add `requires org.gtk;` to your `module-info.java` file.
- Write your GTK application:

```java
package io.github.jwharm.javagi.example;

import org.gtk.gtk.*;
import org.gtk.gio.ApplicationFlags;

public class HelloWorld {

    public void activate(org.gtk.gio.Application g_application) {
        var window = new ApplicationWindow(Application.castFrom(g_application));
        window.setTitle("Window");
        window.setDefaultSize(300, 200);
        
        var box = new Box(Orientation.VERTICAL, 0);
        box.setHalign(Align.CENTER);
        box.setValign(Align.CENTER);
        
        var button = Button.newWithLabel("Hello world!");
        button.onClicked((btn) -> window.close());
        
        box.append(button);
        window.setChild(box);
        window.show();
    }

    public HelloWorld(String[] args) {
        var app = new Application("org.gtk.example", ApplicationFlags.FLAGS_NONE);
        app.onActivate(this::activate);
        app.run(args.length, args);
    }

    public static void main(String[] args) {
        new HelloWorld(args);
    }
}
```

- Because the Panama foreign function API is still in preview status, add the `--enable-preview` command-line parameter when running your application. To suppress warnings about native access, also add `--enable-native-access=org.gtk`.
- It is recommended to download the [Javadoc documentation](https://github.com/jwharm/java-gi/releases/download/v0.2/gtk4-0.2-javadoc.jar) to assist during the development of your GTK application. Optionally, download the [sources](https://github.com/jwharm/java-gi/releases/download/v0.2/gtk4-0.2-sources.jar) too.

## Generating java-gi bindings
The instructions above link to pre-built bindings for the entire GTK4 library stack and LibAdwaita.

If you want to generate bindings for other versions, platforms or libraries, follow these steps:
- First, download and install [JDK 19](https://jdk.java.net/19/) and [Gradle](https://gradle.org/).
- Install the GObject-introspection (gir) files of the library you want to generate bindings for. 
  For example, in Fedora, to install the gir files for GTK4 and LibAdwaita, execute: `sudo dnf install gtk4-devel glib-devel libadwaita-devel gobject-introspection-devel`
- Running `gradle build` is enough to generate and build gtk4 bindings.

The resulting jar files are located in the `gtk4/build/libs` folder.

If you encounter errors about an unsupported `class file version`, make sure that Gradle is using a supported JDK (18 or older) and try again.

If you wish to create bindings for other libraries, add the library name, Java package name, and the name of the GIR file to `gtk4/gradle.build.kts`. In this file, you can also override method names or exclude certain types if neccessary.

## Features
Some interesting features of the bindings that java-gi generates:

### Javadoc
GtkDoc API docstrings are translated into Javadoc, so they are directly available in your IDE. 

As an example, the generated documentation of `gtk_button_get_icon_name` contains links to other methods, and specifies the return value. This is all translated to valid Javadoc:
```java
/**
 * Returns the icon name of the button.
 * <p>
 * If the icon name has not been set with {@link Button#setIconName}
 * the return value will be {@code null}. This will be the case if you create
 * an empty button with {@link Button#Button} to use as a container.
 * @return The icon name set via {@link Button#setIconName}
 */
public @Nullable java.lang.String getIconName() {
    ...
```

### Interfaces
Interfaces are mapped to Java interfaces, using `default` interface methods to call native methods.

### Named constructors
Constructors in GTK are often overloaded, and the name contains valuable information for the user. Therefore, java-gi maps constructors named "new" to regular Java constructors, and generates static factory methods for all other constructors:

```java
// gtk_button_new
var button1 = new Button();

// gtk_button_new_with_label
var button2 = Button.newWithLabel("Button2");

// gtk_button_new_from_icon_name
var button3 = Button.newFromIconName("icon.png");
```
### Automatic memory management (early stages)
Memory management of `GObject`s is automatically taken care of: When a ref-counted object (like `GObject` and its descendants) is fully "owned" by the user, and the object becomes unreachable in Java, a call to `g_object_unref` is automatically executed. 

This feature is still experimenal. Additional development is planned for the 0.3 release.

### Type-safe signals
Signals are mapped to type-safe methods and objects in Java. (Detailed signals like `notify` have an extra `String` parameter.)

For example, `GtkButton`s `clicked` signal is defined in the `Button` class as:
```java
@FunctionalInterface
public interface Clicked {
    void signalReceived(Button source);
}

public Signal<Button.Clicked> onClicked(Button.Clicked handler) {
    ...
}
```
A signal can be connected with a lambda expression:
```java
var button = Button.newWithLabel("Close");
button.onClicked((btn) -> window.destroy());
```
But you can also pass a method reference that takes a `Button` parameter and returns `void`.

New signal connections return a `Signal` object, that allows you to disconnect, block and unblock a signal, or check whether the signal is still connected.

It is not yet possible to define custom signals. The bindings contain a fair amount of generated boilerplate code for signals and callbacks; this works fine for predefined signals and callbacks, but it would be very difficult to implement this for a user-defined signal. In the future, I will look into the possibility to implement a custom `GClosureMarshaller` which should improve this.

### Type-safe callbacks
Most functions with callback parameters are supported. Java-gi needs a `user_data` parameter to be present, to store contextual information.

All callbacks are defined as functional interfaces (just like signals, as shown above) and are fully type-safe.

### User-defined derived types
As of version 0.2 of java-gi, it is possible to register your own Java classes as new GTypes. You can subclass a GObject-derived class in Java, and create the GType as follows:

```java
import io.github.jwharm.javagi.*;
import org.gtk.glib.Type;
import org.gtk.gobject.Value;

/**
 * An example GObject subclass to store a value of type 'double'
 */
public class TDouble extends org.gtk.gobject.Object implements Derived {

    private static Type type;

    public static Type getType() {
        if (type == null) {
            // Register the new gtype
            type = register(TDouble.class);
        }
        return type;
    }

    public TDouble() {
        // Call g_object_new_with_properties
        super(newWithProperties(getType(), 
                0, new String[0], new Value[0]).handle(),
            Ownership.FULL
        );
    }

    public void setValue(double value) {
        setValueObject(value);
    }

    public double getValue() {
        return (double) getValueObject();
    }
}
```
The purpose of the `getType()` method is comparable to the `..._get_type()` methods that register GTypes in C.

The generated memory layout for the typeinstance contains two fields: the `parent_instance` and `int value_object`. When a Java object is passed to `setValueObject()` as demonstrated in the example above, the object is stored in a cache, and the hashcode (an `int`) is stored in the `value_object` field. `getValueObject()` retrieves the hashcode and returns the object from the cache. This mechanism allows to store a reference to any Java object (including `this`) in the native typeinstance struct, without the need to define a complex memory layout.

### Null-checks
Nullability of parameters (as defined in the GObject-introspection attributes) is indicated with `@Nullable` and `@NotNull` attributes, and checked at runtime.

### Out-parameters
Out-parameters are mapped to a simple `Out<T>` container-type in Java, that offers typesafe `get()` and `set()` methods to retrieve or modify the value.

```java
File file = ...
Out<byte[]> contents = new Out<byte[]>();
Out<Long> length = new Out<Long>();
file.loadContents(null, contents, length, null));
System.out.printf("Read %d bytes", length.get());
```

### Arrays and pointers
Arrays with a known length (specified as gobject-introspection annotations) are mapped to Java arrays.

```java
public static void main(String[] args) {
    var app = new Application("io.github.jwharm.javagi.Example", ApplicationFlags.NON_UNIQUE);
    app.onActivate(app::activate);
    
    // Pass the String[] args array to Application.run
    app.run(args.length, args);
}
```
In some cases, the length of an array is impossible to determine from the gobject-introspection attributes, or a raw pointer is expected/returned. java-gi marshalls these values to a `Pointer` class (with typed subclasses) that can dereference the value of a pointer, or iterate through an unbounded sequence of memory locations. Needless to say that this is extremely unsafe.

### Exceptions
`GError**` parameters are mapped to Java `GErrorException`s.

```java
try {
    file.replaceContents(contents, contents.length, null, false, FileCreateFlags.NONE, null, null);
} catch (GErrorException e) {
    e.printStackTrace();
}
```

### Varargs
Variadic functions (varargs) are supported:
```java
Dialog d = Dialog.newWithButtons(
        "Test dialog",
        window,
        DialogFlags.MODAL,
        "Accept",
        ResponseType.ACCEPT,
        "Cancel",
        ResponseType.CANCEL,
        null
);
d.show();
```

### Type-safe casting
All generated classes contain a `castFrom()` method to cast between different GTypes. This method is typesafe: Illegal casts throw a `ClassCastException`.

An example of `castFrom()` can be seen in the HelloWorld example above. The `Application::activate` signal is defined in `GApplication`. When you create a `GtkApplication` and connect to its `activate` signal, the `GApplication` parameter is actually a `GtkApplication` (according to the GType), but the JVM doesn't know this, and doesn't allow a cast. A call to `castFrom()` resolves this.

### Builder pattern
You can construct object with properties using a Builder pattern:
```java
Button openButton = new Button.Build()
        .setLabel("Open...")
        .setIconName("document-open-symbolic")
        .setWidthRequest(100)
        .construct();
```

### Allocating records (structs)
Record types (`struct`s in native code) are mapped to Java classes. Because these types do not always offer a constructor method, the Java classes offer an `allocate()` method to allocate a new uninitialized record. The memory layouts have been generated from the field definitions in the gir files.

### GValues
You can create GValues with a series of overloaded `Value.create()` methods. These methods allocate a new GValue and initialize the data type.

```java
Value value1 = Value.create("string value");
Value value2 = Value.allocate();
value2.init(Type.G_TYPE_STRING);
value1.copy(value2);
System.out.printf("Value 1: %s, Value 2: %s\n", value1.getString(), value2.getString());
// Output: Value 1: string value, Value 2: string value
```
## Known issues

The bindings are still under active development and have not been thoroughly tested yet. The most notable issues and missing features are currently:
* Java does not support unsigned data types. You might encounter issues when native code returns, for example, a `guint`.
* You cannot create custom GObject interfaces yet.
* The generator has not been tested yet on different Linux distributions or GTK versions.
* While running the gradle build script, a large number of warnings occur during javadoc generation. These are safe to ignore.
* Return values of nested arrays (like Gio `g_desktop_app_info_search`) aren't supported yet.
