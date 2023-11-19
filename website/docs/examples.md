# Example applications

A set of example applications is available on the [java-gi-examples GitHub repository](https://github.com/jwharm/java-gi-examples). To run an example, install Java 21 and Gradle 8.3 (or newer), and clone the Git repository:

```
git clone https://github.com/jwharm/java-gi-examples
```

Move into one of the example folders and run `gradle run`:

```
cd HelloWorld
gradle run
```

If you see an error about a missing library, make sure that all dependencies are installed, and available on Java library path (the `"java.library.path"` system property). If necessary, you can override the Java library path with the `-Djava.library.path=` JVM argument in the `gradle.build` file. By default, it contains the system library folders of the common (RedHat/Fedora, Arch and Debian/Ubuntu) Linux distributions. Read [here](usage.md) for instructions about other distributions and operating systems.
