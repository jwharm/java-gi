In order to use icons with GTK, we have two options : 

- Use icons bundled with GTK.
- Bundle SVG icons within our app resources.

### GTK bundled icons

To use GTK bundle icons, we just need to know the name of the icon we want to use. We can use the "GTK Icon Browser" app to see the icons installed on our system.

To install it, run :

=== "Ubuntu/Debian"

    ```bash
    apt-get install gtk-4-examples
    ```

=== "Fedora/RHEL"

    ```bash
    dnf install gtk4-devel-tools
    ```


We can then look up icons and call them by name in our code. For instance :

```Java
var icon = Image.fromGicon(Icon.newForString("avatar-default-symbolic"));
```

This is easy but there aren't a lot of icons to choose from

### Custom SVG Icons

#### Get an SVG icon Source

A good source of GTK-themed SVG icons is the [Icons Library app](https://flathub.org/apps/org.gnome.design.IconLibrary). The app offers a lot of icons and allows copying the SVG code.

Let's say we want to use the `penguin-symbolic` icon from the [Icons Library app](https://flathub.org/apps/org.gnome.design.IconLibrary). Let's save its SVG code in a file in our Java resource folder (i.e. `src/main/resources/icons`).

#### GTK resources compiler

First, we install the software to compile GTK application resources : 

=== "Ubuntu/Debian"

    ```bash
    apt-get install libglib2.0-dev-bin
    ```

=== "Fedora/RHEL"

    ```bash
    dnf install glib2-devel
    ```

#### GTK resources file

Then, we create the GTK XML resource file in our Java resources folder (i.e `src/main/resources`) ; let's call it `ourapp.gresource.xml` :

```xml
<?xml version="1.0" encoding="UTF-8"?>
<gresources>
    <gresource prefix="/icons/scalable/actions">
        <file alias="penguin-symbolic.svg" preprocess="xml-stripblanks">icons/penguin-symbolic.svg</file>
    </gresource>
</gresources>
```

The `.svg` extension is required to be able to add the icon to the applications icon theme.
Furthermore, the `/scalable/actions/` prefix is necessary for GTK to treat the icons as repaintable.
Otherwise, the icons won't be visible when the dark theme is enabled.

#### Gradle build script to compile GTK resources file

We now add a gradle task to our Gradle build script to compile the GTK resources file when we build our application.

=== "Gradle (Groovy)"

    ```Groovy
    tasks.register('compileResources') {
        exec {
            workingDir 'src/main/resources'
            commandLine 'glib-compile-resources', 'ourapp.gresource.xml'
        }
    }
    
    tasks.named('classes') {
        dependsOn compileResources
    }
    ```

=== "Gradle (Kotlin)"

    ```kotlin
    tasks.register("compileResources") {
        exec {
            workingDir("src/main/resources")
            commandLine("glib-compile-resources", "ourapp.gresource.xml")
        }
    }
    
    tasks.named("classes") {
        dependsOn("compileResources")
    }
    ```

#### Use the icon in our app

We can now use the icon using `Image.fromResource` and the string name of the icon, which is this `prefix` of the icon, concatenated with its `alias`:

```Java
var image = Image.fromResource("/icons/scalable/actions/icons/key-symbolic.svg");
```

##### Use the icon in a template

To be able to use the icon inside a template it has to be added to the applications icon theme.

```java
var theme = IconTheme.getForDisplay(Display.getDefault());
theme.addResourcePath("/icons");
```

This should be placed inside a `GtkApplication` `activate()` method because otherwise the display won't be available.
Now the icon can be used with its name.

```xml
<object class="AdwViewStackPage">
    <property name="icon-name">penguin-symbolic</property>
    ...
</object>
```

[Previous](getting_started_13.md){ .md-button }