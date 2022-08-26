# java-gi
*java-gi* is a tool for generating GObject-Introspection bindings for Java. The generated bindings use the Panama foreign function & memory access API (JEP 424) and the jextract tool for accessing native resources.

Panama allows for relatively easy interoperability with native code but the jextract-generated binding classes are very difficult to use directly. C functions like `gtk_button_new_with_label(MemoryAddress label)` still need to be converted into `new gtk.Button(String label)`. This is possible with GObject Introspection. The *java-gi* tool tries to achieve this.

## Prerequisites
- An Early Access build of Java 19
- GTK header files
- GObject-introspection (gir) files

## How to use
Run the `java-gi.sh` script to generate the bindings. By default it generates bindings for GTK4 and its dependencies. It performs the following steps:
- First, it runs jextract to generate a jar file that contains "raw" bindings based on the C header files. It uses `pkg-config` to set the correct include and library paths, and a file `gtk.h` where the necessary includes (besides `<gtk.h>` itself) are listed.
- Next, it starts a Java program that parses gir files (often found in `/usr/share/gir-1.0`) and generates bindings (.java source files).
- Finally, the bindings are compiled and compressed into a .jar file.
