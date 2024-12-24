# Welcome to Java-GI

Java-GI generates Java bindings for native libraries including Gtk, LibAdwaita, GtkSourceview, WebkitGtk and GStreamer using GObject-Introspection data. The bindings use the new Foreign Function & Memory API (JEP-454) to directly access native resources from inside the JVM.

![Screenshot of Java-GI code with the Browser example](img/browser-screenshot.png)

Please note that Java-GI is still under active development. Feedback is welcome.

## How to use

* [Basic usage](usage.md) and [example applications](examples.md)

* [Registering new GTypes](register.md)

* [Creating composite template classes](templates.md)

* [Generating bindings for other libraries](generate.md)

## Supported 

Java-GI version 0.11 requires OpenJDK 22 or newer. Version 0.8.x and 0.9.x work with OpenJDK 21, and version 0.7.2 works with OpenJDK 20. They have been built with the following library versions:

| Library       | Java-GI 0.11.x | Java-GI 0.10.x | Java-GI 0.8.x and 0.9.x | Java-GI 0.7.x |
|---------------|----------------|----------------|-------------------------|---------------|
| OpenJDK       | 22             | 22             | 21                      | 20            |
| GLib          | 2.82           | 2.80           | 2.78                    | 2.76          |
| GTK           | 4.16           | 4.14           | 4.12                    | 4.10          |
| LibAdwaita    | 1.6            | 1.5            | 1.4                     | 1.3           |
| GStreamer     | 1.24           | 1.22           | 1.22                    | 1.20          |
| GtkSourceview | 5.14           | 5.12           | 5.10                    | 5.9           |
| WebkitGtk     | 2.46           | 2.44           | 2.42                    | 2.41          |

The Java bindings for these libraries are available from [Maven Central](https://central.sonatype.com/search?namespace=io.github.jwharm.javagi).

## API documentation

Java-GI converts API documentation from GObject-Introspection to Javadoc. For the library bindings published by Java-GI, [the Javadoc is available online](https://jwharm.github.io/java-gi/javadoc).

JAR files with the Javadoc and source code, to use offline in your IDE, are available from Maven Central.

## Example apps

You can find example applications in the [java-gi-examples repository](https://github.com/jwharm/java-gi-examples).

## Contributing

To build Java-GI for yourself, make changes, or use Java-GI to generate bindings for other (GObject-Introspection based) libraries, follow the instructions [here](https://jwharm.github.io/java-gi/generate/). Please log issues, questions and requests on [Github](https://github.com/jwharm/java-gi), or join the discussion on [Matrix](https://matrix.to/#/#java-gi:matrix.org).
