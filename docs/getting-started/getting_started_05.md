When constructing a more complicated user interface, with dozens or hundreds of widgets, doing all the setup work in Java code is cumbersome, and making changes becomes next to impossible.

Thankfully, GTK supports the separation of user interface layout from your business logic, by using UI descriptions in an XML format that can be parsed by the {{ javadoc('GtkBuilder') }} class.

### Packing buttons with GtkBuilder

Create a new file with the following content named `Example4.java`.

```java
import org.javagi.base.GErrorException;
import org.gnome.gio.ApplicationFlags;
import org.gnome.gtk.*;

public class Example4 {

  private static void printHello() {
    System.out.println("Hello World");
  }

  private static void activate(Application app) {
    // Construct a GtkBuilder instance and load our UI description
    GtkBuilder builder = new GtkBuilder();
    try {
      builder.addFromFile("src/main/resources/builder.ui");
    } catch (GErrorException ignored) {}

    // Connect signal handlers to the constructed widgets.
    Window window = (Window) builder.getObject("window");
    window.setApplication(app);

    Button button = (Button) builder.getObject("button1");
    button.onClicked(Example4::printHello);

    button = (Button) builder.getObject("button2");
    button.onClicked(Example4::printHello);

    button = (Button) builder.getObject("quit");
    button.onClicked(window::destroy);

    window.setVisible(true);
  }

  public static void main(String[] args) {
    Application app = new Application("org.gtk.example", ApplicationFlags.DEFAULT_FLAGS);
    app.onActivate(() -> activate(app));
    app.run(args);
  }
}
```

Create a new file with the following content named `builder.ui` and save it in the `src/main/resources` directory.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<interface>
  <object id="window" class="GtkWindow">
    <property name="title">Grid</property>
    <child>
      <object id="grid" class="GtkGrid">
        <child>
          <object id="button1" class="GtkButton">
            <property name="label">Button 1</property>
            <layout>
              <property name="column">0</property>
              <property name="row">0</property>
            </layout>
          </object>
        </child>
        <child>
          <object id="button2" class="GtkButton">
            <property name="label">Button 2</property>
            <layout>
              <property name="column">1</property>
              <property name="row">0</property>
            </layout>
          </object>
        </child>
        <child>
          <object id="quit" class="GtkButton">
            <property name="label">Quit</property>
            <layout>
              <property name="column">0</property>
              <property name="row">1</property>
              <property name="column-span">2</property>
            </layout>
          </object>
        </child>
      </object>
    </child>
  </object>
</interface>
```

Update the `mainClass` in `build.gradle` to `Example4` and test the program with `gradle run`.

Note that `GtkBuilder` can also be used to construct objects that are not widgets, such as tree models, adjustments, etc. That is the reason the method we use here is called {{ javadoc('GtkBuilder.getObject') }} and returns a `GObject` instead of a GTK `Widget`.

Normally, you would pass a full path to {{ javadoc('GtkBuilder.addFromFile') }} to make the execution of your program independent of the current directory. A common location to install UI descriptions and similar data is
`/usr/share/appname`.

It is also possible to embed the UI description in the source code as a string and use {{ javadoc('GtkBuilder.addFromString') }} to load it. But keeping the UI description in a separate file has several advantages:

- it is possible to make minor adjustments to the UI without recompiling your program
- it is easier to isolate the UI code from the business logic of your application
- it is easier to restructure your UI into separate classes using composite widget templates

Using [GResource](https://docs.gtk.org/gio/struct.Resource.html) you don't need to process the UI XML file at runtime: you can keep the UI definition files separate inside your source code repository, and then ship the compiled resource with your application. We will use GResource later in this guide.

[Previous](getting_started_04.md){ .md-button } [Next](getting_started_06.md){ .md-button }
