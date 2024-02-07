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

import io.github.jwharm.javagi.gir.Module;
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.util.Platform;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.*;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * ParseGir is a Gradle task that will parse a GIR file into a Module object.
 * It will read the Module objects it depends on, and add them to the new
 * Module. The resulting Module is serialized to an output file.
 * <p>
 * When GIR files are found for multiple platforms, they will be merged into
 * one tree.
 */
public abstract class ParseGir extends DefaultTask {

    @InputFiles
    abstract ListProperty<RegularFile> getIncludedModules();

    @InputDirectory
    abstract DirectoryProperty getInputDirectory();

    @OutputFile
    abstract RegularFileProperty getOutputFile();

    @Input
    abstract Property<String> getModuleName();

    @TaskAction
    void execute() {
        Directory basePath = getInputDirectory().get();
        String moduleName = getModuleName().get();
        Module module;

        // Read the GIR file
        try {
            module = parse(basePath, moduleName);
        } catch (Exception e) {
            throw new TaskExecutionException(this, e);
        }

        // Read other module files
        for (RegularFile rf : getIncludedModules().get()) {
            var file = rf.getAsFile();
            try (var in = new ObjectInputStream(new InflaterInputStream(
                    new BufferedInputStream(new FileInputStream(file))))) {
                Module other = (Module) in.readObject();
                module.add(other);
            } catch (Exception e) {
                throw new TaskExecutionException(this, e);
            }
        }

        // Write resulting module to output file
        File file = getOutputFile().getAsFile().get();
        try (var out = new ObjectOutputStream(new DeflaterOutputStream(
                new BufferedOutputStream(new FileOutputStream(file))))) {
            out.writeObject(module);
        } catch (Exception e) {
            throw new TaskExecutionException(this, e);
        }
    }

    // Read the GIR files for all platforms and parse them into a Repository.
    // Return a new Module file containing this Repository.
    private Module parse(Directory baseFolder, String moduleName)
            throws XMLStreamException, FileNotFoundException {
        Repository repository = null;

        for (Integer platform : Platform.toList(Platform.ALL)) {
            try {
                File girFile = findFile(
                        baseFolder.dir(Platform.toString(platform)).getAsFile(),
                        moduleName
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
            throw new FileNotFoundException("No GIR files found for " + moduleName);

        return new Module(moduleName, repository);
    }

    // Find a file in the given folder with the given filename prefix.
    private static File findFile(File folder, String fileNamePrefix)
            throws FileNotFoundException {
        File[] files = folder.listFiles();

        if (files != null)
            for (File file : files)
                if (file.isFile() && file.getName().startsWith(fileNamePrefix))
                    return file;

        throw new FileNotFoundException(fileNamePrefix + " not found in " + folder);
    }
}
