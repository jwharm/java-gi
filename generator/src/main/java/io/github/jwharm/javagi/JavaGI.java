/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 the Java-GI developers
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
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.Platform;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

@CommandLine.Command(
        name = "java-gi",
        mixinStandardHelpOptions = true,
        version = "java-gi 0.11.0-SNAPSHOT",
        description = "Generate Java language bindings from a GObject-Introspection repository (gir) file.")
public class JavaGI implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-g", "--gir-files"},
            paramLabel = "dir",
            defaultValue = "/usr/share/gir-1.0",
            description = "gir files directory, default: /usr/share/gir-1.0")
    private File girDirectory;

    @CommandLine.Option(
            names = {"-o", "--output"},
            paramLabel = "dir",
            defaultValue = ".",
            description = "output directory, default: current working directory")
    private File outputDirectory;

    @CommandLine.Option(
            names = {"-m", "--target-module"},
            paramLabel = "module",
            description = "name of the generated java module, default: gir namespace name")
    private String moduleName;

    @CommandLine.Option(
            names = {"-p", "--target-package"},
            paramLabel = "package",
            description = "name of the generated java package, default: gir namespace name")
    private String packageName;

    @CommandLine.Option(
            names = {"-d", "--description"},
            paramLabel = "description",
            description = "short description of the library to include in the javadoc of the generated Java package")
    private String description;

    @CommandLine.Option(
            names = {"-u", "--doc-url-prefix"},
            paramLabel = "url",
            defaultValue = "",
            description = "url of the online API documentation to prefix before hyperlinks in the generated javadoc")
    private String docUrlPrefix;

    @CommandLine.Parameters(
            index = "0",
            description = "gir file to process")
    private File girFile;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new JavaGI()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        if (girDirectory == null || (! girDirectory.isDirectory()))
            throw new IllegalArgumentException("gir files directory not found");

        if (girFile == null || (! girFile.exists()))
            throw new IllegalArgumentException("gir file not found");

        if (! outputDirectory.exists())
            if (! outputDirectory.mkdirs())
                throw new IllegalArgumentException("Cannot create output directory");

        var library = parseGirDirectory(girDirectory);

        // Ensure that at least GLib gir file is present
        library.lookupNamespace("GLib");

        // Parse the gir file
        var parser = GirParser.getInstance();
        var repository = parser.parse(girFile, Platform.LINUX, null);

        if (repository == null || repository.namespace() == null)
            throw new IllegalArgumentException("Invalid gir file");

        var namespace = repository.namespace().name();
        if (moduleName == null) moduleName = namespace.toLowerCase();
        if (packageName == null) packageName = namespace.toLowerCase();
        ModuleInfo.add(namespace, moduleName, packageName, docUrlPrefix, description);
        library.put(girFile.getName(), repository);

        // No custom packages to export in module-info.java
        var packages = new HashSet<String>();

        generate(namespace, library, packages, outputDirectory);

        return 0;
    }

    public static Library parseGirDirectory(@NotNull File girDirectory)
            throws XMLStreamException, FileNotFoundException {
        var library = new Library();
        var parser = GirParser.getInstance();

        // Load gir files from platform-specific subdirectories
        for (var platform : Platform.toList(Platform.ALL)) {
            String platformName = Platform.toString(platform);
            File platformDirectory = new File(girDirectory, platformName);
            for (File girFile : listGirFiles(platformDirectory)) {
                var name = girFile.getName();
                var repo = parser.parse(girFile, platform, library.get(name));
                library.put(name, repo);
            }
        }

        // Load gir files from the directory itself
        for (File girFile : listGirFiles(girDirectory)) {
            var name = girFile.getName();
            var repo = parser.parse(girFile, Platform.LINUX, library.get(name));
            library.put(name, repo);
        }

        return library;
    }

    // Generate Java source files for a GIR repository
    public static void generate(String namespace,
                                Library library,
                                Set<String> packages,
                                File outputDirectory) throws IOException {

        Namespace ns = library.lookupNamespace(namespace);
        String packageName = ModuleInfo.packageName(namespace);

        // Generate class with namespace-global constants and functions
        var typeSpec = new NamespaceGenerator(ns).generateGlobalsClass();
        writeJavaFile(typeSpec, packageName, outputDirectory);

        // Generate package-info.java
        Path path = outputDirectory.toPath()
                .resolve(packageName.replace('.', File.separatorChar))
                .resolve("package-info.java");
        String packageInfo = new PackageInfoGenerator(ns).generate();
        Files.writeString(path, packageInfo,
                CREATE, WRITE, TRUNCATE_EXISTING);

        // Generate module-info.java
        path = outputDirectory
                .toPath()
                .resolve("module-info.java");
        String moduleInfo = new ModuleInfoGenerator(ns, packages).generate();
        Files.writeString(path, moduleInfo,
                CREATE, WRITE, TRUNCATE_EXISTING);

        // Generate classes for all registered types in this namespace
        for (var rt : ns.registeredTypes().values()) {

            // Do not generate record types named "...Private" (except for
            // GPrivate)
            if (rt.skipJava())
                continue;

            typeSpec = switch(rt) {
                case Alias a -> new AliasGenerator(a).generate();
                case Boxed b -> new BoxedGenerator(b).generate();
                case Callback c -> new CallbackGenerator(c).generate();
                case Class c -> new ClassGenerator(c).generate();
                case FlaggedType f -> new FlaggedTypeGenerator(f).generate();
                case Interface i -> new InterfaceGenerator(i).generate();
                case Record r when r.isGTypeStructFor() == null ->
                        new RecordGenerator(r).generate();
                case Union u -> new UnionGenerator(u).generate();
                default -> null;
            };
            writeJavaFile(typeSpec, packageName, outputDirectory);

            // Write package-private helper classes for interfaces, containing
            // static downcall handles
            if (rt instanceof Interface i) {
                var generator = new InterfaceGenerator(i);
                if (generator.hasDowncallHandles())
                    writeJavaFile(generator.downcallHandlesClass(),
                            packageName,
                            outputDirectory);
            }
        }
    }

    // Write a generated class into a Java file
    public static void writeJavaFile(TypeSpec typeSpec,
                                     String packageName,
                                     File outputDirectory) throws IOException {
        if (typeSpec == null) return;

        JavaFile.builder(packageName, typeSpec)
                .addFileComment(LicenseNotice.NOTICE)
                .indent("    ")
                .build()
                .writeTo(outputDirectory);
    }

    /*
     * Return an array of all *.gir files in this directory. If the directory
     * does not exist or contains no gir files, an empty array is returned.
     */
    private static File[] listGirFiles(@NotNull File directory) {
        File[] empty = new File[] {};
        if (! directory.exists())
            return empty;
        File[] files = directory.listFiles((_, name) -> name.endsWith(".gir"));
        return files == null ? empty : files;
    }
}
