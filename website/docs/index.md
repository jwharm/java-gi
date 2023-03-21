# Welcome to Java-GI

Java-GI generates Java bindings for native libraries like Gtk, LibAdwaita and GStreamer using GObject-Introspection data. The bindings use the Panama Foreign Function & Memory API (JEP-424) to directly access native resources from inside the JVM.

Please note that java-gi is still under active development. Feedback is welcome.

## How to use

* [Basic usage](usage.md)

* [Registering new GTypes](register.md)

* [Creating composite template classes](templates.md)

* [Generating bindings for other libraries](generate.md)

## Supported libraries

Java-GI version 0.5 has been tested with the following libraries:

| Library    | Version |
|------------|---------|
| GLib       | 2.74    |
| GTK        | 4.8     |
| LibAdwaita | 1.2     |
| GStreamer  | 1.20    |

For these libraries, pre-built Java bindings are available from Maven Central, in separate flavors for Linux, Windows and MacOS. Consult the [usage guide](usage.md) for more information.

## API documentation

Java-GI converts API documentation from GObject-Introspection to Javadoc. For the library bindings published by Java-GI, the complete Javadoc pages are available online:

* Javadoc for [GLib](https://jwharm.github.io/java-gi/glib/org.gnome.glib/module-summary.html)
    
* Javadoc for [Gtk](https://jwharm.github.io/java-gi/gtk/org.gnome.gtk/module-summary.html)
    
* Javadoc for [Adwaita](https://jwharm.github.io/java-gi/adwaita/org.gnome.adwaita/module-summary.html)
    
* Javadoc for [GStreamer](https://jwharm.github.io/java-gi/gstreamer/org.freedesktop.gstreamer/module-summary.html)

JAR files with the Javadoc and source code, to use offline in your IDE, are available from [Github Packages](https://github.com/jwharm?tab=packages&repo_name=java-gi).

## Contributing

Please log issues, questions and requests on [Github](https://github.com/jwharm/java-gi).
