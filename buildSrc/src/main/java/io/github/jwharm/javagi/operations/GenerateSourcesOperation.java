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

package io.github.jwharm.javagi.operations;

import io.github.jwharm.javagi.generator.*;
import io.github.jwharm.javagi.model.Module;
import io.github.jwharm.javagi.model.Repository;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Generates Java sources from GIR files
 *
 * @since 0.5
 */
public class GenerateSourcesOperation {
    private Path sourceDirectory = null;
    private Path outputDirectory = null;
    private final List<Source> sources = new ArrayList<>();
    private static Map<String, String> packageNames = null;

    /**
     * Performs the JavaGI operation.
     * @since 0.5
     */
    public void execute() throws Exception {
        Conversions.packageNames = packageNames;
        Set<String> namespaces = new LinkedHashSet<>();

        // Parse all platform-specific gir files
        Module windows = parse(Platform.WINDOWS);
        Module linux = parse(Platform.LINUX);
        Module macos = parse(Platform.MACOS);

        // Merge the gir repositories into one cross-platform repository
        Module module = new Merge().merge(windows, linux, macos);

        // Generate bindings classes
        for (Repository repository : module.repositories.values()) {
            if (repository.generate) {
                Path basePath = outputDirectory().resolve(repository.namespace.pathName);
                repository.generate(basePath);
                namespaces.add(repository.namespace.packageName);
            }
        }
    }

    public Module parse(Platform platform) throws ParserConfigurationException, SAXException {
        Module module = new Module(platform);
        Path girPath = sourceDirectory().resolve(platform.name().toLowerCase());
        if (! girPath.toFile().exists()) {
            System.out.println("Not found: " + girPath);
            return null;
        }
        GirParser parser = new GirParser(girPath, module);

        // Parse the GI files into Repository objects
        for (var source : sources()) {
            try {
                // Parse the file
                Repository r = parser.parse(source.fileName);

                // Check if this one has already been parsed
                if (module.repositories.containsKey(r.namespace.name)) {
                    r = module.repositories.get(r.namespace.name);
                } else {
                    // Add the repository to the module
                    module.repositories.put(r.namespace.name, r);
                }

                r.urlPrefix = source.urlPrefix;

                // Flag unsupported va_list methods so they will not be generated
                module.flagVaListFunctions();

                // Apply patch
                if (source.patch != null) {
                    source.patch.patch(r);
                }

            } catch (IOException ignored) {
                // Gir file not found for this platform: This will generate code with UnsupportedPlatformExceptions
            }
        }

        // Link the type references to the GIR type definition across the GI repositories
        module.link();

        return module;
    }

    /**
     * Source gir file to parse
     * @param fileName the filename of the gir file
     * @param urlPrefix URL to prefix before links to images
     * @param patch patch to apply to a parsed gi repository before generating classes
     */
    public record Source(String fileName, String urlPrefix, Patch patch) {}

    /**
     * Provides the source directory that will be used for the JavaGI operation.
     * @param directory the source directory
     * @return this operation instance
     * @since 0.5
     */
    public GenerateSourcesOperation sourceDirectory(Path directory) {
        sourceDirectory = directory;
        return this;
    }

    /**
     * Provides the output directory where all output is generated.
     * @param directory the output directory
     * @return this operation instance
     * @since 0.5
     */
    public GenerateSourcesOperation outputDirectory(Path directory) {
        outputDirectory = directory;
        return this;
    }

    /**
     * Provides the sources for which bindings are generated.
     * @param sources the sources
     * @return this operation instance
     * @since 0.5
     */
    public GenerateSourcesOperation sources(Source... sources) {
        return sources(Arrays.asList(sources));
    }

    /**
     * Provides a list of source for which bindings are generated.
     * @param sources the sources
     * @return this operation instance
     * @since 0.5
     */
    public GenerateSourcesOperation sources(List<Source> sources) {
        this.sources.addAll(sources);
        return this;
    }

    /**
     * Provide the map of namespaces to package names
     * @param map the map of namespaces to package names
     * @since 0.7
     */
    public static void packageNames(Map<String, String> map) {
        packageNames = map;
    }

    /**
     * Create a new Source
     * @param file the gir filename
     * @param urlPrefix the prefix for image link URLs
     * @param patches patch to apply before generating bindings
     */
    public GenerateSourcesOperation source(String file, String urlPrefix, Patch patches) {
        sources(new Source(file, urlPrefix, patches));
        return this;
    }

    /**
     * Retrieves the source directory that will be used for the
     * JavaGI operation.
     * @return the source directory, or {@code null} if the directory
     * wasn't specified.
     * @since 0.5
     */
    public Path sourceDirectory() {
        return sourceDirectory;
    }

    /**
     * Retrieves the list of sources that will be used for the
     * JavaGI operation.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     * @return the source files
     * @since 0.5
     */
    public List<Source> sources() {
        return sources;
    }

    /**
     * Retrieves the output directory where all output is generated.
     * @return the output directory, or {@code null} if the directory
     * wasn't specified.
     * @since 0.5
     */
    public Path outputDirectory() {
        return outputDirectory;
    }
}
