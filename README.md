# Java-GI

**Java-GI** is a tool for generating GObject-Introspection bindings for Java. The generated bindings use the [Panama Foreign Function & Memory API](https://openjdk.org/projects/panama/) (JEP 424 in JDK 19, JEP 434 in JDK 20, both in preview status) to directly access native resources from inside the JVM, and add wrapper classes based on GObject-Introspection to offer an elegant API. Java-gi version 0.4 generates bindings for the following libraries:

| Library    | Version |
|------------|---------|
| GLib       | 2.74    |
| GTK        | 4.8     |
| LibAdwaita | 1.2     |
| GStreamer  | 1.20    |

Please note that Java-GI is still under active development. The bindings cannot be used in a production environment yet, and the API is subject to unannounced changes. However, feel free to try out the latest release; feedback is welcome.

## Quickstart

- First, download and install [JDK 19](https://jdk.java.net/19/).
- To create a Gtk application, download the GLib and Gtk jars from the [packages section](https://github.com/jwharm?tab=packages&repo_name=java-gi). Make sure to download the versions that end with '-0.4', and not an older release or a snapshot build.

- Add `requires org.gnome.gtk;` to your `module-info.java` file.
- Write, compile and run your GTK application:

```java
package io.github.jwharm.javagi.example;

import org.gnome.gtk.*;
import org.gnome.gio.ApplicationFlags;

public class HelloWorld extends Application {

    public static void main(String[] args) {
        var app = new HelloWorld("my.example.HelloApp", ApplicationFlags.DEFAULT_FLAGS);
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

- Because the Panama foreign function API is still in preview status, add the `--enable-preview` command-line parameter when compiling and running your application. To suppress warnings about native access, also add `--enable-native-access=org.gnome.glib,org.gnome.gtk`.
- It is recommended to download the Javadoc documentation and sources to assist during the development of your GTK application. Both are available in the Packages section.

## Generating Java-GI bindings

The instructions above link to pre-built bindings for the entire GTK4 library stack and LibAdwaita.

If you want to generate bindings for other versions, platforms or libraries, follow these steps:
- Clone the Java-GI project, either the latest release 0.4 (based on OpenJDK 19) or the latest version from the main branch (unstable, and based on OpenJDK 20).
- First, download and install [JDK 19](https://jdk.java.net/19/) or JDK 20.
- Running `./gradlew build` is enough to generate and build GLib, Gtk, Adwaita and GStreamer bindings.

The GIR files are downloaded from [gircore/gir-files](https://github.com/gircore/gir-files) repository by the Gradle build script, so you don't need to install any development packages.

The resulting jar files are located in the `[module]/build/libs` folders.

If you run Java-GI from the main branch, you might encounter errors about an unsupported class file version. Gradle does not support JDK 20 yet. It works for me, but YMMV.

## Features

Some interesting features of the bindings that Java-GI generates:

### Coverage

Almost all types, functions and parameters defined in the GIR files for GTK and GStreamer are supported by java-gi. Even complex function signatures with combinations of arrays, callbacks, out-parameters and varargs are available in Java.

### Javadoc

The API docstrings are translated into Javadoc, so they are directly available in your IDE.

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

The Javadoc is also published online:

- [GLib](https://jwharm.github.io/java-gi/glib/org.gnome.glib/module-summary.html)
- [Gtk](https://jwharm.github.io/java-gi/gtk/org.gnome.gtk/module-summary.html)
- [Adwaita](https://jwharm.github.io/java-gi/adwaita/org.gnome.adwaita/module-summary.html)
- [GStreamer](https://jwharm.github.io/java-gi/gstreamer/org.freedesktop.gstreamer/module-summary.html)

### Interfaces

Interfaces are mapped to Java interfaces, using `default` interface methods to call native methods.

### Type aliases

Type aliases (`typedef`s in C) for classes, records and interfaces are represented in Java with a subclass of the original type. Aliases for primitive types such as `int` or `long` are represented by a simple object with `getValue` and `setValue` methods.

### Enumerations

Enumeration types are represented as Java `enum` types.

### Named constructors

Constructors in GTK are often overloaded, and the name contains valuable information for the user. Therefore, Java-GI maps constructors named "new" to regular Java constructors, and generates static factory methods for all other constructors:

```java
// gtk_button_new
var button1 = new Button();

// gtk_button_new_with_label
var button2 = Button.newWithLabel("Open...");

// gtk_button_new_from_icon_name
var button3 = Button.newFromIconName("document-open");
```

### Automatic memory management

Memory management of `GObject`s is automatically taken care of. The Java-GI bindings will call `g_object_unref` when an object is not used anymore. Floating references (`GInitiallyUnowned` for example) are automatically `ref_sink`ed after construction.

Memory that is allocated when calling native functions (for string and array parameters) or when allocating a struct type, is released after use; either using `try-with-resources` statements or by a `Cleaner` during garbage collection.

### Signals, callbacks and closures

Signals are mapped to type-safe methods and objects in Java. (Detailed signals like `notify` have an extra `String` parameter.) A signal can be connected to a lambda expression or method reference:

```java
var button = Button.newWithLabel("Close");
button.onClicked(window::destroy);
```

For every signal, a method to connect (e.g. `onClicked`) and emit the signal (`emitClicked`) is included in the API. New signal connections return a `Signal` object, that allows you to disconnect, block and unblock a signal, or check whether the signal is still connected.

Functions with callback parameters are supported too. The generated Java bindings contain `@FunctionalInterface` definitions for all callback functions to ensure type safety.

[Closures](https://docs.gtk.org/gobject/struct.Closure.html) are marshaled to Java methods using reflection.

### Registering new types

You can easily register a Java object with the GObject type system:

```java
public class MyObject extends GObject {

    private static Type type;
    
    public Type getType() {
        if (type == null)
            type = Types.register(MyObject.class);
        return type;
    }

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

    // Construct new instance
    public MyObject newInstance() {
        return GObject.newInstance(getType());
    }

    public MyObject(Addressable address) {
        super(address);
    }
}
```

Because `Types.register(Class<?> cls)` uses reflection to determine the name and properties of the new type, you must add `exports [package name] to org.gnome.glib;` to your `module-info.java` file.

The next Java-GI release will also allow a `@Property` annotation, that you can use on getter/setter methods. Java-GI will automatically register GObject properties for these methods:

```java
@Property(name="n-items", type=ParamSpecInt.class, writable=false)
public int getNItems() {
    return items.size();
}
```

### Composite template classes

A class with a `@GtkTemplate` annotation will be registered as a Gtk template class:

```java
@GtkTemplate(name="HelloWindow", ui="/my/example/hello-window.ui")
public class HelloWindow extends ApplicationWindow {

    private static Type type;

    public static Type getType() {
        if (type == null)
            type = Types.registerTemplate(HelloWindow.class);
        return type;
    }

    @GtkChild
    public HeaderBar header_bar;

    @GtkChild
    public Label label;
    
    @GtkCallback
    public void buttonClicked() {
        ...
    }

    ...
```

In the above example, the `header_bar` and `label` fields and the `buttonClicked` callback function are all declared the UI file.

Because the registration of composite template classes uses reflection, you must add `exports [package name] to org.gnome.glib,org.gnome.gtk;` to your `module-info.java` file.

### Null-checks

Nullability of parameters (as defined in the GObject-introspection attributes) is indicated with `@Nullable` and `@NotNull` attributes, and checked at runtime.

The nullability attributes are imported from Jetbrains Annotations (as a compile-time-only dependency).

### Out-parameters

Out-parameters are mapped to a simple `Out<T>` container-type in Java, that offers typesafe `get()` and `set()` methods to retrieve or modify the value.

```java
File file = ...
Out<byte[]> contents = new Out<byte[]>();
file.loadContents(null, contents, null));
System.out.printf("Read %d bytes\n", contents.get().length);
```

### Arrays and pointers

Arrays with a known length (specified as gobject-introspection annotations) are mapped to Java arrays. Java-GI generates code to automatically copy native array contents from and to a Java array, marshaling the contents to the correct types along the way. A `null` terminator is added where applicable. You also don't need to specify the array length as a separate parameter (even though many C functions need this) because Java-GI will insert it for you automatically.

As an example:

```C
int g_application_run (GApplication* application, int argc, char** argv)
```

is in Java:

```Java
class Application {
    public int run(@Nullable java.lang.String[] argv)
}
```

The `String[]` argument is automatically marshaled to a `char**` array, and `argc` is omitted from the Java API. The allocated `char**` memory is immediately released afterwards.

In some cases, the length of an array is impossible to determine from the GObject-Introspection attributes, or a raw pointer is expected/returned. Java-GI marshals these values to a `Pointer` class (with typed subclasses) that can dereference the value of a pointer, or iterate through an unbounded sequence of memory locations. Needless to say that this is extremely unsafe.

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

With a `Builder` you can set the properties of the class, its parents, and all implemented interfaces.

### Allocating structs

Struct definitions (in contrast to GObjects) in native code are mapped to Java classes. Because structs don't necessarily have a constructor method, the Java classes offer an `allocate()` method to allocate a new uninitialized struct. To determine the size of the struct and its members, the memory layouts have been generated from the field definitions in the gir files. The allocated memory is automatically released when the Java object is garbage-collected.

### Naming conventions

All Java types and methods are named according to Java naming conventions. Namespaces are converted into package names (reverse-domain-name style), for example `org.cairographics` for the `Cairo` namespace.

Namespace-global functions and constants are exposed in one class, for example the class `org.gnome.glib.GLib` contains static members for all global declarations in the GLib namespace.

A small number of types have been renamed in the Java API to avoid confusion:

- [`GObject.Object`](https://docs.gtk.org/gobject/class.Object.html), [`GLib.String`](https://docs.gtk.org/glib/struct.String.html) and [`GLib.Error`](https://docs.gtk.org/glib/struct.Error.html) are named `GObject`, `GString` and `GError` (instead of `Object`, `String` and `Error`) to avoid confusion with `java.lang.Object`
- As a result, the namespace-global functions and constants in the `GObject` namespace can be found in `org.gnome.gobject.GObjects` (notice the trailing 's').
- [`Gtk.Builder`](https://docs.gtk.org/gtk4/class.Builder.html) is named `GtkBuilder` (instead of `Builder`) to avoid confusion with the `Builder` that Java-GI generates for every class.

## Known issues

The bindings are still under active development and have not been thoroughly tested yet. The most notable issues and missing features are currently:

- Java does not support unsigned data types. Be extra careful when native code returns, for example, a `guint`.
- There are still a number of memory leaks.
- Release 0.4 is not yet completely portable. Specifically: When generating memory layouts for structs, the generator assumes a 64-bit Linux platform with regards to the size of data types. This will be fixed in the next release.
- Some functions (like `Gio.DesktopAppInfo.search`) work with nested arrays (`gchar***`). These aren't supported yet.
