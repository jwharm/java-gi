package io.github.jwharm.javagi;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class LibLoad {
    private static final boolean OS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    public static void loadLibrary(String name) {
        RuntimeException fail = new RuntimeException("Could not load library");
        try {
            System.loadLibrary(name);
            return;
        } catch (Throwable t) {
            fail.addSuppressed(t);
        }
        for (String s : System.getProperty("java.library.path").split(File.pathSeparator)) {
            if (s.isBlank()) continue;
            Path pk = Path.of(s).toAbsolutePath().normalize();
            if (!Files.isDirectory(pk)) continue;
            Path[] paths;
            try (Stream<Path> p = Files.list(pk)) {
                paths = p.toArray(Path[]::new);
            } catch (Throwable t) {
                fail.addSuppressed(t);
                continue;
            }
            String libName = System.mapLibraryName(name);
            for (Path path : paths) {
                try {
                    String fn = path.getFileName().toString();
                    if (fn.equals(libName)) {
                        System.load(path.toString());
                        return;
                    }
                    if (OS_WINDOWS && fn.startsWith("lib" + name + "-") && fn.endsWith(".dll")) {
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
