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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The LibLoad class is used by Java-GI to load native libraries by name.
 */
public class LibLoad {

    static {
        String javagiPath = System.getProperty("javagi.path");
        String javaPath = System.getProperty("java.library.path");
        if (javaPath != null) {
            if (javagiPath == null) javagiPath = javaPath;
            else javagiPath = javagiPath + File.pathSeparator + javaPath;
            System.setProperty("javagi.path", javagiPath);
        }
    }

    /**
     * Load the native library with the provided name.
     *
     * @param name the name of the library
     */
    public static void loadLibrary(String name) {
        InteropException fail = new InteropException("Could not load library " + name);

        // Try System::loadLibrary first
        try {
            System.loadLibrary(name);
            return;
        } catch (Throwable t) {
            fail.addSuppressed(t);
        }

        // Loop through all paths defined in javagi.path
        String[] libraryPaths = System.getProperty("javagi.path")
                .split(File.pathSeparator);

        for (String s : libraryPaths) {
            if (s.isBlank())
                continue;

            // Get a direct path to the directory
            Path pk = Path.of(s).toAbsolutePath().normalize();
            if (!Files.isDirectory(pk))
                continue;

            // List the files in the directory
            Path[] files;
            try (Stream<Path> p = Files.list(pk)) {
                files = p.toArray(Path[]::new);
            } catch (Throwable t) {
                fail.addSuppressed(t);
                continue;
            }

            Set<String> possibleNames = new HashSet<>();
            possibleNames.add(name);
            if (Platform.getRuntimePlatform().equals("windows") && name.startsWith("lib")) {
                possibleNames.add(name.substring(3));
            }
            // Find the file with the requested library name
            for (Path path : files) {
                try {
                    String fn = path.getFileName().toString();
                    if (possibleNames.contains(fn)) {
                        // Load the library
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
