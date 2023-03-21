# Advanced usage

## Register a new type

Registering a Java class as a new GType is documented [here](register.md).

## Create a Gtk composite template class

To create a composite template class to use in a Gtk application, read [these instructions](templates.md).

## Builder pattern

You can construct an object with GObject Properties using a Builder pattern. For example, to create a new ApplicationWindow:

```java
var window = ApplicationWindow.builder()
    .setApplication(this)
    .setTitle("Window")
    .setDefaultWidth(300)
    .setDefaultHeight(200)
    .build();
```

With a `Builder` you can set the properties of the class, its parents, and all implemented interfaces.

## Exceptions

`GError**` parameters are mapped to Java `GErrorException`s.

```java
try {
    file.replaceContents(contents, null, false, FileCreateFlags.NONE, null, null);
} catch (GErrorException e) {
    e.printStackTrace();
}
```

## Allocating structs

Struct definitions (in contrast to GObjects) in native code are mapped to Java classes. Because structs don't necessarily have a constructor method, the Java classes offer an `allocate()` method to allocate a new uninitialized struct. To determine the size of the struct and its members, the memory layouts have been generated from the field definitions in the gir files. The allocated memory is automatically released when the Java object is garbage-collected.

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
