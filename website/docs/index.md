# Welcome to Java-GI

Java-GI generates Java bindings for native libraries like Gtk, LibAdwaita and GStreamer using GObject-Introspection data. The bindings use the Panama Foreign Function & Memory API (JEP-434) to directly access native resources from inside the JVM.

Please note that java-gi is still under active development. Feedback is welcome.

## How to use

* [Basic usage](usage.md)

* [Registering new GTypes](register.md)

* [Creating composite template classes](templates.md)

* [Generating bindings for other libraries](generate.md)

## Supported libraries

Java-GI version 0.6.0 has been tested with the following libraries:

| Library       | Version |
|---------------|---------|
| GLib          | 2.76    |
| GTK           | 4.10    |
| LibAdwaita    | 1.3     |
| GStreamer     | 1.20    |
| GtkSourceview | 5.9     |
| WebkitGtk     | 2.41    |

For these libraries, pre-built Java bindings are available from JitPack.io. Consult the [usage guide](usage.md) for more information.

## API documentation

Java-GI converts API documentation from GObject-Introspection to Javadoc. For the library bindings published by Java-GI, the complete Javadoc pages are available online:

* Javadoc for [GLib](https://jwharm.github.io/java-gi/glib/org.gnome.glib/module-summary.html)
    
* Javadoc for [Gtk](https://jwharm.github.io/java-gi/gtk/org.gnome.gtk/module-summary.html)
    
* Javadoc for [Adwaita](https://jwharm.github.io/java-gi/adwaita/org.gnome.adwaita/module-summary.html)
    
* Javadoc for [GStreamer](https://jwharm.github.io/java-gi/gstreamer/org.freedesktop.gstreamer/module-summary.html)

JAR files with the Javadoc and source code, to use offline in your IDE, are available from [JitPack.io](https://jitpack.io/#jwharm/java-gi/v0.5.1).

## Example apps

You can find example applications in the [java-gi-examples repository](https://github.com/jwharm/java-gi-examples).

## Contributing

To build Java-GI for yourself, make changes, or use Java-GI to generate bindings for other (GObject-Introspection based) libraries, follow the instructions [here](https://jwharm.github.io/java-gi/generate/). Please log issues, questions and requests on [Github](https://github.com/jwharm/java-gi).
