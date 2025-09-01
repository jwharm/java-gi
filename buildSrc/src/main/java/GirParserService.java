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
import org.javagi.metadata.Matcher;
import org.javagi.metadata.Parser;
import org.javagi.util.Platform;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    // A list of loaded repositories, also used as a locking mechanism to
    // prevent multiple repositories from being loaded concurrently in
    // parallel builds.
    private final Map<String, Repository> repositories = new ConcurrentHashMap<>();

    /**
     * Create and return a Library that contains the requested Repositories
     * with their dependencies.
     *
     * @param  girFiles the names and versions of the requested Repositories
     * @return a Library object with the Repositories and their dependencies
     */
    public Library getLibrary(List<String> girFiles) {
        Library library = new Library();
        for (String repo : girFiles) {
            int dash = repo.indexOf('-');
            if (dash <= 0 || dash >= (repo.length() - 1))
                throw new IllegalArgumentException("Repository not in 'name-version' format: " + repo);
            String name = repo.substring(0, dash);
            String version = repo.substring(dash + 1);
            addToLibrary(library, name, version);
        }
        return library;
    }

    /*
     * Parse the requested gir file and its dependencies, and add them
     * to the library.
     */
    private void addToLibrary(Library library, String name, String version) {
        Repository repository = library.get(name);
        if (repository != null)
            return;

        // Parse the repository
        repositories.computeIfAbsent(name, n -> parse(n, version));
        repository = repositories.get(name);

        // Parse all dependencies and add them to the library
        for (var include : repository.includes())
            addToLibrary(library, include.name(), include.version());

        // Add the repository to the library
        library.put(name, repository);
    }

    /*
     * Call parse(directory, moduleName) for all input directories, and
     * wrap exceptions in runtime exceptions.
     */
    private Repository parse(String name, String version) {
        for (File basePath : getParameters().getInputDirectories()) {
            try {
                return parse(basePath, name, version);
            } catch (FileNotFoundException ignored) {
            } catch (XMLStreamException x) {
                throw new RuntimeException(x);
            }
        }
        throw new RuntimeException(new FileNotFoundException(
                "No GIR files found for %s-%s".formatted(name, version)));
    }

    // Read the GIR files for all platforms and parse them into a Repository.
    private Repository parse(File baseFolder, String name, String version)
            throws XMLStreamException, FileNotFoundException {
        Repository repository = null;

        // First try /macos, /windows and /linux subfolders.
        for (Integer platform : List.of(Platform.MACOS, Platform.WINDOWS, Platform.LINUX)) {
            File subFolder = new File(baseFolder, Platform.toString(platform));
            File girFile = new File(subFolder, name + "-" + version + ".gir");
            if (girFile.exists())
                repository = GirParser.getInstance().parse(girFile, platform, repository);
        }

        // If the gir file was not found in a platform-specific subfolder, try
        // the base folder.
        if (repository == null) {
            int platform = Platform.getRuntimePlatform();
            File girFile = new File(baseFolder, name + "-" + version + ".gir");
            if (girFile.exists())
                repository = GirParser.getInstance().parse(girFile, platform, null);
        }

        if (repository == null)
            throw new FileNotFoundException("No GIR files found for %s-%s in %s"
                    .formatted(name, version, baseFolder.getName()));

        // Apply metadata (if it exists)
        new Matcher().match(new Parser(name, version).parse(), repository);

        return repository;
    }
}
