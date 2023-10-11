# Building and running the bindings generator

Java-GI publishes pre-built bindings for GLib, Gtk4, LibAdwaita, GtkSourceview, WebkitGtk and GStreamer. The bindings should work on Linux, Windows and MacOS. To generate and build the bindings for these libraries, follow these steps:

- Clone the Java-GI project (`jwharm/java-gi`) and the GIR input files (`gircore/gir-files`) from GitHub
- Run the Gradle build, either using an IDE, or navigate into the `java-gi` folder and run `./gradlew build` (on Windows: `gradlew build`).

```shell
git clone https://github.com/gircore/gir-files.git
git clone https://github.com/jwharm/java-gi.git
cd java-gi
./gradlew build
```

The gir-files repository contains regularly updated gir files that Java-GI generates bindings from. If you clone this repository in another location, update the `girFilesLocation` path in `gradle.properties` accordingly.

## Generating bindings for other libraries

First of all, install the GObject-introspection (gir) files of the library you want to generate bindings for. To do this, you need to download the release package (usually a `.tar.gz` file) of the library from their website and generate the introspection files using the included build script. Usually there is a build option to generate the gir file.

Alternatively, in most Linux distributions, the gir files are usually installed with a development package, postfixed with "-dev" or "-devel". For example, the Gtk gir files on Fedora are installed by the "gtk4-devel" package. All installed gir files can normally be found in `/usr/share/gir-1.0`. However, this is not the preferred method to obtain a gir file. Distributions often patch libraries, impacting the gir file too. It's better to generate the gir file from the upstream release package. Be sure to download the release package for an actual release, and not a random snapshot.

Once the gir file is available, copy it into the `gir-files` folder under the correct platform subfolder (linux, windows or macos).

Now you can add a module to Java-GI for the library. Simply copy one of the existing module folders to a new folder, add it to `settings.gradle`, and modify the `build.gradle` file to suit your needs. Also add all dependencies (look for `<include>` elements in gir file) to the `build.gradle` of the new module.

If you encounter any problems or errors, check the generated Java source code in the `build/generated/java-gi` directory for issues. When neccessary, you can patch the introspection data. To do this, create a patch class implementing `PatchSet` in the folder `buildSrc/main/java/io/github/jwharm/javagi/patches` and add a line `patch = new ...Patch()` to the `generateSources` configuration block in `build.gradle`. (See the [Gtk](https://github.com/jwharm/java-gi/blob/main/modules/gtk/build.gradle) buildfile for an example).
