# Basic usage

## Required Java version

First, download and install [JDK 19](https://jdk.java.net/19/). Java-GI uses the "Panama" Foreign Function & Memory API that is available as a preview feature in JDK 19.

Please be aware that the Panama API is still under development. The current version of Java-GI has been developed with JDK 19, and will not work with JDK 20. Future versions of Java-GI will be ported to JDK 20 and then JDK 21, etcetera, until Panama enters general availability.

## Dependencies

First of all, make sure that the native GLib, Gtk and/or GStreamer libraries are installed on your operating system.

Next, download the jar files with the Java bindings from [Github Packages](https://github.com/jwharm?tab=packages&repo_name=java-gi). At a minimum, you will need to download the GLib bindings jar. Additionally, download the Gtk, Adwaita and/or GStreamer bindings, and add the jars to the classpath of your project.

It is recommended to download the Javadoc documentation and sources, to assist during the development of your GTK application. They are available for download from Github Packages.

## Modules

The following modules are available:

* org.gnome.glib
    * org.freesdesktop.gstreamer
    * org.gnome.gtk
        * org.gnome.adwaita

Each module transitively exports its dependencies, so when you want to create a Gtk application, you only need to add `requires org.gnome.gtk;` to your `module-info.java` file.

## Application code

An example Gtk application with a "Hello world" button can be created as follows:

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

## Compile and run

Because the Panama foreign function API is still in preview status, add the `--enable-preview` command-line parameter both when **compiling** and **running** your application.

To suppress warnings about native access, add a command-line parameter `--enable-native-access=org.gnome.glib` (and all other modules that you use).
