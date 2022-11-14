# java-gi

**java-gi** is a very **experimental, work-in-progress** tool for generating GObject-Introspection bindings for Java. The generated bindings use the Panama foreign function & memory access API (JEP 424) for accessing native resources, and add wrapper classes based on GObject-Introspection to offer an easy API to use with Java. The included Gradle build scripts generate bindings for GTK4 and LibAdwaita.

Java-gi bindings are automatically generated from GI data (gir files).
Panama allows for relatively easy interoperability with native code, but jextract-generated binding classes are very difficult to use directly.
C functions like `gtk_button_set_icon_name(GtkButton* button, const char* icon_name)` are mapped to a static Java method `gtk_button_set_icon_name(MemoryAddress button, MemoryAddress icon_name)`.
Using GObject-Introspection, it is possible to generate a wrapper API that includes "proxy" classes that native functions with automatically marshalled parameters and return values, for example `button.setIconName(iconName)`.
Java-gi tries to achieve this.

## Quickstart

- First, download and install [JDK 19](https://jdk.java.net/19/).
- Download [gtk4-0.1.jar](https://github.com/jwharm/java-gi/releases/download/v0.1/gtk4-0.1.jar) and add it to the Java module path. The 0.1 release contains bindings for GTK version 4.6.2.
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
- It is recommended to download the [Javadoc documentation](https://github.com/jwharm/java-gi/releases/download/v0.1/gtk4-0.1-javadoc.jar) to assist during the development of your GTK application. Optionally, download the [sources](https://github.com/jwharm/java-gi/releases/download/v0.1/gtk4-0.1-sources.jar) too.

## Generating java-gi bindings

If you want to generate bindings by yourself, by following these steps:
- First, download and install [JDK 19](https://jdk.java.net/19/) and [Gradle](https://gradle.org/).
- Gradle doesn't run on JDK 19 yet, so you will also need to install a supported JDK, for example [JDK 18](https://jdk.java.net/18/), and configure Gradle to use it, until Gradle version 7.6 is released.
- Make sure that `javac` and `java` from JDK 19 and `gradle` are in your `PATH`.
- Install the GObject-introspection (gir) files of the library you want to generate bindings for. 
  For example, in Fedora, to install the gir files for GTK4 and LibAdwaita, execute: `sudo dnf install gtk4-devel glib-devel libadwaita-devel gobject-introspection-devel`
- Running `gradle build` is enough to generate and build gtk4 bindings.
- If you wish to create bindings for other libraries, you can run the extractor to generate source files which you can then compile.

## Features

Some interesting features of the bindings:
* Because Panama (JEP 424) allows direct access to native resources from the JVM, a 'glue library' that solutions using JNI or JNA need to interface between Java and native code, is unnecessary.
* GtkDoc API docstrings are translated into Javadoc, so they are directly available in your IDE.
* Interfaces are mapped to Java interfaces, using `default` interface methods to call native methods.
* Signals are mapped to type-safe methods and objects in Java. (Detailed signals like `notify` have an extra `String` parameter.)
* Memory management of `GObject`s is automatically taken care of: When a ref-counted object (like `GObject` and its descendants) is fully "owned" by the user, and the object becomes unreachable in Java, a call to `g_object_unref` is automatically executed.
* Most functions with callback parameters are supported. Java-gi uses the `user_data` parameter to store a reference to the Java callback.
* Nullability of parameters is indicated with `@Nullable` and `@NotNull` attributes, and checked at runtime.
* Out-parameters are mapped to a simple `Out<T>` container-type in Java, that offers typesafe `get()` and `set()` methods to retrieve or modify the value.
* Arrays with a known length are mapped to Java arrays.
* `GError**` parameters are mapped to Java `GErrorException`s.
* Variadic functions (varargs) are supported.
* All generated classes contain a `castFrom()` method to cast between different GTypes. This method is typesafe: Illegal casts throw a `ClassCastException`.
* Record types (`struct`s in native code) are mapped to Java classes. Because these types do not always offer a constructor method, the Java classes offer an `allocate()` method to allocate a new uninitialized record. The memory layouts have been generated from the field definitions in the gir files.
* Ability to rename or remove classes or methods in the build script.

## Known issues

The bindings are still under active development and have not been thoroughly tested yet. The most notable issues and missing features are currently:
* Java does not support unsigned data types. You might encounter issues when native code expects, for example, a `guint` parameter.
* You cannot create new GObject types, or subclass existing ones, from Java code.
* The generator has not been tested yet on different Linux distributions or GTK versions.
* A large number of warnings occur during javadoc generation.
* Thread-safety has not been considered yet.
* Unions (including GValue) aren't supported yet.
* Return values of nested arrays (like Gio `g_desktop_app_info_search`) aren't supported yet.
