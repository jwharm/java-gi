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

package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Repository extends GirElement {

    public final Module module;
    public Namespace namespace = null;
    public Package package_ = null;
    public final boolean generate;
    public String urlPrefix;

    public Repository(Module module, boolean generate) {
        super(null);
        this.module = module;
        this.generate = generate;
    }

    /**
     * Generate Java bindings for the provided GI repository
     */
    public void generate(Path basePath) throws IOException {

        Files.createDirectories(basePath);

        // Create a java file for each RegisteredType (class, interface, ...)
        for (RegisteredType rt : namespace.registeredTypeMap.values()) {

            // Class type structs are generated as inner classes
            if (rt instanceof Record rec && rec.isGTypeStructFor != null) {
                continue;
            }

            // Private inner structs can be omitted from language bindings, but don't exclude `GPrivate`
            if (rt instanceof Record rec && rec.name.endsWith("Private") && (! rec.name.equals("Private"))) {
                continue;
            }

            // Generate the class
            try (SourceWriter writer = new SourceWriter(Files.newBufferedWriter(basePath.resolve(rt.javaName + ".java")))) {
                rt.generate(writer);
            }
        }
        // Create a class file for global declarations
        generateGlobals(basePath);
    }

    /**
     * Generate the contents for the class with the namespace-global declarations and a package-info.
     * The name of the class is the namespace identifier.
     */
    public void generateGlobals(Path basePath) throws IOException {
        String className = Conversions.convertToJavaType(namespace.globalClassName, false, namespace);
        try (SourceWriter writer = new SourceWriter(Files.newBufferedWriter(basePath.resolve(className + ".java")))) {

            writer.write("package " + namespace.packageName + ";\n");
            writer.write("\n");
            generateImportStatements(writer);

            writer.write("/**\n");
            writer.write(" * Constants and functions that are declared in the global " + className + " namespace.\n");
            writer.write(" */\n");
            writer.write("public final class " + className + " {\n");
            writer.increaseIndent();
            writer.write("\n");
            writer.write("static {\n");
            writer.increaseIndent();

            // Load libraries
            if (namespace.sharedLibrary != null) {
                writer.write("switch (Platform.getRuntimePlatform()) {\n");
                writer.increaseIndent();
                namespace.sharedLibraries.forEach((platform, libraryName) -> {
                    // Remove path from library name
                    String libFilename = libraryName;
                    if (libraryName.contains("/")) {
                        libFilename = libFilename.substring(libFilename.lastIndexOf("/") + 1);
                    }
                    try {
                        if (libFilename.contains(",")) {
                            writer.write("case \"" + platform.name + "\" -> {\n");
                            for (String libName : libFilename.split(",")) {
                                writer.write("    LibLoad.loadLibrary(\"" + libName + "\");\n");
                            }
                            writer.write("}\n");
                        } else {
                            writer.write("case \"" + platform.name + "\" -> LibLoad.loadLibrary(\"" + libFilename + "\");\n");
                        }
                    } catch (IOException ignored) {
                    }
                });
                writer.decreaseIndent();
                writer.write("}\n");
            }

            // Register types
            writer.write("registerTypes();\n");
            writer.decreaseIndent();
            writer.write("}\n");
            writer.write("\n");
            writer.write("@ApiStatus.Internal public static void javagi$ensureInitialized() {}\n");

            // Generate constants
            for (Constant constant : namespace.constantList) {
                constant.generate(writer);
            }

            // Generate global functions
            for (Function function : namespace.functionList) {
                function.generate(writer);
            }

            // Generate registerTypes function
            writer.write("\n");
            writer.write("private static void registerTypes() {\n");
            writer.increaseIndent();

            // Classes
            for (Class c : namespace.classList) {
                writer.write("TypeCache.register(" + c.javaName + ".getType(), " + c.getConstructorString() + ");\n");
            }

            // Interfaces
            for (Interface i : namespace.interfaceList) {
                writer.write("TypeCache.register(" + i.javaName + ".getType(), " + i.getConstructorString() + ");\n");
            }

            // Aliases
            for (Alias a : namespace.aliasList) {
                if (a.getTargetType() == Alias.TargetType.CLASS) {
                    Class c = (Class) a.type.girElementInstance;
                    writer.write("TypeCache.register(" + a.javaName + ".getType(), " + c.getConstructorString() + ");\n");
                } else if (a.getTargetType() == Alias.TargetType.INTERFACE) {
                    Interface i = (Interface) a.type.girElementInstance;
                    writer.write("TypeCache.register(" + a.javaName + ".getType(), " + i.getConstructorString() + ");\n");
                }
            }

            writer.decreaseIndent();
            writer.write("}\n");

            writer.decreaseIndent();
            writer.write("}\n");
        }

        // Generate package-info.java file
        try (SourceWriter writer = new SourceWriter(Files.newBufferedWriter(basePath.resolve("package-info.java")))) {
            writer.write("/**\n");
            writer.write(" * This package contains the generated bindings for " + namespace.name + ".\n");
            writer.write(" * <p>\n");

            if (namespace.sharedLibrary != null) {
                writer.write(" * The following native libraries are required and will be loaded: ");
                for (String libraryName : namespace.sharedLibrary.split(",")) {
                    String fileName = libraryName;
                    // Strip path from library name
                    if (fileName.contains("/")) {
                        fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                    }
                    // Strip extension from library name
                    fileName = fileName.substring(0, fileName.lastIndexOf("."));
                    writer.write(" {@code " + fileName + "}");
                }
                writer.write("\n");
                writer.write(" * <p>\n");
            }
            writer.write(" * For namespace-global declarations, refer to the {@link " + className + "} class documentation.\n");

            if (platforms.size() < 3) {
                writer.write(" * <p>\n");
                writer.write(" * This package is only available on ");
                boolean first = true;
                for (var p : platforms) {
                    if (! first) {
                        writer.write(" and ");
                    }
                    writer.write(Conversions.toCamelCase(p.name, true));
                    first = false;
                }
                writer.write(".\n");
            }

            // Write docsections in the package-info documentation
            for (var docsection : namespace.docsectionList) {
                docsection.generate(writer);
            }

            writer.write(" */\n");
            writer.write("package " + namespace.packageName + ";\n");
        }
    }

    public void generateImportStatements(SourceWriter writer) throws IOException {
        if (module.repositories.containsKey("GObject")) {
            writer.write("import io.github.jwharm.javagi.gobject.*;\n");
            writer.write("import io.github.jwharm.javagi.gobject.types.*;\n");
        }
        writer.write("import io.github.jwharm.javagi.base.*;\n");
        writer.write("import io.github.jwharm.javagi.interop.*;\n");
        writer.write("import java.lang.foreign.*;\n");
        writer.write("import java.lang.invoke.*;\n");
        writer.write("import org.jetbrains.annotations.*;\n");
        writer.write("\n");
    }

    public Repository copy() {
        var copy = new Repository(module, generate);
        copy.namespace = namespace.copy();
        copy.package_ = package_;
        return copy;
    }
}
