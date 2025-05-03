/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 Jan-Willem Harmannij
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package io.github.jwharm.javagi.interop;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The LibLoad class is used by Java-GI to load native libraries by name.
 */
public class LibLoad {

    private static final Pattern separator = Pattern.compile(Pattern.quote(File.pathSeparator));
    private static final List<Path> sourceDirs;
    private static final Map<String, Set<String>> additionalDependencies;
    private static final boolean pathOverride;
    private static final Path tmp = Path.of(System.getProperty("java.io.tmpdir"))
            .resolve("io.github.jwharm.javagi.natives")
            .toAbsolutePath();

    static {
        String javagiPath = System.getProperty("javagi.path");
        String javaPath = System.getProperty("java.library.path");
        pathOverride = javagiPath != null;
        if (javaPath != null) {
            if (javagiPath == null) javagiPath = javaPath;
            else javagiPath = javagiPath + File.pathSeparator + javaPath;
            System.setProperty("javagi.path", javagiPath);
        }

        sourceDirs = separator.splitAsStream(javagiPath)
                .filter(s -> !s.isBlank())
                .map(s -> Path.of(s).toAbsolutePath().normalize())
                .filter(Files::isDirectory)
                .toList();

        additionalDependencies = sourceDirs.stream().map(s -> s.resolve("java-gi-meta-v1.txt"))
                .filter(Files::isRegularFile)
                .findFirst()
                .map(LibLoad::parseMetadata)
                .orElseGet(LibLoad::loadClasspathMetadata);
    }

    /**
     * Load the metadata file from the provided path.
     */
    private static Map<String, Set<String>> parseMetadata(Path path) {
        try (var reader = Files.newBufferedReader(path)) {
            return parseMetadata(reader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read metadata file: " + path, e);
        }
    }

    /**
     * Load the metadata file from the classpath. Returns an empty map if it doesn't exist.
     */
    private static Map<String, Set<String>> loadClasspathMetadata() {
        for (Module m : modules()) {
            try (InputStream in = m.getResourceAsStream("/io/github/jwharm/javagi/natives/java-gi-meta-v1.txt")) {
                if (in != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                        return parseMetadata(reader);
                    }
                }
            } catch (IOException e) {
                // This might be bad, but we can probably live with it
            }
        }
        return Map.of();
    }

    /**
     * Parse the metadata file and return a map of library names to their dependencies.
     */
    private static Map<String, Set<String>> parseMetadata(BufferedReader reader) throws IOException {
        Map<String, Set<String>> dependencyMap = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(":");
            if (parts.length == 2) {
                String dllName = parts[0].trim();
                String[] dependencies = parts[1].split(",");
                Set<String> dependencySet = new HashSet<>();
                for (String dep : dependencies) {
                    dependencySet.add(dep.trim());
                }
                dependencyMap.put(dllName, dependencySet);
                if (Platform.getRuntimePlatform() == Platform.WINDOWS) {
                    if (dllName.startsWith("lib")) dependencyMap.put(dllName.substring(3), dependencySet);
                    else dependencyMap.put("lib" + dllName, dependencySet);
                }
            }
        }
        return Map.copyOf(dependencyMap);
    }

    private static final Set<String> loadedLibraries = new HashSet<>();

    /**
     * Load the native library with the provided name.
     *
     * @param name the name of the library
     */
    public static void loadLibrary(String name) {
        if (loadedLibraries.contains(name)) return;

        Set<String> dependencies = additionalDependencies.get(name);
        if (dependencies != null) {
            for (String dependency : dependencies) {
                loadLibrary(dependency);
            }
        }

        InteropException fail = new InteropException("Could not load library " + name);

        // If javagi.path was not set, try System::loadLibrary first
        if (!pathOverride) {
            try {
                System.loadLibrary(name);
                loadedLibraries.add(name);
                return;
            } catch (Throwable t) {
                fail.addSuppressed(t);
            }
        }

        // Identify the possible names of the library
        Set<String> possibleNames = new HashSet<>();
        possibleNames.add(name);
        if (Platform.getRuntimePlatform() == Platform.WINDOWS) {
            if (name.startsWith("lib")) possibleNames.add(name.substring(3));
            else possibleNames.add("lib" + name);
        }

        // Loop through all paths defined in javagi.path
        for (Path pk : sourceDirs) {
            // List the files in the directory
            Path[] files;
            try (Stream<Path> p = Files.list(pk)) {
                files = p.toArray(Path[]::new);
            } catch (Throwable t) {
                fail.addSuppressed(t);
                continue;
            }

            // Find the file with the requested library name
            for (Path path : files) {
                try {
                    String fn = path.getFileName().toString();
                    if (possibleNames.contains(fn)) {
                        // Load the library
                        System.load(path.toString());
                        loadedLibraries.add(name);
                        return;
                    }
                } catch (Throwable t) {
                    fail.addSuppressed(t);
                }
            }
        }

        // If the library was not found, try to load it from the classpath
        for (Module m : modules()) {
            for (String n : possibleNames) {
                try (InputStream in = m.getResourceAsStream("/io/github/jwharm/javagi/natives/" + n)) {
                    if (in != null) {
                        Path tempFile = tmp.resolve(n);
                        Files.copy(in, tempFile);
                        System.load(tempFile.toString());
                        loadedLibraries.add(name);
                        return;
                    }
                } catch (IOException e) {
                    fail.addSuppressed(e);
                }
            }
        }

        // If javagi.path was set and the library was not found, also try System::loadLibrary
        if (pathOverride) {
            try {
                System.loadLibrary(name);
                loadedLibraries.add(name);
                return;
            } catch (Throwable t) {
                fail.addSuppressed(t);
            }
        }

        throw fail;
    }

    /**
     * Enumerate all modules that are loaded.
     * This is used to find native libraries that are bundled with the application.
     */
    private static Iterable<Module> modules() {
        return Stream.iterate(
                List.of(ModuleLayer.boot()),
                Objects::nonNull,
                s -> s.stream().flatMap(x -> x.parents().stream()).toList()
        )
                .flatMap(List::stream)
                .flatMap(s -> s.modules().stream())
                ::iterator;
    }
}
