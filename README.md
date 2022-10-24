# java-gi

**java-gi** is a very **experimental, work-in-progress** tool for generating GObject-Introspection bindings for Java. The generated bindings use the Panama foreign function & memory access API (JEP 424) for accessing native resources, and add wrapper classes based on GObject-Introspection to offer an easy API to use with Java. The included Gradle build scripts generate bindings for GTK4 and LibAdwaita.

Java-gi bindings are automatically generated from GI data (gir files).
Panama allows for relatively easy interoperability with native code, but jextract-generated binding classes are very difficult to use directly.
C functions like `gtk_button_set_icon_name(GtkButton* button, const char* icon_name)` are mapped to a static Java method `gtk_button_set_icon_name(MemoryAddress button, MemoryAddress icon_name)`.
Using GObject-Introspection, it is possible to generate a wrapper API that includes "proxy" classes that native functions with automatically marshalled parameters and return values, for example `button.setIconName(iconName)`.
Java-gi tries to achieve this.

## Prerequisites

- First, download and install [JDK 19](https://jdk.java.net/19/) and [Gradle](https://gradle.org/). The Gradle build will also download and install a dependency jar [JetBrains Annotations](https://www.jetbrains.com/help/idea/annotating-source-code.html).
- Gradle doesn't run on JDK 19 yet, so you will also need to install a supported JDK, for example [JDK 18](https://jdk.java.net/18/), and configure Gradle to use it, until version 7.6 is released.
- Make sure that `javac` and `java` from JDK 19 and `gradle` are in your `PATH`.
- Install the GObject-introspection (gir) files of the library you want to generate bindings for. 
  For example, in Fedora, to generate bindings for GTK4 and LibAdwaita, execute: `sudo dnf install gtk4-devel glib-devel libadwaita-devel gobject-introspection-devel`

## How to create bindings

Running `gradle build` is enough to generate and build gtk4 bindings.
If you wish to create bindings for other libraries, you can run the extractor to generate source files which you can then compile.

## What the bindings look like

A "Hello world" example can be found in [`example`](https://github.com/jwharm/java-gi/blob/main/example/src/main/java/io/github/jwharm/javagi/example/HelloWorld.java)

Because the Panama foreign function API is still in preview status, to run the above application, be sure to add the `--enable-preview` command-line parameter when running `javac` and `java`. To suppress warnings about illegal native access, add the parameter `--enable-native-access=org.gtk`.

## Features
Some interesting features of the bindings:
* Because Panama (JEP 424) allows direct access to native resources from the JVM, a 'glue library' that solutions using JNI or JNA need to interface between Java and native code, is unnecessary.
* Interfaces are mapped to Java interfaces, using Java 8-style `default` methods to call native methods.
* Signals are mapped to type-safe methods and objects in Java.
* Memory management of `GObject`s is automatically taken care of: When a ref-counted object (like `GObject` and its descendants) is "owned" by the user and the proxy object in Java gets garbage-collected, a call to `g_object_unref` is automatically executed to release the native resources.
* Functions with callback parameters are supported when there is a `user_data` parameter available to store a reference to the Java callback.
* Nullability of parameters is indicated with `@Nullable` and `@NotNull` attributes.
* Out-parameters are mapped to a simple `Out<T>` container-type in Java, that offers typesafe `get()` and `set()` methods to retrieve or modify the value.
* Arrays with a known length are mapped to Java arrays.
* `GError**` parameters are mapped to Java `GErrorException`s. (This is currently broken, but will be fixed soon.)
* Ability to rename or remove classes or methods in the build script.
* GtkDoc API docstrings are (roughly) translated into Javadoc (though this also needs more work).

## Known issues
The bindings are still under active development and have not been thoroughly tested yet. The most notable issues and missing features are currently:
* The generator has not been tested yet on different Linux distributions or GTK versions.
* The allocator used to allocate native memory segments ([`MemorySegment.newNativeArena`](https://docs.oracle.com/en/java/javase/19/docs/api/java.base/java/lang/foreign/SegmentAllocator.html#newNativeArena(java.lang.foreign.MemorySession))) does not release the memory afterwards. This means java-gi apps leak memory like crazy. The obvious solution is to use [`MemorySegment.implicitAllocator`](https://docs.oracle.com/en/java/javase/19/docs/api/java.base/java/lang/foreign/SegmentAllocator.html#implicitAllocator()) which releases memory automatically with a `Cleaner`, but currently the memory segments are released too soon, so it requires more work (and more testing).
* `null` parameters are passed directly to native code. These should at the very least be marshalled to `MemoryAddress.NULL` and vice versa.
* `GErrorException` doesn't do anything right now.
* Signals with `::detail`s are not supported yet. This is hard to implement in a type-safe way. I will probably add a workaround for `Object.notify`.
* Errors occur during javadoc generation: invalid combinations of html tags, lists that aren't closed properly, dead links, and probably a few others issues.
* The javadoc doesn't include the documentation of parameters and return values yet.
* I haven't looked into GObject properties and ParamSpecs yet.
* The `castFrom()` method doesn't do type checks, so if you accidentally try to cast an object to another GType, it will crash the JVM.
* Methods marked as `deprecated` are currently excluded.
* Thread-safety has not been considered yet.
* Varargs aren't supported yet.
* Unions aren't supported.
* Return values of nested arrays (like Gio `g_desktop_app_info_search`) aren't supported yet.
