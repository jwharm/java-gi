# Basic usage

## Required Java version

First, download and install [JDK 20](https://jdk.java.net/20/). Java-GI uses the "Panama" Foreign Function & Memory API that is available as a preview feature in JDK 20.

## Dependencies

Make sure that the native GLib, Gtk and/or GStreamer libraries are installed on your operating system.

* If you use Linux, Gtk is often installed by default. On Windows or MacOS, follow the [installation instructions](https://www.gtk.org/docs/installations/).

* GStreamer: Follow the [installation instructions](https://gstreamer.freedesktop.org/documentation/installing/).

Next, add the dependencies as described on [JitPack.io](https://jitpack.io/#jwharm/java-gi/0.6.1). For example, if you use Gradle:

```groovy
allprojects {
	repositories {
		mavenCentral()
		maven { url 'https://jitpack.io' }
	}
}

dependencies {
    // For the @Nullable/@NotNull annotations
    compileOnly 'org.jetbrains:annotations:24.+'
    
    // Include the libraries you want to use. 
    implementation 'com.github.jwharm.java-gi:glib:latest.release'
    implementation 'com.github.jwharm.java-gi:gtk:latest.release'
    implementation 'com.github.jwharm.java-gi:adwaita:latest.release'
    implementation 'com.github.jwharm.java-gi:gstreamer:latest.release'
    implementation 'com.github.jwharm.java-gi:gtksourceview:latest.release'
    implementation 'com.github.jwharm.java-gi:webkitgtk:latest.release'
}
```

The `glib` jar contains GLib, GIO, GObject and GModule, and the Java-GI base classes. You should always include it. The other jars only need to be included if you intend to use that specific functionality.

The `gtk` jar contains, besides Gtk, also the bindings for Gdk, GdkPixbuf, Graphene, HarfBuzz and Pango. Furthermore, it depends on the [cairo-java-bindings](https://github.com/jwharm/cairo-java-bindings). Maven or Gradle will download the cairo dependency from JitPack.io automatically.

## Application code

An example Gtk application with a "Hello world" button can be created as follows:

```java
package io.github.jwharm.javagi.examples.helloworld;

import org.gnome.gtk.*;
import org.gnome.gio.ApplicationFlags;

public class HelloWorld {

    public static void main(String[] args) {
        new HelloWorld(args);
    }
    
    private final Application app;
    
    public HelloWorld(String[] args) {
        app = new Application("my.example.HelloApp", ApplicationFlags.DEFAULT_FLAGS);
        app.onActivate(this::activate);
        app.run(args);
    }
    
    public void activate() {
        var window = new ApplicationWindow(app);
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
        window.present();
    }
}
```

## Compile and run

Build and run the application with the following parameters:

* Set the Java language version: `--release 20`

* While the Panama foreign function API is still in preview status, set the `--enable-preview` option both when **compiling** and **running** your application.

* To suppress warnings about native access, also add `--enable-native-access=ALL-UNNAMED`. For module-based applications, add `--enable-native-access=org.gnome.glib` (and all other modules that you use) instead.

* If you encounter an error about a missing library, override the java library path with `"-Djava.library.path=/usr/lib/..."`

See [this `build.gradle` file](https://github.com/jwharm/java-gi-examples/blob/main/HelloWorld/build.gradle) for a complete example.

## Java library path

If you see an error about a missing library, make sure that all dependencies are installed, and available on Java library path (the `"java.library.path"` system property). If necessary, you can override the Java library path with the `-Djava.library.path=` JVM argument, for example: `-Djava.library.path=/lib/x86_64-linux-gnu` on Debian-based systems.

## Modules

For module-based applications (with a `module-info.java` file), the following modules are available:

* org.gnome.glib
    * org.freedesktop.gstreamer
    * org.gnome.gtk
        * org.gnome.adwaita
        * org.gnome.gtksourceview
        * org.gnome.webkitgtk

Each module transitively exports its dependencies, so when you want to create a Gtk application, you only need to add `requires org.gnome.gtk;` to your `module-info.java` file.
