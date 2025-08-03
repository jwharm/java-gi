/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 the Java-GI developers
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

package org.javagi;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.javagi.configuration.LicenseNotice;
import org.javagi.configuration.ModuleInfo;
import org.javagi.gir.*;
import org.javagi.util.Platform;
import org.javagi.generators.*;
import org.javagi.gir.Class;
import org.javagi.gir.Record;
import picocli.CommandLine;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.javagi.util.Platform.*;
import static java.nio.file.StandardOpenOption.*;
import static java.util.stream.Collectors.joining;

/**
 * Main class for the {@code java-gi} command-line utility.
 * <p>
 * The command-line arguments are processed with Picocli. The tool loads the
 * included gir files into a {@link Library}, and then generates Java bindings
 * for one or more gir files. With an optional argument, a complete Gradle
 * project structure is generated.
 * <p>
 * The {@link #generate} method is used by the Gradle build scripts as well (see
 * the {@code GenerateSources} class in the {@code buildSrc} folder).
 */
@CommandLine.Command(
        name = "java-gi",
        mixinStandardHelpOptions = true,
        version = "${app.version}",
        description = "Generate Java bindings from GObject-Introspection repository (gir) files.")
public class JavaGI implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-d", "--domain"},
            paramLabel = "domain",
            description = "reverse domain name prefixed to the Java package " +
                    "and module name, for example \"org.gnome\"")
    private String domain;

    @CommandLine.Option(
            names = {"-o", "--output"},
            paramLabel = "dir",
            defaultValue = ".",
            description = "output directory, default: current working directory")
    private File outputDirectory;

    @CommandLine.Option(
            names = {"-p", "--project"},
            description = "generate Gradle project structure and build scripts"
    )
    private boolean generateProject;

    @CommandLine.Option(
            names = {"-S", "--stacktrace"},
            description = "write a stacktrace to stderr for all exceptions"
    )
    private boolean stacktrace;

    @CommandLine.Option(
            names = {"-s", "--summary"},
            paramLabel = "text",
            description = "short summary of the library to include in the " +
                    "javadoc of the generated Java package")
    private String summary;

    @CommandLine.Option(
            names = {"-u", "--doc-url"},
            paramLabel = "url",
            defaultValue = "",
            description = "url of the online API documentation to prefix before " +
                    "hyperlinks in the generated javadoc")
    private String docUrl;

    @CommandLine.Parameters(
            arity = "1..*",
            description = "one or more gir files to process")
    private File[] girFiles;

    // List of generated subprojects (one for each gir file)
    private final List<String> subprojects = new ArrayList<>();

    // The gir parser
    private final GirParser parser = GirParser.getInstance();

    // The runtime platform is assumed to be the target platform
    private final int platform = Platform.getRuntimePlatform();

    /**
     * Overrides error output and redirects to {@link #call}
     *
     * @param args processed by picocli
     */
    public static void main(String[] args) {
        var javaGi = new JavaGI();
        int exitCode = new CommandLine(javaGi)
                .setExecutionExceptionHandler(javaGi::writeErrorMessages)
                .execute(args);
        System.exit(exitCode);
    }

    /**
     * When "--stacktrace" is passed on the command line, the exception is
     * rethrown. Otherwise, this will print the exception message on the
     * command line, without the stack trace.
     */
    private int writeErrorMessages(Exception ex,
                                   CommandLine cmd,
                                   CommandLine.ParseResult result) throws Exception {
        if (stacktrace)
            throw ex;

        String message = Objects.requireNonNullElse(
                ex.getMessage(),
                ex.getClass().getSimpleName());

        // bold red error message
        cmd.getErr().println(cmd.getColorScheme().errorText(message));

        return cmd.getExitCodeExceptionMapper() != null
                ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                : cmd.getCommandSpec().exitCodeOnExecutionException();
    }

    /**
     * Runs the bindings generator from the command-line arguments.
     *
     * @return status code (0 = success)
     * @throws Exception all exceptions are handled (reported) by picocli
     */
    @Override
    public Integer call() throws Exception {
        // Do not generate runtime platform checks
        Platform.GENERATE_PLATFORM_CHECKS = false;

        // Load included gir files
        var library = loadIncludedGirFiles();

        // Ensure that at least GLib gir file is present
        library.lookupNamespace("GLib"); // throws exception when not found

        // Parse the gir files for which bindings will be generated
        for (var girFile : girFiles) {
            var repository = parser.parse(girFile, platform, null);

            // Check if parsing succeeded (the gir file contains a namespace)
            if (repository == null || repository.namespace() == null)
                throw new IllegalArgumentException("gir file %s is invalid"
                        .formatted(girFile.getName()));

            // Prepare module and package information
            var name = repository.namespace().name();
            var version = repository.namespace().version();
            var packageName = generatePackageName(name);
            ModuleInfo.add(name, packageName, packageName, docUrl, summary);
            library.put(girFile.getName(), repository);

            // Create a directory for each module
            var libDirectory = new File(outputDirectory, name.toLowerCase());

            // No custom packages to export in module-info.java
            var packages = new HashSet<String>();

            // Create standard source directories for maven/gradle projects
            var srcDirectory = generateProject
                    ? new File(libDirectory, "src/main/java")
                    : libDirectory;
            var ignored = srcDirectory.mkdirs();

            // Generate the language bindings
            generate(name, version, library, packages, srcDirectory);

            if (generateProject) {
                // Generate build.gradle script
                writeBuildScript(libDirectory.toPath(), repository);
                subprojects.add(name.toLowerCase());
            }
        }

        if (generateProject) {
            // Generate settings.gradle script
            writeSettingsScript(subprojects);
        }

        return 0;
    }

    private Library loadIncludedGirFiles()
            throws XMLStreamException, IOException {
        var library = new Library();
        ZipEntry entry;

        try (var resource = getClass().getResourceAsStream("/gir-files.zip")) {
            if (resource == null)
                throw new FileNotFoundException("gir-files.zip resource not found");

            var zipIn = new ZipInputStream(resource);
            while ((entry = zipIn.getNextEntry()) != null) {
                var name = entry.getName();
                if (!entry.isDirectory() && name.endsWith(".gir")) {
                    var platform = name.startsWith("windows/") ? WINDOWS
                            : name.startsWith("macos/") ? MACOS
                            : LINUX;
                    var file = name.substring(name.lastIndexOf("/") + 1);
                    var repo = parser.parse(zipIn, platform, library.get(file));
                    library.put(file, repo);
                }
            }
        }

        return library;
    }

    private String generatePackageName(String namespace) {
        var ns = namespace.toLowerCase();
        if (domain == null || domain.isBlank())
            return ns;
        if (domain.endsWith("."))
            return domain + ns;
        else
            return domain + "." + ns;
    }

    // Generate Java language bindings for a GIR repository
    public static void generate(String name,
                                String version,
                                Library library,
                                Set<String> packages,
                                File outputDirectory) throws IOException {

        Namespace ns = library.lookupNamespace(name);
        String packageName = ModuleInfo.packageName(name);

        // Generate class with namespace-global constants and functions
        var typeSpec = new NamespaceGenerator(ns).generateGlobalsClass();
        writeJavaFile(typeSpec, packageName, outputDirectory);

        // Generate package-info.java
        Path path = outputDirectory.toPath()
                .resolve(packageName.replace('.', File.separatorChar))
                .resolve("package-info.java");
        String packageInfo = new PackageInfoGenerator(ns).generate();
        Files.writeString(path, packageInfo, CREATE, WRITE, TRUNCATE_EXISTING);

        // Generate module-info.java
        path = outputDirectory.toPath().resolve("module-info.java");
        String moduleInfo = new ModuleInfoGenerator(ns, packages).generate();
        Files.writeString(path, moduleInfo, CREATE, WRITE, TRUNCATE_EXISTING);

        // Generate classes for all registered types in this namespace
        for (var rt : ns.registeredTypes().values()) {

            // Do not generate record types named "...Private" (except for
            // GPrivate) or types that are custom implemented in java-gi
            if (rt.skipJava() || rt.customJava())
                continue;

            typeSpec = switch(rt) {
                case Alias a -> new AliasGenerator(a).generate();
                case Boxed b -> new BoxedGenerator(b).generate();
                case Callback c -> new CallbackGenerator(c).generate();
                case Class c -> new ClassGenerator(c).generate();
                case EnumType e -> new EnumGenerator(e).generate();
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

    private void writeBuildScript(Path basePath,
                                  Repository repository) throws IOException {
        // List the project dependencies
        var dependencies = new HashSet<String>();
        for (var incl : repository.includes()) {
            var name = incl.name().toLowerCase();
            String dep = generateDependencyLine(name);
            dependencies.add(dep);
        }

        // Generate the build script
        String script = """
                plugins {
                    id("java-library")
                }
                
                repositories {
                    mavenCentral()
                }
                
                java {
                    toolchain {
                        languageVersion = JavaLanguageVersion.of(22)
                    }
                }
                
                dependencies {
                    compileOnly("org.jetbrains:annotations:26.0.1")
                %s
                }
                """.formatted(String.join("\n", dependencies));
        Path file = basePath.resolve("build.gradle");
        Files.writeString(file, script, CREATE, WRITE, TRUNCATE_EXISTING);
    }

    private String generateDependencyLine(String name) {
        if (name.equals("cairo")) {
            return "    api(\"io.github.jwharm.cairobindings:cairo:1.18.4.1\")";
        }
        else if (ModuleInfo.INCLUDED_MODULES.containsKey(name)) {
            String version = System.getProperty("app.version");
            return "    api(\"io.github.jwharm.javagi:" + name + ":" + version + "\")";
        } else {
            return "    api(project(\":" + name + "\"))";
        }
    }

    private void writeSettingsScript(List<String> subprojects) throws IOException {
        String script = subprojects.stream()
                .map(s -> "include(\"" + s + "\")")
                .collect(joining("\n", "", "\n"));
        Path file = outputDirectory.toPath().resolve("settings.gradle");
        Files.writeString(file, script, CREATE, WRITE, TRUNCATE_EXISTING);
    }
}
