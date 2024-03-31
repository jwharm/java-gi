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
import io.github.jwharm.javagi.configuration.ModuleInfo;
import io.github.jwharm.javagi.generators.*;
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.gir.Enumeration;
import io.github.jwharm.javagi.gir.Library;
import io.github.jwharm.javagi.gir.Record;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.StandardOpenOption.*;

/**
 * GenerateSources is a Gradle task that will generate Java source files for
 * the types defined in a GIR Library. (The Library is provided by the
 * GirParserService.)
 */
public abstract class GenerateSources extends DefaultTask {

    @ServiceReference("gir")
    abstract Property<GirParserService> getGirParserService();

    @Input
    abstract Property<String> getNamespace();

    @OutputDirectory
    abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    void execute() {
        try {
            GirParserService buildService = getGirParserService().get();
            String namespace = getNamespace().get();
            Library library = buildService.getLibrary(namespace);
            generate(namespace, library, getOutputDirectory().get());
        } catch (Exception e) {
            throw new TaskExecutionException(this, e);
        }
    }

    // Generate Java source files for a GIR repository
    private void generate(String namespace,
                          Library library,
                          Directory outputDirectory) throws IOException {

        Namespace ns = library.lookupNamespace(namespace);
        String packageName = ModuleInfo.packageName(namespace);

        // Generate class with namespace-global constants and functions
        var typeSpec = new NamespaceGenerator(ns).generateGlobalsClass();
        writeJavaFile(typeSpec, packageName, outputDirectory);

        // Generate package-info.java
        Path path = outputDirectory
                .dir(packageName.replace('.', File.separatorChar))
                .getAsFile()
                .toPath()
                .resolve("package-info.java");
        String packageInfo = new PackageInfoGenerator(ns).generate();
        Files.writeString(path, packageInfo,
                CREATE, WRITE, TRUNCATE_EXISTING);

        // Generate module-info.java
        path = outputDirectory
                .getAsFile()
                .toPath()
                .resolve("module-info.java");
        String moduleInfo = new ModuleInfoGenerator(ns, getPackages()).generate();
        Files.writeString(path, moduleInfo,
                CREATE, WRITE, TRUNCATE_EXISTING);

        // Generate classes for all registered types in this namespace
        for (var rt : ns.registeredTypes().values()) {

            // Do not generate record types named "...Private" (except for
            // GPrivate)
            if (rt instanceof Record rec
                    && (! "GPrivate".equals(rec.cType()))
                    && rec.name().endsWith("Private"))
                continue;

            typeSpec = switch(rt) {
                case Alias a -> new AliasGenerator(a).generate();
                case Bitfield b -> new BitfieldGenerator(b).generate();
                case Callback c -> new CallbackGenerator(c).generate();
                case Class c -> new ClassGenerator(c).generate();
                case Enumeration e -> new EnumerationGenerator(e).generate();
                case Interface i -> new InterfaceGenerator(i).generate();
                case Record r when r.isGTypeStructFor() == null -> new RecordGenerator(r).generate();
                case Union u -> new UnionGenerator(u).generate();
                default -> null;
            };
            writeJavaFile(typeSpec, packageName, outputDirectory);
        }
    }

    // Write a generated class into a Java file
    private void writeJavaFile(TypeSpec typeSpec,
                               String packageName,
                               Directory outputDirectory) throws IOException {
        if (typeSpec == null) return;

        JavaFile.builder(packageName, typeSpec)
                .addFileComment(LicenseNotice.NOTICE)
                .indent("    ")
                .build()
                .writeTo(outputDirectory.getAsFile());
    }

    /*
     * Return a set of package names for all directories in the src/main/java
     * folder that contain at least one *.java file. These packages will be
     * exported in the module-info.java file.
     */
    private Set<String> getPackages() throws IOException {
        Set<String> packages = new HashSet<>();

        var srcDir = getProject().getProjectDir().toPath()
                .resolve(Path.of("src", "main", "java"));

        if (! Files.exists(srcDir))
            return packages;

        Files.walkFileTree(
                srcDir,
                EnumSet.noneOf(FileVisitOption.class),
                Integer.MAX_VALUE,
                new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".java")) {
                    String pkg = srcDir
                            .relativize(file.getParent())
                            .toString()
                            .replace(srcDir.getFileSystem().getSeparator(), ".");
                    packages.add(pkg);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return packages;
    }
}
