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
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates Java sources from GIR files
 *
 * @since 0.5
 */
public class GenerateSourcesOperation {
    private Path sourceDirectory = null;
    private Path outputDirectory = null;
    private final List<Source> sources = new ArrayList<>();
    private String moduleInfo;

    /**
     * Performs the JavaGI operation.
     * @since 0.5
     */
    public void execute() throws Exception {
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

        // Write module-info.java
        Files.writeString(outputDirectory().resolve("module-info.java"), moduleInfo()
                .formatted(namespaces.stream()
                        .map(s -> "exports " + s + ";")
                        .collect(Collectors.joining("\n    "))
                )
        );
    }

    public Module parse(Platform platform) throws ParserConfigurationException, SAXException {
        Module module = new Module(platform);
        GirParser parser = new GirParser(module);
        Path girPath = sourceDirectory().resolve(platform.name().toLowerCase());
        if (! girPath.toFile().exists()) {
            System.out.println("Not found: " + girPath);
            return null;
        }

        // Parse the GI files into Repository objects
        for (var source : sources()) {
            try {
                // Parse the file
                Repository r = parser.parse(girPath.resolve(source.fileName), source.pkg);
                r.generate = source.generate;
                r.urlPrefix = source.urlPrefix;

                // Add the repository to the module
                module.repositories.put(r.namespace.name, r);

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
     * @param pkg the java package name to generate
     * @param urlPrefix URL to prefix before links to images
     * @param generate true when classes must be generated, false when this Source is only needed as a dependency
     * @param patch patch to apply to a parsed gi repository before generating classes
     */
    public record Source(String fileName, String pkg, String urlPrefix, boolean generate, Patch patch) {}

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
     * Provides the contents for the module-info.java file.
     * @param moduleInfo the module-info contents
     * @return this operation instance
     * @since 0.5
     */
    public GenerateSourcesOperation moduleInfo(String moduleInfo) {
        this.moduleInfo = moduleInfo;
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
     * Create a new Source
     * @param file the gir filename
     * @param pkg the package name
     * @param urlPrefix the prefix for image link URLs
     * @param generate whether to generate bindings for this source
     * @param patches patch to apply before generating bindings
     */
    public GenerateSourcesOperation source(String file, String pkg, String urlPrefix, boolean generate, Patch patches) {
        sources(new Source(file, pkg, urlPrefix, generate, patches));
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

    /**
     * Retrieves the provided module-info.java contents.
     * @return the provided module-info.java contents
     * @since 0.5
     */
    public String moduleInfo() {
        return moduleInfo;
    }
}
