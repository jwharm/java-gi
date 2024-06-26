# Basic usage

## Required Java version

To use Java-GI, download and install [JDK 22](https://jdk.java.net/22/) or newer. Java-GI uses the "Panama" Foreign Function & Memory API that is only available since JDK 22.

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
    implementation 'io.github.jwharm.javagi:gtk:0.10.1'
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

Build and run the application using your IDE or build tool of choice. The following command-line parameters are useful:

- Add `--enable-native-access=ALL-UNNAMED` to suppress warnings about native access.

- If you encounter an error about a missing library, override the java library path with `"-Djava.library.path=/usr/lib/..."`.

See [this `build.gradle` file](https://github.com/jwharm/java-gi-examples/blob/main/HelloWorld/build.gradle) for a complete example.

!!! tip "Use an IDE"
    An Integrated Development Environment (IDE) with support for Java is the most efficient way to develop software in Java. IDEs will help setup a project and build configuration, navigate and refactor source code, detect problems and suggest improvements. The most commonly used Java IDEs are:
    
    * [JetBrains IntelliJ IDEA](https://www.jetbrains.com/idea/)
    * [Eclipse IDE](https://eclipseide.org/)
    * [Apache NetBeans](https://netbeans.apache.org/)
    * [Visual Studio Code with a Java extension](https://code.visualstudio.com/docs/languages/java)
    
    If you often work from the command line, [SDKMAN!](https://sdkman.io/) will proove useful to manage your installed SDKs and build tools.

## Further reading

For more advanced instructions on using Java-GI consult [this page](advanced.md), and read about subclassing GObject classes with Java-GI [here](register.md). If you're new to Gtk development, read the [Gtk "Getting started" guide](getting-started/getting_started_00.md) that has been translated to use Java for all code examples. 

## Platform-specific notes

### Linux

On most Linux distributions, Gtk will already be installed. Java-GI will load shared libraries using `dlopen`, and fallback to the `java.library.path`. So in most cases, you can simply run your application with `--enable-native-access=ALL-UNNAMED`:

```groovy
tasks.named('run') {
    jvmArgs += "--enable-native-access=ALL-UNNAMED"
}
```

### MacOS

On MacOS, you can install Gtk using Homebrew. Gtk needs to run on the main thread, therefore you need to set the parameter `-XstartOnFirstThread`. A complete Gradle `run` task will look like this:

```groovy
tasks.named('run') {
    jvmArgs += "--enable-native-access=ALL-UNNAMED"
    jvmArgs += '-Djava.library.path=/opt/homebrew/lib'
    jvmArgs += '-XstartOnFirstThread'
}
```

### Windows

On Windows, Gtk can be installed with MSYS2. A Gradle `run` task will look like this:

```groovy
tasks.named('run') {
    jvmArgs += "--enable-native-access=ALL-UNNAMED"
    jvmArgs += '-Djava.library.path=C:/msys64/mingw64/bin'
}
```

