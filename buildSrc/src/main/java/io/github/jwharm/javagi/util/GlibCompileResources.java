/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2023 Jan-Willem Harmannij
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

package io.github.jwharm.javagi.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class that runs {@code glib-compile-resources} on all {@code .gresources.xml}
 * files in a specific directory.
 */
public class GlibCompileResources {

    private final File workDirectory;

    public GlibCompileResources(File workDirectory) {
        this.workDirectory = workDirectory;
    }

    private List<String> getCommand() throws FileNotFoundException {
        List<String> command = new ArrayList<>();
        command.add("glib-compile-resources");
        File[] files = workDirectory.listFiles((dir, name) -> name.endsWith(".gresource.xml"));
        if (files == null) {
            throw new FileNotFoundException("No .gresource.xml files found");
        }
        Arrays.stream(files).map(File::getAbsolutePath).forEach(command::add);
        return command;
    }

    public void execute() throws Exception {
        int exitCode = new ProcessBuilder()
            .inheritIO()
            .directory(workDirectory)
            .command(getCommand())
            .start()
            .waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("GResource compilation failed.");
        }
        System.out.println("GResource compilation completed successfully.");
    }
}
