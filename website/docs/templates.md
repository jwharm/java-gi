# Gtk Composite template classes

A class with a `@GtkTemplate` annotation will be registered as a Gtk composite template class. You need to specify the UI file and the name that your class is referred to from the UI file. The path to the UI file is a GResource path.

## Example

```java
@GtkTemplate(name="HelloWindow", ui="/my/example/hello-window.ui")
public class HelloWindow extends ApplicationWindow {

    private static final Type gtype = Types.register(HelloWindow.class);

    @GtkChild
    public HeaderBar header_bar;

    @GtkChild
    public Label label;
    
    @GtkCallback
    public void buttonClicked() {
        ...
    }

    ...
```

In the above class, the `header_bar` and `label` fields and the `buttonClicked` callback function are all declared the UI file.

Because the registration of composite template classes uses reflection, you must add the following line to your `module-info.java` file:

```
exports [package name] to org.gnome.glib,org.gnome.gtk;
```

A complete example template-application can be found [here](https://github.com/jwharm/java-gi-examples/tree/main/HelloTemplate).

## Annotations

Composite template classes in Java-GI use three annotations:

* `@GtkTemplate` is a class-annotation that marks the class as a composite template class. Here the name of the GType and the path to the template is specified.

* `@GtkChild` is a field-annotation that is used for Gtk widgets that are defined in the template file, and need to be used from Java. Java-GI will generate a GTypeClass struct with pointers to all GtkChild fields, and "connect" the fields to the definitions in the template file.

* `@GtkCallback` is a method-annotation to mark callback-functions that are used from inside the template file, for example in a `<signal>` element.

All annotations have an optional `name` attribute to manually override the name of the class, field, or callback method.

The `@GtkCallback` annotation is mostly useful for overriding the method's name. When you name the method exactly the same as it is specified in the signal handler in the UI template, you can safely omit the `@GtkCallback` annotation. The signal connection will still work as expected.

## Compiling the UI template file to a resource bundle

The path to the UI file is treated by Java-GI as a [GResource](https://docs.gtk.org/gio/struct.Resource.html) identifier. Template UI files (and other resources) are compiled in a GResource bundle with the command-line tool `glib-compile-resources`. This program should be included in the GTK development packages for your operating system (for example, on Ubuntu, install `libglib2.0-dev`).

Load the compiled resource bundle in Java during startup of your application:

```
import io.github.jwharm.javagi.base.GErrorException;
import org.gnome.gio.Resource;

...

    public static void main(String[] args) throws GErrorException {
    
        ...
        
        Resource resource = Resource.load("path/to/the/compiled/resource/bundle");
        resource.resourcesRegister();
    
        ...
```
