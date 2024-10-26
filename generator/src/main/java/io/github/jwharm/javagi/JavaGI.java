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

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public class JavaGI {

    public static void main(String[] args)
            throws XMLStreamException, IOException {
        File girDirectory = new File("../ext/gir-files");
        File outputDirectory = new File("build/generated/sources/java-gi");
        String namespace = "Gtk";
        System.out.printf("Parse gir files from %s\n", girDirectory.getAbsolutePath());
        var library = parseGirFiles(girDirectory);
        System.out.printf("Generate %s to %s\n", namespace, outputDirectory.getAbsolutePath());
        generate(namespace, library, outputDirectory);
    }

    public static Library parseGirFiles(@NotNull File girDirectory)
            throws XMLStreamException, FileNotFoundException {
        var library = new Library();
        var parser = GirParser.getInstance();

        for (var platform : Platform.toList(Platform.ALL)) {
            String platformName = Platform.toString(platform);
            File dir = new File(girDirectory, platformName);
            for (File girFile : listGirFiles(dir)) {
                var name = girFile.getName();
                var repo = parser.parse(girFile, platform, library.get(name));
                library.put(name, repo);
                System.out.printf("Loaded %s %s\n", platformName, name);
            }
        }

        return library;
    }

    // Generate Java source files for a GIR repository
    private static void generate(String namespace,
                                 Library library,
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
        String moduleInfo = new ModuleInfoGenerator(ns, new HashSet<>()).generate();
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
    private static void writeJavaFile(TypeSpec typeSpec,
                                      String packageName,
                                      File outputDirectory) throws IOException {
        if (typeSpec == null) return;

        JavaFile.builder(packageName, typeSpec)
                .addFileComment(LicenseNotice.NOTICE)
                .indent("    ")
                .build()
                .writeTo(outputDirectory);
    }

    private static File[] listGirFiles(@NotNull File parent) {
        File[] empty = new File[] {};
        if (! parent.exists())
            return empty;
        File[] files = parent.listFiles((_, name) -> name.endsWith(".gir"));
        return files == null ? empty : files;
    }
}
