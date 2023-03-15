package io.github.jwharm.javagi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import io.github.jwharm.javagi.generator.BindingsGenerator;
import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.CrossReference;
import io.github.jwharm.javagi.generator.GirParser;
import io.github.jwharm.javagi.generator.PatchSet;
import io.github.jwharm.javagi.model.Repository;

public class JavaGI {

    /**
     * Parse the provided GI source files, and generate the Java bindings
     * @param sources JavaGI.Source records with the GIR file path and name, Java package name, output location
     *                and patches to be applied
     * @throws Exception any exceptions that occur while parsing the GIR file and generating the bindings
     */
    public static Generated generate(Path outputDir, Source... sources) throws Exception {
        GirParser parser = new GirParser();

        Conversions.repositoriesLookupTable.clear();
        Map<String, Parsed> parsed = new HashMap<>();
        
        // Parse the GI files into Repository objects
        for (Source source : sources) {
            try {
                Repository r = parser.parse(source.source(), source.pkg());
                Conversions.repositoriesLookupTable.put(r.namespace.name, r);
                parsed.put(r.namespace.name, new Parsed(r, source.generate, source.natives, source.patches));
            } catch (IOException ioe) {
                System.out.println("Not found: " + source.source());
            }
        }
        
        // Link the type references to the GIR type definition across the GI repositories
        CrossReference.link();

        // Patches are specified in build.gradle.kts
        for (Parsed p : parsed.values()) {
            p.patches.patch(p.repository);
        }
        
        // Create lookup tables
        CrossReference.createIdLookupTable();
        CrossReference.createCTypeLookupTable();
        
        // Generate the Java class files
        Set<Generated.Element> namespaces = new LinkedHashSet<>();
        for (Parsed p : parsed.values()) {
            if (p.generate) {
                Path basePath = outputDir.resolve(p.repository.namespace.pathName);
                BindingsGenerator.generate(p.repository, p.natives, basePath);
                namespaces.add(new Generated.Element(p.repository.namespace.packageName));
            }
        }

        return new Generated(namespaces, outputDir);
    }

    /**
     * Source GIR file to parse
     */
    public record Source(Path source, String pkg, boolean generate, Set<String> natives, PatchSet patches) {}
    
    /**
     * Parsed GI repository
     */
    private record Parsed(Repository repository, boolean generate, Set<String> natives, PatchSet patches) {}

    /**
     * Generated set of Namespace elements in an output directory
     */
    public record Generated(Set<Element> elements, Path outputDir) {
        public record Element(String namespace) {}

        public void writeModuleInfo(String format) throws IOException {
            Files.writeString(outputDir.resolve("module-info.java"), format
                    .formatted(elements.stream()
                            .map(s -> "exports " + s.namespace + ";")
                            .collect(Collectors.joining("\n    "))
                    )
            );
        }
    }
}
