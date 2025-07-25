/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 Jan-Willem Harmannij
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

import org.javagi.gir.GirParser;
import org.javagi.gir.Library;
import org.javagi.gir.Repository;
import org.javagi.metadata.MetadataParser;
import org.javagi.util.Platform;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * A Gradle build service that provides Library objects containing a GIR
 * Repository for a requested GIR file, and all GIR repositories that it
 * depends on. The build service caches all repositories so every GIR file is
 * only parsed once during a build.
 */
public abstract class GirParserService implements BuildService<GirParserService.Params> {

    public interface Params extends BuildServiceParameters {
        ConfigurableFileCollection getInputDirectories();
    }

    private final Library library = new Library();

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
        Repository repository = library.get(name);
        if (repository != null)
            return library;

        // Parse the repository. This also adds it to the library and applies
        // metadata (if any).
        repository = parse(name);

        // Make sure all dependencies are in the library
        for (var include : repository.includes())
            getLibrary(include.name());

        return library;
    }

    /*
     * Call parse(directory, moduleName) for all input directories, and
     * wrap exceptions in runtime exceptions.
     */
    private Repository parse(String moduleName) {
        for (File basePath : getParameters().getInputDirectories()) {
            try {
                return parse(basePath, moduleName);
            } catch (FileNotFoundException ignored) {
            } catch (XMLStreamException x) {
                throw new RuntimeException(x);
            }
        }
        throw new RuntimeException(new FileNotFoundException(
                "No GIR files found for %s".formatted(moduleName)));
    }

    // Read the GIR files for all platforms and parse them into a Repository.
    private Repository parse(File baseFolder, String moduleName)
            throws XMLStreamException, FileNotFoundException {
        Repository repository = null;

        // First try /macos, /windows and /linux subfolders.
        for (Integer platform : List.of(Platform.MACOS, Platform.WINDOWS, Platform.LINUX)) {
            try {
                File subFolder = new File(baseFolder, Platform.toString(platform));
                File girFile = findFile(subFolder, moduleName + "-");
                repository = GirParser.getInstance().parse(girFile, platform, repository);
            } catch (FileNotFoundException ignored) {
            }
        }

        // If the gir file was not found in a platform-specific subfolder, try
        // the base folder.
        if (repository == null) {
            try {
                int platform = Platform.getRuntimePlatform();
                File girFile = findFile(baseFolder, moduleName + "-");
                repository = GirParser.getInstance().parse(girFile, platform, null);
            } catch (FileNotFoundException ignored) {
                throw new FileNotFoundException("No GIR files found for %s in %s"
                        .formatted(moduleName, baseFolder.getName()));
            }
        }

        // Add the repository to the library
        library.put(moduleName, repository);

        // Apply metadata (if it exists)
        new MetadataParser().parse(repository);

        return repository;
    }

    // Find a Gir file in the given folder with the given filename prefix.
    private static File findFile(File folder, String prefix) throws FileNotFoundException {
        File[] files = folder.listFiles();

        if (files != null)
            for (File file : files)
                if (file.isFile()
                        && file.getName().startsWith(prefix)
                        && file.getName().endsWith(".gir"))
                    return file;

        throw new FileNotFoundException("%s.gir not found in %s".formatted(prefix, folder));
    }
}
