# Advanced usage

## Memory management

Memory management of native resources is automatically taken care of. Java-GI uses GObject toggle references to dispose the native object when the Java instance is garbage-collected, and releases all other memory allocations (for strings, arrays and structs) after use.

## Builder pattern

You can construct an object with GObject properties using a Builder pattern. For example, to create a new ApplicationWindow:

```java
var window = ApplicationWindow.builder()
    .setApplication(this)
    .setTitle("Window")
    .setDefaultWidth(300)
    .setDefaultHeight(200)
    .build();
```

With a `Builder` you can set the properties of the class, its parents, and all implemented interfaces. Behind the scenes, this will call `g_object_new_with_properties()`.

## Exceptions

`GError**` parameters are mapped to Java `GErrorException`s.

```java
try {
    file.replaceContents(contents, null, false, FileCreateFlags.NONE, null, null);
} catch (GErrorException e) {
    e.printStackTrace();
}
```

Use `GErrorException.getCode()`, `getDomain()` and `getMessage()` to get the GError code, domain and message. Java-GI does not generate separate Exception types for different GError domains, because the domain is only set at runtime.

## Allocating structs

Struct definitions (in contrast to GObjects) in native code are mapped to Java classes. Because structs don't necessarily have a constructor method, the Java classes offer an `allocate()` method to allocate a new uninitialized struct. To determine the size of the struct and its members, the memory layouts have been generated from the field definitions in the gir files. The allocated memory is automatically released when the Java object is garbage-collected.

## Nullable/NotNull parameter annotations

Nullability of parameters (as defined in the GObject-introspection attributes) is indicated with `@Nullable` and `@NotNull` attributes, and checked at runtime. The nullability attributes are imported from Jetbrains Annotations (as a compile-time-only dependency).

## Arrays

C functions that work with arrays, often expect the array length as an additional parameter. In the corresponding Java methods, that parameter is unnecessary, because Java-GI will set it automatically.

## Out-parameters

Out-parameters are mapped to a simple `Out<T>` container-type in Java, that offers typesafe `get()` and `set()` methods to retrieve or modify the value.

```java
File file = ...
Out<byte[]> contents = new Out<byte[]>();
file.loadContents(null, contents, null));
System.out.printf("Read %d bytes\n", contents.get().length);
```

## Varargs

Variadic functions are available in Java using varargs:

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

Be aware that with most variadic functions in GLib, you are expected to add `null` as a final parameter.

**Note:** Java-GI currently provides bindings for native functions with a `va_list` parameter, but these bindings will be removed in a future version, because the upcoming third preview of the Panama FFI (JEP 442) in OpenJDK 21 will remove the `VaList` class.

## Signals and callbacks

Signals are mapped to type-safe methods and objects in Java. (Detailed signals like `notify` have an extra `String` parameter.) A signal can be connected to a lambda expression or method reference:

```java
var button = Button.newWithLabel("Close");
button.onClicked(window::close);
```

For every signal, a method to connect (e.g. `onClicked`) and emit the signal (`emitClicked`) is included in the API. New signal connections return a `Signal` object, that allows you to disconnect, block and unblock a signal, or check whether the signal is still connected.

Functions with callback parameters are supported too. The generated Java bindings contain `@FunctionalInterface` definitions for all callback functions to ensure type safety.

## Closures

[Closures](https://docs.gtk.org/gobject/struct.Closure.html) can be marshaled to Java methods. Similar to the `CClosure` type in C code, Java-GI offers a [JavaClosure](https://jwharm.github.io/java-gi/glib/org.gnome.glib/io/github/jwharm/javagi/util/JavaClosure.html). You can create a JavaClosure for a `Runnable`, a `BooleanSupplier`, or a `java.lang.reflect.Method`, and then pass it to native code (for example, the last two parameters of [`GObject.bindPropertyFull()`](https://jwharm.github.io/java-gi/glib/org.gnome.glib/org/gnome/gobject/GObject.html#bindPropertyFull(java.lang.String,org.gnome.gobject.GObject,java.lang.String,org.gnome.gobject.BindingFlags,org.gnome.gobject.Closure,org.gnome.gobject.Closure))).

Be aware that the Java method that is wrapped in a JavaClosure must have the correct signature, or else the application will fail at runtime. Closures cannot be type-checked by the compiler!

## Registering a new type

Registering a Java class as a new GType is documented [here](register.md).

## Creating a Gtk composite template class

To create a composite template class to use in a Gtk application, read [these instructions](templates.md).
