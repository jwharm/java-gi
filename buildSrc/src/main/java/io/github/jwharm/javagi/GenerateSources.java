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

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import io.github.jwharm.javagi.configuration.LicenseNotice;
import io.github.jwharm.javagi.generators.*;
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.gir.Enumeration;
import io.github.jwharm.javagi.gir.Module;
import io.github.jwharm.javagi.gir.Record;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.Optional;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.zip.InflaterInputStream;

/**
 * GenerateSources is a Gradle task that will parse a GIR file (and all included GIR files)
 * and generate Java source files for the types defined in the GIR file.
 */
public abstract class GenerateSources extends DefaultTask {

    @InputFile
    abstract RegularFileProperty getInputFile();

    @OutputDirectory
    abstract DirectoryProperty getOutputDirectory();

    @Input @Optional
    abstract Property<String> getUrlPrefix();

    @TaskAction
    void execute() {
        File inputFile = getInputFile().getAsFile().get();
        Module module;

        try (var in = new ObjectInputStream(new InflaterInputStream(
                new BufferedInputStream(new FileInputStream(inputFile))))) {
            module = (Module) in.readObject();
        } catch (Exception e) {
            throw new TaskExecutionException(this, e);
        }

        try {
            generate(module, getOutputDirectory().get());
        } catch (Exception e) {
            throw new TaskExecutionException(this, e);
        }
    }

    private static void generate(Module module, Directory outputDirectory) {
        Namespace ns = module.lookupNamespace(module.name());
        var typeSpec = new NamespaceGenerator(ns).generateGlobalsClass();
        try {
            generate(typeSpec, ns.packageName(), outputDirectory);
        } catch (Exception _) {
        }
        for (var rt : ns.registeredTypes().values()) {
            try {
                typeSpec = switch(rt) {
                    case Alias a -> new AliasGenerator(a).generate();
                    case Bitfield b -> new BitfieldGenerator(b).generate();
                    case Boxed b -> null;
                    case Callback c -> new CallbackGenerator(c).generate();
                    case Class c -> new ClassGenerator(c).generate();
                    case Enumeration e -> new EnumerationGenerator(e).generate();
                    case Interface i -> new InterfaceGenerator(i).generate();
                    case Record r -> new RecordGenerator(r).generate();
                    case Union u -> new UnionGenerator(u).generate();
                };
                generate(typeSpec, ns.packageName(), outputDirectory);
            } catch (Exception _) {
            }
        }
    }

    private static void generate(TypeSpec typeSpec, String packageName, Directory outputDirectory)
            throws IOException {
        if (typeSpec == null) return;

        JavaFile.builder(packageName, typeSpec)
                .addFileComment(LicenseNotice.NOTICE)
                .indent("    ")
                .build()
                .writeTo(outputDirectory.getAsFile());
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
