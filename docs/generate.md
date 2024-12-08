# Generating bindings from a GIR file

Download the `java-gi` command-line utility from the [Releases section](https://github.com/jwharm/java-gi/releases) on GitHub. It is attached as an "asset" below the release notes. Extract the zip file. You will find the `java-gi` utility in the `bin` folder. Try running it:

```
> java-gi --help
Usage: java-gi [-hpV] [-d=domain] [-o=dir] [-s=text] [-u=url] <girFiles>...
Generate Java bindings from GObject-Introspection repository (gir) files.
      <girFiles>...     one or more gir files to process
  -d, --domain=domain   reverse domain name prefixed to the Java package and
                          module name, for example "org.gnome"
  -h, --help            Show this help message and exit.
  -o, --output=dir      output directory, default: current working directory
  -p, --project         generate Gradle project structure and build scripts
  -s, --summary=text    short summary of the library to include in the javadoc
                          of the generated Java package
  -u, --doc-url=url     url of the online API documentation to prefix before
                          hyperlinks in the generated javadoc
  -V, --version         Print version information and exit.
> _
```

With the `java-gi` tool ready to use, find the GObject-introspection (gir) file of the library you want to generate bindings for. To do this, you can download the release package (usually a `.tar.gz` file) of the library from their website and generate the introspection files using the included build script. Usually there is a build option to generate the gir file.

Alternatively, in most Linux distributions, the gir files are usually installed with a development package, postfixed with "-dev" or "-devel". For example, the Gtk gir files on Fedora are installed by the "gtk4-devel" package. All installed gir files can normally be found in `/usr/share/gir-1.0`. However, this is not the preferred method to obtain a gir file. Distributions often patch libraries, impacting the gir file too. It's better to generate the gir file from the upstream release package. Be sure to download the release package for an actual release, and not a random snapshot.

Once the gir file is available, run `java-gi <filename.gir>` to generate the Java bindings.

If you encounter any problems or errors, in most cases it should be possible to manually fix the generated Java source code. If you think the problem is caused by a bug in Java-GI, please [log an issue in the GitHub repo](https://github.com/jwharm/java-gi/issues).

The generated sources code can be included in your application, or you can build a separate library. Use the `-p` or `--project` switch to generate a Gradle build script and directory structure that will be ready to build and deploy.

To change the package name for the generated sources, use the `-d` option. The `-s` option adds a summary to the package-info.java file. The `-u` option adds a URL prefix to image URLs in the API documentation, to make sure the image also appears in the generated Javadoc.

It is possible to generate bindings for multiple gir files at once. The `java-gi` tool will generate them in order, so if `x.gir` depends on `y.gir`, run `java-gi y.gir x.gir`, to first process `x.gir`, use the results to process `y.gir`, and write Java sources for both libraries.

# Building the included bindings

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

