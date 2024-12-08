# Basic usage

## Required Java version

To use Java-GI, download and install [JDK 22](https://jdk.java.net/22/) or newer. Java-GI uses the "Panama" Foreign Function & Memory API that is only available since JDK 22.

## Dependencies

Make sure that the native GLib, Gtk and/or GStreamer libraries are installed on your operating system.

- If you use Linux, Gtk is often installed by default. On Windows or MacOS, follow the [installation instructions](https://www.gtk.org/docs/installations/).

- GStreamer: Follow the [installation instructions](https://gstreamer.freedesktop.org/documentation/installing/).

Next, add the dependencies. For example, to add Gtk as a dependency:

=== "Maven"

    ```xml
    <dependency>
      <groupId>io.github.jwharm.javagi</groupId>
      <artifactId>gtk</artifactId>
      <version>0.11.1</version>
    </dependency>
    ```

=== "Gradle (Groovy)"

    ```groovy
    repositories {
        mavenCentral()
    }

    dependencies {
        implementation 'io.github.jwharm.javagi:gtk:0.11.1'
    }
    ```

=== "Gradle (Kotlin)"

    ```groovy
    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("io.github.jwharm.javagi:gtk:0.11.1")
    }
    ```

=== "Scala SBT"

    ```scala
    libraryDependencies += "io.github.jwharm.javagi" % "gtk" % "0.11.1"
    ```

=== "Leiningen"

    ```clojure
    [io.github.jwharm.javagi/gtk "0.11.1"]
    ```

=== "bld"

    ```java
    repositories = List.of(MAVEN_CENTRAL);
    scope(main)
        .include(dependency("io.github.jwharm.javagi",
                            "gtk",
                            version(0,11,1)));
    ```

This will add the Gtk bindings to the application's compile and runtime classpath. Other libraries, like `webkit`, `gst`, `adw` and `gtksourceview` can be included likewise. The complete list of available libraries is available [here](https://search.maven.org/search?q=io.github.jwharm.javagi).

## Application code

An example Gtk application with a "Hello world" button can be created as follows:

=== "Java"

    ```java
    package my.example.helloapp;

    import org.gnome.gtk.*;
    import org.gnome.gio.ApplicationFlags;

    public class HelloWorld {

        public static void main(String[] args) {
            new HelloWorld(args);
        }
        
        public HelloWorld(String[] args) {
            var app = new Application("my.example.HelloApp", ApplicationFlags.DEFAULT_FLAGS);
            app.onActivate(() -> activate(app));
            app.run(args);
        }
        
        public void activate(Application app) {
            var window = new ApplicationWindow(app);
            window.setTitle("GTK from Java");
            window.setDefaultSize(300, 200);
            
            var box = Box.builder()
                .setOrientation(Orientation.VERTICAL)
                .setHalign(Align.CENTER)
                .setValign(Align.CENTER)
                .build();
            
            var button = Button.withLabel("Hello world!");
            button.onClicked(window::close);
            
            box.append(button);
            window.setChild(box);
            window.present();
        }
    }
    ```

=== "Kotlin"

    ```kotlin
    package my.example.helloapp

    import org.gnome.gio.ApplicationFlags
    import org.gnome.gtk.*

    fun main(args: Array<String>) {
        val app = Application("my.example.HelloApp", ApplicationFlags.DEFAULT_FLAGS)
        app.onActivate { activate(app) }
        app.run(args)
    }

    private fun activate(app: Application) {
        val window = ApplicationWindow(app)
        window.title = "GTK from Kotlin"
        window.setDefaultSize(300, 200)

        val box = Box.builder()
            .setOrientation(Orientation.VERTICAL)
            .setHalign(Align.CENTER)
            .setValign(Align.CENTER)
            .build()

        val button = Button.withLabel("Hello world!")
        button.onClicked(window::close)

        box.append(button)
        window.child = box
        window.present()
    }
    ```

=== "Scala"

    ```scala
    package my.example.helloapp
     
    import org.gnome.gtk.*
    import org.gnome.gio.ApplicationFlags
     
    class HelloWorld {
        def activate(app: Application) = {
            var window = new ApplicationWindow(app)
            window.setTitle("GTK from Scala")

            var box = new Box(Orientation.VERTICAL, 1) {
                setHalign(Align.CENTER)
                setValign(Align.CENTER)
            }

            var button = Button.withLabel("Hello world!")
            button.onClicked{() => window.close}

            box.append(button)
            window.setChild(box)
            window.present()
        }
    }

    object HelloWorld {
        def main(args: Array[String]) = {
            val app = Application("my.example.HelloApp", ApplicationFlags.DEFAULT_FLAGS)
            app.onActivate(() => HelloWorld().activate(app))
            app.run(args);
            ()
        }
    }
    ```

## Compile and run

Build and run the application using your IDE or build tool of choice. The following command-line parameters are useful:

- Add `--enable-native-access=ALL-UNNAMED` to suppress warnings about native access.

- If you encounter an error about a missing library, override the java library path with `"-Djava.library.path=/usr/lib/..."`.

See [this `build.gradle` file](https://github.com/jwharm/java-gi-examples/blob/main/HelloWorld/build.gradle) for a complete example.

!!! tip "Use an IDE"
    An Integrated Development Environment (IDE) with support for Java is the most efficient way to develop software in Java. IDEs will help setup a project and build configuration, navigate and refactor source code, detect problems and suggest improvements. The most commonly used Java IDEs are:
    
    * [JetBrains IntelliJ IDEA](https://www.jetbrains.com/idea/)
    * [Eclipse IDE](https://eclipseide.org/)
    * [Apache NetBeans](https://netbeans.apache.org/)
    * [Visual Studio Code with a Java extension](https://code.visualstudio.com/docs/languages/java)
    
    If you often work from the command line, [SDKMAN!](https://sdkman.io/) will proove useful to manage your installed SDKs and build tools.

## Further reading

For more advanced instructions on using Java-GI consult [this page](advanced.md), and read about subclassing GObject classes with Java-GI [here](register.md). If you're new to Gtk development, read the [Gtk "Getting started" guide](getting-started/getting_started_00.md) that has been translated to use Java for all code examples. 

## Platform-specific notes

### Linux

On most Linux distributions, Gtk will already be installed. Java-GI will load shared libraries using `dlopen`, and fallback to the `java.library.path`. So in most cases, you can simply run your application with `--enable-native-access=ALL-UNNAMED`:

=== "Gradle (Groovy)"

    ```groovy
    application {
        applicationDefaultJvmArgs = ['--enable-native-access=ALL-UNNAMED']
        mainClass = ...
    }
    ```

=== "Gradle (Kotlin)"

    ```kotlin
    application {
        applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
        mainClass = ...
    }
    ```

### MacOS

On MacOS, you can install Gtk using Homebrew. Gtk needs to run on the main thread, therefore you need to set the parameter `-XstartOnFirstThread`. A complete Gradle `run` task will look like this:

=== "Gradle (Groovy)"

    ```groovy
    application {
        applicationDefaultJvmArgs = [
            '--enable-native-access=ALL-UNNAMED',
            '-Djava.library.path=/opt/homebrew/lib',
            '-XstartOnFirstThread'
        ]
        mainClass = ...
    }
    ```

=== "Gradle (Kotlin)"

    ```kotlin
    application {
        applicationDefaultJvmArgs = listOf(
            "--enable-native-access=ALL-UNNAMED",
            "-Djava.library.path=/opt/homebrew/lib",
            "-XstartOnFirstThread"
        )
        mainClass = ...
    }
    ```

### Windows

On Windows, Gtk can be installed with MSYS2. A Gradle `run` task will look like this:

=== "Gradle (Groovy)"

    ```groovy
    application {
        applicationDefaultJvmArgs = [
            '--enable-native-access=ALL-UNNAMED',
            '-Djava.library.path=C:/msys64/mingw64/bin',
        ]
        mainClass = ...
    }
    ```

=== "Gradle (Kotlin)"

    ```kotlin
    application {
        applicationDefaultJvmArgs = listOf(
            "--enable-native-access=ALL-UNNAMED",
            "-Djava.library.path=C:/msys64/mingw64/bin",
        )
        mainClass = ...
    }
    ```
