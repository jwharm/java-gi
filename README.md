# java-gi

**java-gi** is a tool for generating GObject-Introspection bindings for Java. The generated bindings use the Panama foreign function & memory access API (JEP 424) and the `jextract` tool for accessing native resources, and add wrapper classes that offer an easy-to-use API to use with Java.

## Prerequisites

- Download an Early Access build of Java 19 and set JAVA_HOME to its path
- Install GTK-development header files
- Ensure that GObject-introspection (gir) files are in `/usr/share/gir-1.0`

## How to create bindings

Run the `java-gi.sh` script to generate the bindings. By default it generates bindings for GTK4 and its dependencies. It performs the following steps:
- First, it runs jextract to generate a jar file that contains "raw" bindings based on the C header files. It uses `pkg-config` to set the correct include and library paths, and a file `gtk.h` where the necessary includes (besides `<gtk.gtk.h>` obviously) are listed.
- Next, it execute a Java program that parses gir files (from `/usr/share/gir-1.0`) and generates bindings (java source files).
- Finally, the bindings are compiled and compressed into a JAR file.

## What the bindings look like

A "Hello world" example:

```java
import org.gtk.gtk.*;
import org.gtk.gio.ApplicationFlags;

public class HelloWorld {

    public void activate(org.gtk.gio.Application g_application) {
        var window = new ApplicationWindow(Application.castFrom(g_application));
        window.setTitle("Window");
        window.setDefaultSize(300, 200);
        var box = new Box(Orientation.VERTICAL, 0);
        box.setHalign(Align.CENTER);
        box.setValign(Align.CENTER);
        var button = Button.newWithLabel("Hello world!");
        button.onClicked((btn) -> window.close());
        box.append(button);
        window.setChild(box);
        window.show();
    }

    public HelloWorld(String[] args) {
        var app = new Application("org.gtk.example", ApplicationFlags.FLAGS_NONE);
        app.onActivate(this::activate);
        app.run(args.length, args);
    }

    public static void main(String[] args) {
        new HelloWorld(args);
    }
}

```

## Background

Panama allows for relatively easy interoperability with native code, but the jextract-generated binding classes are very difficult to use directly. C functions like `gtk_button_set_icon_name(GtkButton* button, const char* icon_name)` are mapped to a static Java method `gtk_button_set_icon_name(MemoryAddress button, MemoryAddress icon_name)`. Using GObject-Introspection, it is possible to generate a wrapper API that includes "proxy" classes that call the jextract-generated functions with automatically marshalled parameters and return values, for example `button.setIconName(iconName)`. **java-gi** tries to achieve this.

## Frequently-asked questions

### Why use Java?

Java is a well-establised programming language that is widely used; it's free software, well-supported, and under active development. The OpenJDK JVM benefits from more than 20 years of enhancements and bugfixes, and the JIT compiler produces extremely fast results. The standard library is very complete, and the ecosystem is huge. Thanks to IDEs such as IntelliJ IDEA and Eclipse, developing and maintaining Java code is a breeze.

### Why not use existing bindings?

**java-gi** is unique because it offers Java bindings for GTK4 (and other GObject-Introspectable libraries), that are automatically generated from both the header files and GI data. Manual changes to bind specific APIs are only done when absolutely necessary. Existing Java bindings for GTK (like [java-gnome](http://java-gnome.sourceforge.net/)) were manually written, which means they need constant maintenance, especially for major GTK version upgrades. This is not feasible in the long term: Java-gnome has been mostly unmaintained for over a decade; it requires Java 6 to build, and only supports GTK 3. Another existing solution, [JGIR](https://wiki.gnome.org/Projects/JGIR), was last updated 12 years ago. Furthermore, these bindings require additional native libraries to work; a fully JVM-based API for accessing native resources is only possible with Panama.
