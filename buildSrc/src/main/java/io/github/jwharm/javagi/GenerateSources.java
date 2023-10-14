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

package io.github.jwharm.javagi;

import io.github.jwharm.javagi.generator.*;
import io.github.jwharm.javagi.model.Repository;
import io.github.jwharm.javagi.model.Module;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.Optional;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * GenerateSources is a Gradle task that will parse a GIR file (and all included GIR files)
 * and generate Java source files for the types defined in the GIR file.
 */
public abstract class GenerateSources extends DefaultTask {

    @InputDirectory
    abstract DirectoryProperty getInputDirectory();

    @OutputDirectory
    abstract DirectoryProperty getOutputDirectory();

    @Input
    abstract Property<String> getGirFile();

    @Input @Optional
    abstract Property<String> getUrlPrefix();

    @Input @Optional
    abstract Property<Patch> getPatch();

    @TaskAction
    void execute() {
        try {
            Module linux = parse(Platform.LINUX, getInputDirectory().get(), getGirFile().get(),
                    getUrlPrefix().getOrElse(null), getPatch().getOrElse(null));
            Module windows = parse(Platform.WINDOWS, getInputDirectory().get(), getGirFile().get(),
                    getUrlPrefix().getOrElse(null), getPatch().getOrElse(null));
            Module macos = parse(Platform.MACOS, getInputDirectory().get(), getGirFile().get(),
                    getUrlPrefix().getOrElse(null), getPatch().getOrElse(null));

            Module module = new Merge().merge(linux, windows, macos);

            for (Repository repository : module.repositories.values()) {
                if (repository.generate) {
                    Path basePath = getOutputDirectory().get().file(repository.namespace.pathName).getAsFile().toPath();
                    repository.generate(basePath);
                    repository.generateModuleInfo(getOutputDirectory().get().getAsFile().toPath(), getPackages());
                }
            }
        } catch (Exception e) {
            throw new TaskExecutionException(this, e);
        }
    }

    private static Module parse(Platform platform, Directory sourceDirectory, String girFile, String urlPrefix, Patch patch)
            throws SAXException, ParserConfigurationException {
        Module module = new Module(platform);
        Directory girPath = sourceDirectory.dir(platform.name().toLowerCase());
        if (! girPath.getAsFile().exists()) {
            System.out.println("Not found: " + girPath);
            return null;
        }
        GirParser parser = new GirParser(girPath.getAsFile().toPath(), module);

        // Parse the GI files into Repository objects
        try {
            // Parse the file
            Repository r = parser.parse(girFile);

            // Check if this one has already been parsed
            if (module.repositories.containsKey(r.namespace.name)) {
                r = module.repositories.get(r.namespace.name);
            } else {
                // Add the repository to the module
                module.repositories.put(r.namespace.name, r);
            }

            r.urlPrefix = urlPrefix;

            // Flag unsupported va_list methods so they will not be generated
            module.flagVaListFunctions();

            // Apply patch
            if (patch != null) {
                patch.patch(r);
            }

        } catch (IOException ignored) {
            // Gir file not found for this platform: This will generate code with UnsupportedPlatformExceptions
        }

        // Link the type references to the GIR type definition across the GI repositories
        module.link();

        return module;
    }

    /**
     * Return a set of package names for all directories in the src/main/java folder that
     * contain at least one *.java file.
     */
    private Set<String> getPackages() throws IOException {
        Set<String> packages = new HashSet<>();
        var srcDir = getProject().getProjectDir().toPath()
                .resolve(Path.of("src", "main", "java"));
        if (! Files.exists(srcDir)) {
            return packages;
        }
        Files.walkFileTree(srcDir, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".java")) {
                    String pkg = srcDir.relativize(file.getParent()).toString()
                            .replace(srcDir.getFileSystem().getSeparator(), ".");
                    packages.add(pkg);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return packages;
    }
}
