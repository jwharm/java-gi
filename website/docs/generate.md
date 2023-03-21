# Generating bindings

Java-GI publishes pre-built bindings for the entire GTK4 library stack, LibAdwaita and GStreamer for Linux, Windows and MacOS. If you want to generate bindings for other libraries, platforms or versions, follow these steps:

* First, download and install [JDK 19](https://jdk.java.net/19/) and [Gradle](https://gradle.org/) version 8.0 or newer.

* Install the GObject-introspection (gir) files of the library you want to generate bindings for. 
  The gir files are usually installed with a development package, postfixed with "-dev" or "-devel". For example, the Gtk gir files on Fedora are installed by the "gtk4-devel" package. All installed gir files can normally be found in `/usr/share/gir-1.0`.

* Copy the one of the existing module folders (preferrably adwaita or gstreamer) to a new folder, and change the gir file list in `build.gradle.kts` to the gir files that you want to generate bindings for.

* Run `./gradlew build` to generate the bindings.

* The resulting jar files are located in the `[module]/build/libs` folders.

If you encounter errors about an unsupported class file version, make sure that the right version of Gradle is installed.
