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

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static io.github.jwharm.javagi.JavaGI.generate;

/**
 * GenerateSources is a Gradle task that will generate Java source files for
 * the types defined in a GIR Library. (The Library is provided by the
 * GirParserService.)
 */
public abstract class GenerateSources extends DefaultTask {

    @ServiceReference("gir")
    public abstract Property<GirParserService> getGirParserService();

    @Input
    public abstract Property<String> getNamespace();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    void execute() {
        try {
            var buildService = getGirParserService().get();
            var namespace = getNamespace().get();
            var library = buildService.getLibrary(namespace);
            var packages = getPackages();
            var outputDirectory = getOutputDirectory().get().getAsFile();
            generate(namespace, library, packages, outputDirectory);
        } catch (Exception e) {
            throw new TaskExecutionException(this, e);
        }
    }

    /*
     * Return a set of package names for all directories in the src/main/java
     * folder that contain at least one *.java file. These packages will be
     * exported in the module-info.java file.
     */
    private Set<String> getPackages() throws IOException {
        var packages = new HashSet<String>();
        var rootDir = getProject().getProjectDir().toPath();
        var srcDir = rootDir.resolve(Path.of("src", "main", "java"));
        var separator = srcDir.getFileSystem().getSeparator();

        if (! Files.exists(srcDir))
            return packages;

        Files.walkFileTree(
            srcDir,
            EnumSet.noneOf(FileVisitOption.class),
            Integer.MAX_VALUE,
            new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file,
                                                 BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        String pkg = srcDir
                                .relativize(file.getParent())
                                .toString()
                                .replace(separator, ".");
                        packages.add(pkg);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        return packages;
    }
}
