# Example applications

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
tasks.withType(JavaExec) {
    jvmArgs += "--enable-preview"
    jvmArgs += "--enable-native-access=ALL-UNNAMED"
    jvmArgs += "-Djava.library.path=/usr/lib64:/lib64:/lib:/usr/lib:/lib/x86_64-linux-gnu"
}
```
