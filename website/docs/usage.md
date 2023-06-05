# Basic usage

## Required Java version

First, download and install [JDK 20](https://jdk.java.net/20/). Java-GI uses the "Panama" Foreign Function & Memory API that is available as a preview feature in JDK 20.

Please be aware that the Panama API is still under development. You will need to pass the `--enable-preview` parameter when compiling and running Java-GI applications.

## Dependencies

First of all, make sure that the native GLib, Gtk and/or GStreamer libraries are installed on your operating system.

Next, add the dependencies as described on [JitPack.io](https://jitpack.io/#jwharm/java-gi/v0.5.1). For example, if you use Gradle:

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
    // Include the libraries you want to use. You will at the very least want to include GLib.
    implementation 'com.github.jwharm.java-gi:glib:v0.5.1'
    implementation 'com.github.jwharm.java-gi:gtk:v0.5.1'
    implementation 'com.github.jwharm.java-gi:adwaita:v0.5.1'
    implementation 'com.github.jwharm.java-gi:gstreamer:v0.5.1'
}
```

Furthermore, you must set the Java language version to 20. And while the Panama foreign function API is still in preview status, set the `--enable-preview` option to the compile and execution tasks. To suppress warnings about native access, also add `--enable-native-access=ALL-UNNAMED`. See [this `build.gradle` file](https://github.com/jwharm/java-gi-examples/blob/main/HelloWorld/build.gradle) for a complete example.

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

Because the Panama foreign function API is still in preview status, add the `--enable-preview` command-line parameter both when **compiling** and **running** your application.

To suppress warnings about native access, add a command-line parameter `--enable-native-access=ALL-UNNAMED`. For module-based applications, add `--enable-native-access=org.gnome.glib` (and all other modules that you use) instead.

## Modules

For module-based applications (with a `module-info.java` file), the following modules are available:

* org.gnome.glib
    * org.freedesktop.gstreamer
    * org.gnome.gtk
        * org.gnome.adwaita

Each module transitively exports its dependencies, so when you want to create a Gtk application, you only need to add `requires org.gnome.gtk;` to your `module-info.java` file.
