# java-gi

**java-gi** is a very experimental, **work-in-progress** tool for generating GObject-Introspection bindings for Java. The generated bindings use the Panama foreign function & memory access API (JEP 424) and `jextract` for accessing native resources, and add wrapper classes based on GObject-Introspection to offer an easy API to use with Java. The included build script generates bindings for GTK4.

## Prerequisites

- Download and extract an [Early Access build of Java 19](https://jdk.java.net/panama/)
- Set JAVA_HOME path to point to your Java 19 folder, for example: `JAVA_HOME=/opt/jdk-19`
- Install GTK-development header files and GObject-introspection (gir) files.
  - Fedora: `sudo dnf install gtk4-devel glib-devel gobject-introspection-devel`

## How to create bindings

Use the included shell scripts to create bindings for GTK4:
- First run `java-gi.sh` to generate a jar with jextract classes for GTK4. The script uses `pkg-config` to set the correct include and library paths, and a file `gtk.h` which contains the necessary includes (`<gtk/gtk.h>` on its own is not sufficient).
- Next, run `java-gi-gtk4.sh` to parse gir files (from `/usr/share/gir-1.0`) and generate the bindings (java source files) for GTK4 and its dependencies.
- At the moment, you still need to compile the generated sources and create a jar file by yourself.
- The script `clean.sh` removes the generated bindings from `java-gi-gtk4.sh`.

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

### Why should I use java-gi bindings?

**java-gi** is unique because it offers Java bindings for GTK4 (and other GObject-Introspectable libraries), that are automatically generated from both the header files and GI data. Manual changes to bind specific APIs are only done when absolutely necessary. This means the bindings will be easy to maintain in the future. Furthermore, the java-gi bindings don't require any native "glue" libraries to work; thanks to Panama, you only need the jar file.
