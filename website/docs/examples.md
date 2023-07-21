# Examples applications

A set of example applications is available on the [java-gi-examples GitHub repository](https://github.com/jwharm/java-gi-examples). To run an example, install Java 20 and Gradle, clone the Git repository:

```
git clone https://github.com/jwharm/java-gi-examples
```

Move into one of the example folders and run `gradle run`:

```
cd HelloWorld
gradle run
```

If you see an error about a missing library, make sure that all dependencies are installed, and available on Java library path (the `"java.library.path"` system property). If necessary, you can override the Java library path with the `-Djava.library.path=` JVM argument in the `gradle.build` file, in the `tasks.withType(JavaExec) {` block. For example:

```
jvmArgs += "-Djava.library.path=/usr/java/packages/lib:/usr/lib64:/lib64:/lib:/usr/lib:/lib/x86_64-linux-gnu"
```

### Hello World

A typical ["Hello World" example](https://github.com/jwharm/java-gi-examples/tree/main/HelloWorld) that displays a Gtk window with a button. When you click the button, the application quits.

![Hello World screenshot](https://github.com/jwharm/java-gi-examples/blob/main/images/simple-helloworld.png)

### Hello World - template based

This is a bit more complete ["Hello World" example](https://github.com/jwharm/java-gi-examples/tree/main/HelloTemplate) that is based on the default application that GNOME Builder generates for new Vala projects. It uses Gtk composite template classes to define the user interface in XML files.

![Hello World (template based) screenshot](https://github.com/jwharm/java-gi-examples/blob/main/images/template-helloworld.png)

### GStreamer sound player example

This example [demonstrates how to use GStreamer](https://github.com/jwharm/java-gi-examples/tree/main/PlaySound) and is ported from the GStreamer tutorials. It creates a GStreamer pipeline that will play sound from an Ogg Vorbis file.

Installation of the GStreamer libraries is described [here](https://gstreamer.freedesktop.org/documentation/installing/on-linux.html?gi-language=c) on the GStreamer website.

### Calculator example

A [basic calculator](https://github.com/jwharm/java-gi-examples/tree/main/Calculator) that uses Gtk and LibAdwaita. There's a header bar, and a grid-based layout for the buttons. The app reacts to key presses as expected.

![Calculator screenshot](https://github.com/jwharm/java-gi-examples/blob/main/images/calculator.png)

### List integration example

This example [demonstrates](https://github.com/jwharm/java-gi-examples/tree/main/ListViewer) how you can use a Java ArrayList to implement the GListModel interface, which is central to all modern Gtk list widgets.

| ![ListViewer screenshot](https://github.com/jwharm/java-gi-examples/blob/main/images/listviewer.png)

### Notepad example

A very basic Adwaita [plain-text editor](https://github.com/jwharm/java-gi-examples/tree/main/Notepad), that can load and save files using GIO.

![Notepad screenshot](https://github.com/jwharm/java-gi-examples/blob/main/images/notepad.png)

### Code editor example

A [source code editor](https://github.com/jwharm/java-gi-examples/tree/main/CodeEditor). It is mostly the same application as the Notepad example above, but this one uses GtkSourceview as the text widget and enables line numbers and syntax highlighting.

![Code Editor screenshot](https://github.com/jwharm/java-gi-examples/blob/main/images/codeeditor.png)

### Web browser example

This example creates a very basic [web browser](https://github.com/jwharm/java-gi-examples/tree/main/Browser) using WebkitGtk. It was inspired by the browser example in GNOME Workbench.

![Web Browser screenshot](https://github.com/jwharm/java-gi-examples/blob/main/images/browser.png)

### Mediastream example

The [Mediastream example](https://github.com/jwharm/java-gi-examples/tree/main/MediaStream) is ported from GtkDemo. It paints a simple image using Cairo, and then rotates the image in a GtkVideo widget. The Cairo draw commands use the [Cairo Java bindings](https://github.com/jwharm/cairo-java-bindings).

![Media Stream screenshot](https://github.com/jwharm/java-gi-examples/blob/main/images/mediastream.png)
