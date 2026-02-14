# Example applications

A set of example applications is available on the [java-gi-examples GitHub repository](https://github.com/jwharm/java-gi-examples). To run an example, install Java (OpenJDK 22 or any later version) and Gradle (8.3 or newer), and clone the Git repository:

```
git clone https://github.com/jwharm/java-gi-examples
```

Move into one of the example folders and run `gradle run`:

```
cd HelloWorld
gradle run
```

If you see an error about a missing library, make sure that all dependencies are installed. If necessary, you can override the Java library path with the `-Djava.library.path=` JVM argument in the `gradle.build` file. Read [here](usage.md) for more detailed instructions about running Java-GI on different operating systems.
