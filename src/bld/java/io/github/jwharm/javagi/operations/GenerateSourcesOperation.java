package io.github.jwharm.javagi.operations;

import io.github.jwharm.javagi.generator.*;
import io.github.jwharm.javagi.model.Module;
import io.github.jwharm.javagi.model.Repository;
import org.xml.sax.SAXException;
import rife.bld.operations.AbstractOperation;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates Java sources from GIR files
 *
 * @since 0.5
 */
public class GenerateSourcesOperation extends AbstractOperation<GenerateSourcesOperation> {
    private Path sourceDirectory_ = null;
    private Path outputDirectory_ = null;
    private final List<Source> sources_ = new ArrayList<>();
    private String moduleInfo_;

    /**
     * Performs the JavaGI operation.
     * @since 0.5
     */
    public void execute() throws Exception {
        Set<String> namespaces = new LinkedHashSet<>();
        boolean generated = false;

        // Parse all platform-specific gir files
        Module windows = parse(Platform.WINDOWS);
        Module linux = parse(Platform.LINUX);
        Module macos = parse(Platform.MACOS);

        // Merge the gir repositories into one cross-platform repository
        Module module = new Merge().merge(windows, linux, macos);

        // Generate bindings classes
        for (Repository repository : module.repositories.values()) {
            if (repository.generate) {
                Path basePath = outputDirectory().resolve(repository.namespace.pathName);
                repository.generate(basePath);
                namespaces.add(repository.namespace.packageName);
                generated = true;
            }
        }

        // Write module-info.java
        Files.writeString(outputDirectory().resolve("module-info.java"), moduleInfo()
                .formatted(namespaces.stream()
                        .map(s -> "exports " + s + ";")
                        .collect(Collectors.joining("\n    "))
                )
        );

        if (! silent()) {
            System.out.println(generated ? "Bindings generated successfully." : "No bindings were generated.");
        }
    }

    public Module parse(Platform platform) throws ParserConfigurationException, SAXException {
        Module module = new Module(platform);
        GirParser parser = new GirParser(module);
        Path girPath = sourceDirectory().resolve(platform.name().toLowerCase());
        if (! girPath.toFile().exists()) {
            System.out.println("Not found: " + girPath);
            return null;
        }

        // Parse the GI files into Repository objects
        for (var source : sources()) {
            try {
                // Parse the file
                Repository r = parser.parse(girPath.resolve(source.fileName), source.pkg);
                r.generate = source.generate;
                r.natives = source.natives;

                // Add the repository to the module
                module.repositories.put(r.namespace.name, r);

                // Flag unsupported va_list methods so they will not be generated
                module.flagVaListFunctions();

                // Apply patches
                if (source.patches != null) {
                    source.patches.patch(r);
                }

            } catch (IOException ioe) {
                System.out.println("Not found: " + source.fileName);
            }
        }

        // Link the type references to the GIR type definition across the GI repositories
        module.link();

        return module;
    }

    /**
     * Source GIR file to parse
     */
    public record Source(String fileName, String pkg, boolean generate, Set<String> natives, Patch patches) {}

    /**
     * Provides the source directory that will be used for the JavaGI operation.
     * @param directory the source directory
     * @return this operation instance
     * @since 0.5
     */
    public GenerateSourcesOperation sourceDirectory(Path directory) {
        sourceDirectory_ = directory;
        return this;
    }

    /**
     * Provides the output directory where all output is generated.
     * @param directory the output directory
     * @return this operation instance
     * @since 0.5
     */
    public GenerateSourcesOperation outputDirectory(Path directory) {
        outputDirectory_ = directory;
        return this;
    }

    /**
     * Provides the contents for the module-info.java file.
     * @param moduleInfo the module-info contents
     * @return this operation instance
     * @since 0.5
     */
    public GenerateSourcesOperation moduleInfo(String moduleInfo) {
        moduleInfo_ = moduleInfo;
        return this;
    }

    /**
     * Provides the sources for which bindings are generated.
     * @param sources the sources
     * @return this operation instance
     * @since 0.5
     */
    public GenerateSourcesOperation sources(Source... sources) {
        return sources(Arrays.asList(sources));
    }

    /**
     * Provides a list of source for which bindings are generated.
     * @param sources the sources
     * @return this operation instance
     * @since 0.5
     */
    public GenerateSourcesOperation sources(List<Source> sources) {
        sources_.addAll(sources);
        return this;
    }

    /**
     * Create a new Source
     * @param file the gir filename
     * @param pkg the package name
     * @param generate whether to generate bindings for this source
     * @param natives the names of native libraries
     * @param patches patches to apply before generating bindings
     */
    public GenerateSourcesOperation source(String file, String pkg, boolean generate, Set<String> natives, Patch patches) {
        sources(new Source(file, pkg, generate, natives, patches));
        return this;
    }

    /**
     * Retrieves the source directory that will be used for the
     * JavaGI operation.
     * @return the source directory, or {@code null} if the directory
     * wasn't specified.
     * @since 0.5
     */
    public Path sourceDirectory() {
        return sourceDirectory_;
    }

    /**
     * Retrieves the list of sources that will be used for the
     * JavaGI operation.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     * @return the source files
     * @since 0.5
     */
    public List<Source> sources() {
        return sources_;
    }

    /**
     * Retrieves the output directory where all output is generated.
     * @return the output directory, or {@code null} if the directory
     * wasn't specified.
     * @since 0.5
     */
    public Path outputDirectory() {
        return outputDirectory_;
    }

    /**
     * Retrieves the provided module-info.java contents.
     * @return the provided module-info.java contents
     * @since 0.5
     */
    public String moduleInfo() {
        return moduleInfo_;
    }
}