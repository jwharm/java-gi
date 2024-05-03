# Basic usage

## Required Java version

First, download and install [JDK 21](https://jdk.java.net/21/). Java-GI uses the "Panama" Foreign Function & Memory API that is available as a preview feature in JDK 21.

## Dependencies

Make sure that the native GLib, Gtk and/or GStreamer libraries are installed on your operating system.

- If you use Linux, Gtk is often installed by default. On Windows or MacOS, follow the [installation instructions](https://www.gtk.org/docs/installations/).

- GStreamer: Follow the [installation instructions](https://gstreamer.freedesktop.org/documentation/installing/).

Next, add the dependencies. For example, if you use Gradle:

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'io.github.jwharm.javagi:gtk:0.9.1'
}
```

This will add the Gtk bindings to the application's compile and runtime classpath. Other libraries, like `webkit`, `gst`, `adw` and `gtksourceview` can be included likewise. The complete list of available libraries is available [here](https://github.com/jwharm/java-gi/tree/main/modules).

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
        
        var button = Button.withLabel("Hello world!");
        button.onClicked(window::close);
        
        box.append(button);
        window.setChild(box);
        window.present();
    }
}
```

## Compile and run

Build and run the application with the following parameters:

- Set the Java language version: `--release 21`

- While the Panama foreign function API is still in preview status, set the `--enable-preview` option both when **compiling** and **running** your application.

- To suppress warnings about native access, also add `--enable-native-access=ALL-UNNAMED`.

- If you encounter an error about a missing library, override the java library path with `"-Djava.library.path=/usr/lib/..."`.

See [this `build.gradle` file](https://github.com/jwharm/java-gi-examples/blob/main/HelloWorld/build.gradle) for a complete example.

## Java library path

If you see an error about a missing library, make sure that all dependencies are installed, and available on Java library path (the `"java.library.path"` system property). If necessary, you can override the Java library path with the `-Djava.library.path=` JVM argument, for example: `-Djava.library.path=/lib/x86_64-linux-gnu` on Debian-based systems.

In the [Java-GI examples](examples.md), the JVM arguments are setup for the system library folders of the common (RedHat/Fedora, Arch and Debian/Ubuntu) Linux distributions:

```groovy
tasks.named('run') {
    jvmArgs += "--enable-preview"
    jvmArgs += "--enable-native-access=ALL-UNNAMED"
    jvmArgs += "-Djava.library.path=/usr/lib64:/lib64:/lib:/usr/lib:/lib/x86_64-linux-gnu"
}
```

On MacOS, if you installed Gtk using Homebrew, the library path is usually `/opt/homebrew/lib`. You also need to add the parameter `-XstartOnFirstThread`. So the complete task definition will look like this:

```groovy
tasks.named('run') {
    jvmArgs += "--enable-preview"
    jvmArgs += "--enable-native-access=ALL-UNNAMED"
    jvmArgs += '-Djava.library.path=/opt/homebrew/lib'
    jvmArgs += '-XstartOnFirstThread'
}
```

On Windows, if you installed Gtk with MSYS2, the default path is `C:\msys64\mingw64\bin`. If that's the case, you can change the build file like this:

```groovy
tasks.named('run') {
    jvmArgs += "--enable-preview"
    jvmArgs += "--enable-native-access=ALL-UNNAMED"
    jvmArgs += '-Djava.library.path=C:/msys64/mingw64/bin'
}
```
