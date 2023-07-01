package io.github.jwharm.javagi.interop;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * The LibLoad class is used internally to load native libraries by name
 */
public class LibLoad {

    static {
        String javagiPath = System.getProperty("javagi.path");
        String javaPath = System.getProperty("java.library.path");
        if (javagiPath != null) {
            if (javaPath == null) {
                System.setProperty("java.library.path", javagiPath);
            } else {
                System.setProperty("java.library.path", javaPath + File.pathSeparator + javagiPath);
            }
        }
    }

    /**
     * Load the native library with the provided name
     * @param name the name of the library
     */
    public static void loadLibrary(String name) {
        InteropException fail = new InteropException("Could not load library " + name);
        try {
            System.loadLibrary(name);
            return;
        } catch (Throwable t) {
            fail.addSuppressed(t);
        }
        for (String s : System.getProperty("java.library.path").split(File.pathSeparator)) {
            if (s.isBlank()) {
                continue;
            }

            Path pk = Path.of(s).toAbsolutePath().normalize();
            if (!Files.isDirectory(pk)) {
                continue;
            }

            Path[] paths;
            try (Stream<Path> p = Files.list(pk)) {
                paths = p.toArray(Path[]::new);
            } catch (Throwable t) {
                fail.addSuppressed(t);
                continue;
            }

            for (Path path : paths) {
                try {
                    String fn = path.getFileName().toString();
                    if (fn.equals(name)) {
                        System.load(path.toString());
                        return;
                    }
                } catch (Throwable t) {
                    fail.addSuppressed(t);
                }
            }
        }
        throw fail;
    }
}
