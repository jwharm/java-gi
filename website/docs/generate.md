# Building and running the bindings generator

Java-GI publishes pre-built bindings for GLib, Gtk4, LibAdwaita, GtkSourceview, WebkitGtk and GStreamer. The bindings should work on Linux, Windows and MacOS. If you want to generate bindings for other libraries, platforms or versions, follow these steps:

- Clone the Java-GI project from GitHub.
- Run `./bld all download publish`.

Java-GI uses `bld` (from [Rife2](https://rife2.com/bld)) to build. `bld` is a pure java build tool that is very easy to use. You don't need to install it separately; the included wrapper will download it the first time.

During the build process, the [gircore/gir-files](https://github.com/gircore/gir-files) repository is cloned into the `build/gir-files` folder. The gir-core repository contains regularly updated gir files for GLib, Gtk, GStreamer, LibAdwaita and some other commonly used libraries for Linux, Windows and MacOS. Once downloaded, subsequent builds will run `git pull` instead of `git clone`.

The resulting jar files are located in the `build/dist` folder, and also published in the MavenLocal repository.

If you want to build a specific module, run `./bld [modulename] [command]`. Running just `./bld` will display a list of all options. Example usage:

| Command                                 | Result                                                          |
|-----------------------------------------|-----------------------------------------------------------------|
| `./bld`                                 | Display all available commands                                  |
| `./bld clean`                           | Clean the build artifacts                                       |
| `./bld download`                        | Download dependencies                                           |
| `./bld glib gtk compile`                | Generate and compile bindings for GLib and Gtk                  |
| `./bld all compile`                     | Shorthand to compile bindings for all modules                   |
| `./bld gtk jar jar-javadoc jar-sources` | Create jar, javadoc.jar and sources.jar for Gtk                 |
| `./bld glib gstreamer publish`          | Publish jar, javadoc.jar and sources.jar for GLib and GStreamer |

## Generating bindings for other libraries

First of all, install the GObject-introspection (gir) files of the library you want to generate bindings for. To do this, you need to download the release package (usually a `.tar.gz` file) of the library from their website and generate the introspection files using the included build script. Usually there is a build option to generate the gir file.

Alternatively, in most Linux distributions, the gir files are usually installed with a development package, postfixed with "-dev" or "-devel". For example, the Gtk gir files on Fedora are installed by the "gtk4-devel" package. All installed gir files can normally be found in `/usr/share/gir-1.0`. However, this is not the preferred method to obtain a gir file. Distributions often patch libraries, impacting the gir file too. It's better to generate the gir file from the upstream release package. Be sure to download the release package for an actual release, and not a random snapshot.

Once the gir file is available, you can add a module to Java-GI for it (replace "xxx" with the library name).

- Add a new `XxxBuild` class in the `src/bld/java/io/github/jwharm/javagi/modules` folder (you can use the existing classes as examples). Change the input- and output-paths to the correct locations.

- Add a build command for your library to the `JavaGIBuild` class (located in the parent folder). This is a simple method, annotated with `@BuildCommand`, that adds your `XxxBuild` class to the list of modules that will be built.

Run `./bld xxx publish` to generate bindings, compile the classes, create jar, javadoc-jar and sources-jar artifacts, and publish them in maven-local. (They can also be found in the `build/dist` directory.)

If you encounter any problems or errors, check the generated Java source code in the `build/generated` directory for issues. When neccessary, you can patch the introspection data. To do this, create a `XxxPatch` class implementing `PatchSet` in the folder `src/bld/java/io/github/jwharm/javagi/patches` and add it to the `source()` line in the module build class. You can use the other patches as examples.
