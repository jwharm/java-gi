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

package io.github.jwharm.javagi;

import io.github.jwharm.javagi.gir.GirParser;
import io.github.jwharm.javagi.gir.Library;
import io.github.jwharm.javagi.gir.Repository;
import io.github.jwharm.javagi.util.Platform;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * A Gradle build service that provides Library objects containing a GIR
 * Repository for a requested GIR file, and all GIR repositories that it
 * depends on. The build service caches all repositories so every GIR file is
 * only parsed once during a build.
 */
public abstract class GirParserService
        implements BuildService<GirParserService.Params> {

    interface Params extends BuildServiceParameters {
        DirectoryProperty getInputDirectory();
    }

    private final Library library = new Library();

    /**
     * Create a new GirParserService. This will only check if the input
     * directory is specified and exists.
     *
     * @throws IllegalArgumentException the input directory is not specified
     * @throws FileNotFoundException    the input directory does not exist
     */
    public GirParserService() throws FileNotFoundException {
        var dir = getParameters().getInputDirectory();
        if (!dir.isPresent())
            throw new IllegalArgumentException("Input directory is not set");

        if (!dir.get().getAsFile().exists())
            throw new FileNotFoundException("Input directory does not exist: "
                    + dir.get().getAsFile());
    }

    /**
     * Create and return a Library that contains the requested Repository with
     * its dependencies.
     * <p>
     * When building multiple modules, the library is shared, so it will often
     * contain more Repositories than were requested.
     *
     * @param  name the name of the requested Repository
     * @return a Library object with the Repository and its dependencies, and
     *         possibly other Repositories
     */
    public Library getLibrary(String name) {
        Repository repository = library.computeIfAbsent(name, this::parse);

        for (var include : repository.includes())
            getLibrary(include.name());

        return library;
    }

    /*
     * Call parse(getInputDirectory(), moduleName) and wrap exceptions in
     * runtime exceptions.
     */
    private Repository parse(String moduleName) {
        try {
            Directory basePath = getParameters().getInputDirectory().get();
            return parse(basePath, moduleName);
        } catch (XMLStreamException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // Read the GIR files for all platforms and parse them into a Repository.
    private Repository parse(Directory baseFolder, String moduleName)
            throws XMLStreamException, FileNotFoundException {
        Repository repository = null;

        for (Integer platform : Platform.toList(Platform.ALL)) {
            try {
                File girFile = findFile(
                        baseFolder.dir(Platform.toString(platform)).getAsFile(),
                        moduleName + "-"
                );
                repository = GirParser.getInstance().parse(
                        girFile,
                        platform,
                        repository
                );
            } catch (FileNotFoundException ignored) {
            }
        }

        if (repository == null)
            throw new FileNotFoundException("No GIR files found for %s"
                    .formatted(moduleName));

        repository.setLibrary(library);

        return repository;
    }

    // Find a file in the given folder with the given filename prefix.
    private static File findFile(File folder, String fileNamePrefix)
            throws FileNotFoundException {
        File[] files = folder.listFiles();

        if (files != null)
            for (File file : files)
                if (file.isFile() && file.getName().startsWith(fileNamePrefix))
                    return file;

        throw new FileNotFoundException("%s not found in %s"
                .formatted(fileNamePrefix, folder));
    }
}
