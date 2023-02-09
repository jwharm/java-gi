package io.github.jwharm.javagi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import io.github.jwharm.javagi.generator.BindingsGenerator;
import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.CrossReference;
import io.github.jwharm.javagi.generator.GirParser;
import io.github.jwharm.javagi.generator.PatchSet;
import io.github.jwharm.javagi.model.Repository;

public class JavaGI {

    /**
     * Run the JavaGI bindings generator as a command-line application instead of a Gradle task.
     * You will need to specify an XML file with the repository locations and package names, 
     * and an output folder location as command-line parameters.
     * See {@link #run(String, String)} for more information about the input file.
     * @param args command-line parameters
     * @throws Exception any exceptions that occur while parsing the GIR file and generating the bindings
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("ERROR: No input file provided.");
            return;
        }
        String inputFile = args[0];
        
        if (args.length == 1) {
            System.err.println("ERROR: No output directory provided.");
            return;
        }
        String outputDir = args[1];

        run(inputFile, outputDir);
    }

    /**
     * Run the JavaGI bindings generator with an XML input file.
     * @param inputFile an XML file with &#60;repository&#62; elements with attributes "path" and "package".
     * @param outputDir the directory in to write the Java files
     * @throws Exception any exceptions that occur while parsing the GIR file and generating the bindings
     */
    public static void run(String inputFile, String outputDir) throws Exception {
        List<Source> toGenerate = new ArrayList<>();
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        parser.parse(inputFile, new DefaultHandler() {
            @Override
            public void startElement(String uri, String lName, String qName, Attributes attr) {
                if ("repository".equals(qName)) {
                    toGenerate.add(new Source(Path.of(attr.getValue("path")), attr.getValue("package"), true, Set.of(), PatchSet.EMPTY));
                }
            }
        });
        generate(Path.of(outputDir), toGenerate.toArray(Source[]::new));
    }

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
                System.out.println("PARSE " + source.source());
                Repository r = parser.parse(source.source(), source.pkg());
                Conversions.repositoriesLookupTable.put(r.namespace.name, r);
                parsed.put(r.namespace.name, new Parsed(r, source.generate, source.natives, source.patches));
            } catch (IOException ioe) {
                System.err.printf("PARSE %s: NOT FOUND\n", source.source());
            }
        }
        
        // Link the type references to the GIR type definition across the GI repositories
        System.out.println("LINK " + parsed.size() + " REPOSITORIES");
        CrossReference.link();

        // Patches are specified in build.gradle.kts
        System.out.println("APPLY PATCHES");
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
                System.out.println("GENERATE " + p.repository.namespace.name + " to " + basePath);
                BindingsGenerator.generate(p.repository, p.natives, basePath);
                namespaces.add(new Generated.Element(p.repository.namespace.packageName));
            }
        }

        return new Generated(namespaces, outputDir);
    }

    public record Source(Path source, String pkg, boolean generate, Set<String> natives, PatchSet patches) {}
    private record Parsed(Repository repository, boolean generate, Set<String> natives, PatchSet patches) {}

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
