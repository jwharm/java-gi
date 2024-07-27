# Building and running the bindings generator

Java-GI publishes pre-built bindings for a number of libraries, including GLib, Gtk4, LibAdwaita, GtkSourceview, WebkitGtk and GStreamer. The bindings should work on Linux, Windows and MacOS. To generate and build the bindings for these libraries, follow these steps:

- Clone the Java-GI project (`jwharm/java-gi`) from GitHub. Use the `--recurse-submodules` flag to also clone the `gir-files` submodule
- Run the Gradle build, either using an IDE, or navigate into the `java-gi` folder and run `./gradlew build` (on Windows: `gradlew build`).

=== "Linux & macOS"

    ```
    git clone --recurse-submodules https://github.com/jwharm/java-gi.git
    cd java-gi
    ./gradlew build
    ```

=== "Windows"

    ```
    git clone --recurse-submodules https://github.com/jwharm/java-gi.git
    cd java-gi
    gradlew build
    ```

The repository contains a Git submodule under `ext/gir-files` that originates from `gir-core/gir-files`. It contains regularly updated gir files for Linux, Windows and MacOS that Java-GI generates bindings from. If you clone this repository in another location, update the `girFilesLocation` path in `gradle.properties` accordingly.

## Generating bindings for other libraries

First of all, install the GObject-introspection (gir) files of the library you want to generate bindings for. To do this, you need to download the release package (usually a `.tar.gz` file) of the library from their website and generate the introspection files using the included build script. Usually there is a build option to generate the gir file.

Alternatively, in most Linux distributions, the gir files are usually installed with a development package, postfixed with "-dev" or "-devel". For example, the Gtk gir files on Fedora are installed by the "gtk4-devel" package. All installed gir files can normally be found in `/usr/share/gir-1.0`. However, this is not the preferred method to obtain a gir file. Distributions often patch libraries, impacting the gir file too. It's better to generate the gir file from the upstream release package. Be sure to download the release package for an actual release, and not a random snapshot.

Once the gir file is available, copy it into the `ext/gir-files` repository under the correct platform subfolder (linux, windows or macos).

Now you can add a Java-GI module for the library. Create a folder under `/modules`, add it to `settings.gradle`, and create a `build.gradle` file:

```groovy
plugins {
    id 'java-gi.library-conventions'
}

dependencies {
    // Add dependencies on other modules here.
    // The dependencies can be found in the GIR file (search for <include> tags).
    api project(':gio')
    api project(':gtk')
}

generateSources.configure {
    namespace = 'Insert-Namespace-Here'
}
```

Configure the Java package name, description and an URL prefix for image links in the `ModuleInfo` class in the package `io.github.jwharm.javagi.configuration` in the `BuildSrc/src/main/java` directory.

When necessary, you can patch the introspection data. To do this, create a patch class implementing `Patch` in the folder `buildSrc/main/java/io/github/jwharm/javagi/patches` and add it to the list in the `Patches` class.

If you encounter any problems or errors, check the generated Java source code in the `java-gi/modules/{modulename}/build/generated/sources/java-gi` directory for issues. Remember that other libraries are not guaranteed to work flawlessly as soon as they are added. If you have patched the introspection data and are still getting errors, Java GI may have a bug. In that case, please [log an issue in the GitHub repo](https://github.com/jwharm/java-gi/issues).
