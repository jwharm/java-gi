In order to use icons with GTK, we have two options : 

- Use icons bundled with GTK.
- Bundle SVG icons within our app resources.

### GTK bundled icons

To use GTK bundle icons, we just need to know the name of the icon we want to use. We can use the "GTK Icon Browser" app to see the icons installed on our system.

To install it, run :

=== "Ubuntu/Debian"

    ```bash
    apt-get install gtk-3-examples
    ```

=== "Fedora/RHEL"

    ```bash
    dnf install gtk3-devel
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
    dnf install glib2-devel-2.82.2-1.fc41.x86_64
    ```

#### GTK resources file

Then, we create the GTK XML resource file in our Java resources folder (i.e `src/main/resources`) ; let's call it `ouapp.gresource.xml` :

```xml
<?xml version="1.0" encoding="UTF-8"?>
<gresources>
    <gresource prefix="/icons">
        <file alias="penguin-symbolic" preprocess="xml-stripblanks">icons/penguin-symbolic.svg</file>
    </gresource>
</gresources>
```

#### Gradle build script to compile GTK resources file

We now add a gradle task to ou Gradle build script to compile the GTK resources file when we build our application.

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
var image = Image.fromResource("/icons/key-symbolic");
```