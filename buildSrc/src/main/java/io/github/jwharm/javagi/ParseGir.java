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

import static io.github.jwharm.javagi.util.FileUtils.findFile;

/**
 * GenerateSources is a Gradle task that will parse a GIR file (and all included GIR files)
 * and generate Java source files for the types defined in the GIR file.
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

    private Module parse(Directory baseFolder, String moduleName) throws XMLStreamException {
        System.out.println("Parse " + moduleName);
        GirParser parser = GirParser.getInstance();
        Repository repository = null;
        for (Integer platform : Platform.toList(Platform.ALL)) {
            try {
                File girFile = findFile(baseFolder.dir(Platform.toString(platform)).getAsFile(), moduleName);
                repository = parser.parse(girFile, platform, repository);
            } catch (FileNotFoundException ignored) {
            }
        }
        return new Module(moduleName, repository);
    }
}
