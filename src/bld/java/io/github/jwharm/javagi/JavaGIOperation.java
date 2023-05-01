package io.github.jwharm.javagi;

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
public class JavaGIOperation extends AbstractOperation<JavaGIOperation> {
    private Path sourceDirectory_ = null;
    private Path outputDirectory_ = null;
    private final List<Source> sources_ = new ArrayList<>();
    private Platform platform_;
    private String moduleInfo_;

    /**
     * Performs the JavaGI operation.
     * @since 0.5
     */
    public void execute() throws Exception {
        Set<String> namespaces = new LinkedHashSet<>();
        boolean generated = false;

        // Parse gir files
        for (Parsed p : parse().values()) {
            if (p.generate) {
                // Generate bindings classes
                Path basePath = outputDirectory().resolve(p.repository.namespace.pathName);
                BindingsGenerator.generate(p.repository, p.natives, basePath);
                namespaces.add(p.repository.namespace.packageName);
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

    public Map<String, Parsed> parse() throws ParserConfigurationException, SAXException {
        GirParser parser = new GirParser(platform());
        Module module = new Module();
        Map<String, Parsed> parsed = new HashMap<>();

        // Parse the GI files into Repository objects
        for (Source source : sources()) {
            try {
                Repository r = parser.parse(sourceDirectory().resolve(source.fileName()), source.pkg(), module);
                module.repositoriesLookupTable.put(r.namespace.name, r);
                parsed.put(r.namespace.name, new Parsed(r, source.generate(), source.natives(), source.patches()));
            } catch (IOException ioe) {
                System.out.println("Not found: " + source.fileName());
            }
        }

        // Link the type references to the GIR type definition across the GI repositories
        module.link();

        // Patches are specified in build.gradle.kts
        for (Parsed p : parsed.values()) {
            if (p.patches != null) {
                p.patches.patch(p.repository);
            }
        }

        // Create lookup tables
        module.createIdLookupTable();
        module.createCTypeLookupTable();

        return parsed;
    }

    /**
     * Source GIR file to parse
     */
    public record Source(String fileName, String pkg, boolean generate, Set<String> natives, PatchSet patches) {}

    /**
     * Parsed GI repository
     */
    public record Parsed(Repository repository, boolean generate, Set<String> natives, PatchSet patches) {}

    /**
     * Provides the source directory that will be used for the JavaGI operation.
     * @param directory the source directory
     * @return this operation instance
     * @since 0.5
     */
    public JavaGIOperation sourceDirectory(Path directory) {
        sourceDirectory_ = directory;
        return this;
    }

    /**
     * Provides the output directory where all output is generated.
     * @param directory the output directory
     * @return this operation instance
     * @since 0.5
     */
    public JavaGIOperation outputDirectory(Path directory) {
        outputDirectory_ = directory;
        return this;
    }

    /**
     * Provides the platform for which bindings are generated.
     * @param platform the platform
     * @return this operation instance
     * @since 0.5
     */
    public JavaGIOperation platform(Platform platform) {
        platform_ = platform;
        return this;
    }

    /**
     * Provides the contents for the module-info.java file.
     * @param moduleInfo the module-info contents
     * @return this operation instance
     * @since 0.5
     */
    public JavaGIOperation moduleInfo(String moduleInfo) {
        moduleInfo_ = moduleInfo;
        return this;
    }

    /**
     * Provides the sources for which bindings are generated.
     * @param sources the sources
     * @return this operation instance
     * @since 0.5
     */
    public JavaGIOperation sources(Source... sources) {
        return sources(Arrays.asList(sources));
    }

    /**
     * Provides a list of source for which bindings are generated.
     * @param sources the sources
     * @return this operation instance
     * @since 0.5
     */
    public JavaGIOperation sources(List<Source> sources) {
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
    public JavaGIOperation source(String file, String pkg, boolean generate, Set<String> natives, PatchSet patches) {
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
     * Retrieves the platform for which bindings are generated.
     * @return the platform
     * @since 0.5
     */
    public Platform platform() {
        return platform_;
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