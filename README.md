# java-gi

**java-gi** is a tool for generating GObject-Introspection bindings for Java. The generated bindings use the [Panama](https://openjdk.org/projects/panama/) Foreign Function & Memory API (JEP 424) to directly access native resources from inside the JVM, and add wrapper classes based on GObject-Introspection to offer an elegant API. The included Gradle build scripts generate bindings for:
- GLib
- GTK4
- LibAdwaita
- GStreamer

## Quickstart
- First, download and install [JDK 19](https://jdk.java.net/19/).
- To create a Gtk application, add the `GLib` and `Gtk4` modules to your `pom.xml`:
```xml
<dependency>
  <groupId>io.github.jwharm.javagi</groupId>
  <artifactId>glib</artifactId>
  <version>0.3-SNAPSHOT</version>
</dependency>

<dependency>
  <groupId>io.github.jwharm.javagi</groupId>
  <artifactId>gtk4</artifactId>
  <version>0.3-SNAPSHOT</version>
</dependency>
```
or download the jar files manually from the packages section.
- Add `requires org.gtk;` to your `module-info.java` file.
- Write, compile and run your GTK application:
```java
package io.github.jwharm.javagi.example;

import org.gtk.gtk.*;
import org.gtk.gio.ApplicationFlags;

public class HelloWorld extends Application {

    public static void main(String[] args) {
        var app = new HelloWorld("org.gtk.example", ApplicationFlags.FLAGS_NONE);
        app.onActivate(app::activate);
        app.run(args);
    }
    
    public void activate() {
        var window = new ApplicationWindow(this);
        window.setTitle("GTK from Java");
        window.setDefaultSize(300, 200);
        
        var box = Box.builder()
            .setOrientation(Orientation.VERTICAL)
            .setHalign(Align.CENTER)
            .setValign(Align.CENTER)
            .build();
        
        var button = Button.newWithLabel("Hello world!");
        button.onClicked(window::close);
        
        box.append(button);
        window.setChild(box);
        window.show();
    }
}
```

- Because the Panama foreign function API is still in preview status, add the `--enable-preview` command-line parameter when running your application. To suppress warnings about native access, also add `--enable-native-access=org.glib,org.gtk`.
- It is recommended to download the Javadoc documentation to assist during the development of your GTK application. Optionally, download the sources too. Both are available in the [Releases](https://github.com/jwharm/java-gi/releases) section.

## Background
Project Panama allows for relatively easy interoperability with native code. To mechanically generate Java bindings from native library headers, the [jextract](https://github.com/openjdk/jextract) tool was developed by the Panama developers. It is possible to generate Java bindings from header files using jextract, but the resulting API is very difficult to use. All C functions, like `gtk_button_set_icon_name(GtkButton* button, const char* icon_name)`, are mapped by jextract to static Java methods:

```java
gtk_button_set_icon_name(MemoryAddress button, MemoryAddress icon_name)
```

Using GObject-Introspection instead, it is possible to generate a wrapper API with "proxy" classes that mirror native types and functions, with automatically marshalled parameters and return values:

```java
button.setIconName(iconName)
```
java-gi generates this API. The Panama Foreign Function & Memory API is still used for calling native functions and allocating memory, but the generated API is completely based on GI data (gir files).

## Generating java-gi bindings
The instructions above link to pre-built bindings for the entire GTK4 library stack and LibAdwaita.

If you want to generate bindings for other versions, platforms or libraries, follow these steps:
- First, download and install [JDK 19](https://jdk.java.net/19/) and [Gradle](https://gradle.org/) version 7.6 or newer.
- Install the GObject-introspection (gir) files of the library you want to generate bindings for. 
  For example, in Fedora, to install the gir files for GTK4 and LibAdwaita, execute: `sudo dnf install gtk4-devel glib-devel libadwaita-devel gobject-introspection-devel`
- Running `gradle build` is enough to generate and build gtk4 bindings.

The resulting jar files are located in the `gtk4/build/libs` folder.

If you encounter errors about an unsupported `class file version`, make sure that Gradle is at least version 7.6.

## Features
Some interesting features of the bindings that java-gi generates:

### Coverage
Almost all types, functions and parameters defined in the GIR files for GTK and GStreamer are supported by java-gi. Even complex function signatures with combinations of arrays, callbacks, out-parameters and varargs are handled correctly.

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

The Javadoc is also [published online](https://jwharm.github.io/java-gi/gtk4/org.gtk/module-summary.html).

### Interfaces
Interfaces are mapped to Java interfaces, using `default` interface methods to call native methods.

### Type aliases
Type aliases (`typedef`s in C) for classes, records and interfaces are represented in Java with a subclass of the original type. Aliases for primitive types such as `int` or `long` are represented by a simple object with `getValue` and `setValue` methods.

### Enumerations
Enumeration types are represented as Java `enum` types.

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

### Automatic memory management
Memory management of `GObject`s is automatically taken care of: When a ref-counted object (like `GObject` and its descendants) is fully "owned" by the user, and the object becomes unreachable in Java, a call to `unref` for that instance (for example, `g_object_unref`) is automatically executed. Floating references (`GInitiallyUnowned` for example) are automatically `ref_sink`ed after construction.

Memory that is allocated when calling native functions (for string and array parameters) or when allocating a struct type, is released after use; either using `try-with-resources` statements or by a `Cleaner` during garbage collection.

### Type-safe signals
Signals are mapped to type-safe methods and objects in Java. (Detailed signals like `notify` have an extra `String` parameter.) A signal can be connected to a lambda expression or method reference:
```java
var button = Button.newWithLabel("Close");
button.onClicked(window::destroy);
```
For every signal, a method to connect (e.g. `onClicked`) and emit the signal (`emitClicked`) is included in the API. New signal connections return a `Signal` object, that allows you to disconnect, block and unblock a signal, or check whether the signal is still connected.

### Type-safe callbacks
Functions with callback parameters are supported. In previous versions, java-gi required a `user_data` argument to be present, but since version 0.3, the `user_data` parameter is not used anymore (and hidden from the Java API).

All callbacks are defined as functional interfaces (just like signals, as shown above) and are fully type-safe.

### User-defined derived types
As of version 0.2 of java-gi, it is possible to register your own GTypes. You can subclass a GObject-derived class with new GType using `GObjects.typeRegisterStaticSimple`, define your own memory layout, add properties, and implement virtual methods.

### Null-checks
Nullability of parameters (as defined in the GObject-introspection attributes) is indicated with `@Nullable` and `@NotNull` attributes, and checked at runtime.

### Out-parameters
Out-parameters are mapped to a simple `Out<T>` container-type in Java, that offers typesafe `get()` and `set()` methods to retrieve or modify the value.

```java
File file = ...
Out<byte[]> contents = new Out<byte[]>();
file.loadContents(null, contents, null));
System.out.printf("Read %d bytes", contents.get().length);
```

### Arrays and pointers
Arrays with a known length (specified as gobject-introspection annotations) are mapped to Java arrays. java-gi generates code to automatically copy native array contents from and to a Java array, marshalling the contents to the correct types along the way. A `null` terminator is added where applicable. You also don't need to specify the array length as a separate parameter (even though many C functions need this) because java-gi will insert it for you automatically.
As an example:
```C
int g_application_run (GApplication* application, int argc, char** argv)
```
is in Java:
```Java
public int run(@Nullable java.lang.String[] argv)
```
The `String[]` argument is automatically marshalled to a `char**` array, and `argc` is omitted from the Java API. The allocated `char**` memory is immediately released afterwards (since java-gi version 0.3, memory is allocated and released with `try-with-resources` blocks).

In some cases, the length of an array is impossible to determine from the GObject-Introspection attributes, or a raw pointer is expected/returned. java-gi marshalls these values to a `Pointer` class (with typed subclasses) that can dereference the value of a pointer, or iterate through an unbounded sequence of memory locations. Needless to say that this is extremely unsafe.

### Exceptions
`GError**` parameters are mapped to Java `GErrorException`s.

```java
try {
    file.replaceContents(contents, null, false, FileCreateFlags.NONE, null, null);
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
Java proxy instances are instantiated with the proxy class for the `gtype` of the native object. As a result, a cast like `Button myButton = (Button) GtkBuilder.getObject("my_button");` is completely safe, and does not throw a `ClassCastException`, even though the return type of `getObject` is `GObject`.

This removes the need for the `castFrom()` workaround that was required in previous java-gi versions.

### Builder pattern
You can construct an object with properties using a Builder pattern. In the "Hello World" app above, it's used to create a `Box`. It can be used for any other type too:
```java
var window = ApplicationWindow.builder()
    .setApplication(this)
    .setTitle("Window")
    .setDefaultWidth(300)
    .setDefaultHeight(200)
    .build();
```
Since java-gi version 0.3, you can set the properties of the class, its parents, and all implemented interfaces. We also avoid type erasure (using the "curiously recurring template pattern") that used to occur in version 0.2 when setting properties of parent classes.

### Allocating records (structs)
Record types (`struct`s in native code) are mapped to Java classes. Because structs don't necessarily have a constructor method, the Java classes offer an `allocate()` method to allocate a new uninitialized record. To determine the size of the struct and its members, the memory layouts have been generated from the field definitions in the gir files. The allocated memory is automatically released when the Java object is garbage-collected.

### GValues
You can create GValues with a series of overloaded `Value.create()` methods. These methods allocate a new GValue and initialize the data type.
In the following example, `value1` is created with this method, while `value2` is manually allocated and initialized:
```java
Value value1 = Value.create("string value");
Value value2 = Value.allocate();
value2.init(Type.G_TYPE_STRING);
value1.copy(value2);
System.out.printf("Value 1: %s, Value 2: %s\n", value1.getString(), value2.getString());
// Output: Value 1: string value, Value 2: string value
```

### Naming conventions
All Java types and methods are named according to Java naming conventions. Namespaces are converted into package names (reverse-domain-name style), for example `org.cairographics` for the `Cairo` namespace.

Namespace-global functions and constants are exposed in one class, for example the class `org.gtk.glib.GLib` contains static members for all global declarations in the GLib namespace.

A small number of types have been renamed in the Java API to avoid confusion:
- [`GObject.Object`](https://docs.gtk.org/gobject/class.Object.html) is named `GObject` (instead of `Object`) to avoid confusion with `java.lang.Object`
- As a result, the namespace-global functions and constants in the `GObject` namespace can be found in `org.gtk.gobject.GObjects` (notice the trailing 's').
- [`GLib.String`](https://docs.gtk.org/glib/struct.String.html) is named `GString` (instead of `String`) to avoid confusion with `java.lang.String`
- [`Gtk.Builder`](https://docs.gtk.org/gtk4/class.Builder.html) is named `GtkBuilder` (instead of `Builder`) to avoid confusion with the `Builder` that java-gi generates for every class.

### Source code
The Java API is generated as source code and then compiled into JAR files. The generated code is properly formatted and indented, and snapshots are [available online](https://github.com/jwharm/java-gi-generated-sources) for reference.

## Known issues
The bindings are still under active development and have not been thoroughly tested yet. The most notable issues and missing features are currently:
- Java does not support unsigned data types. Be extra careful when native code returns, for example, a `guint`.
- A [closure marshaller](https://docs.gtk.org/gobject/struct.Closure.html) has not yet been implemented as part of java-gi. As a result, you cannot create custom signals yet, or use functions like `g_object_bind_property_with_closures`.
- Reference-counted classes are automatically cleaned by java-gi, but for other types, like `GString`, you still need to manually call [`free`](https://docs.gtk.org/glib/method.String.free.html) when applicable. (GObject-Introspection [recently added](https://gitlab.gnome.org/GNOME/gobject-introspection/-/merge_requests/365) annotations for copy and free functions that will help implement this in java-gi.)
- The code is not yet completely portable. Specifically: When generating memory layouts for structs, the generator assumes a 64-bit Linux platform with regards to the size of data types.
- While running the gradle build script, a large number of DocLint warnings occur during Javadoc generation. These are safe to ignore. The quality and completeness of the generated Javadoc is still a work in progress.
- Some functions (like `Gio.DesktopAppInfo.search`) work with nested arrays (`gchar***`). These aren't supported yet.
