package io.github.jwharm.javagi;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * Change this to TRUE to display warnings about invalid types that are skipped 
     * by the GIR parser and the bindings generator.
     */
    public static final boolean DISPLAY_WARNINGS = false;

    /**
     * Run the JavaGI bindings generator as a command-line application instead of a Gradle task.
     * You will need to specify an XML file with the repository locations and package names, 
     * and an output folder location as command-line parameters.
     * See {@link #run(String, String)} for more information about the input file.
     * @param args Command-line parameters
     * @throws Exception Any exceptions that occur while parsing the GIR file and generating the bindings
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
     * @param inputFile An XML file with <repository> elements with attributes "path" and "package".
     * @param outputDir The directory in to write the Java files
     * @throws Exception Any exceptions that occur while parsing the GIR file and generating the bindings
     */
    public static void run(String inputFile, String outputDir) throws Exception {
        List<Source> toGenerate = new ArrayList<>();
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        parser.parse(inputFile, new DefaultHandler() {
            @Override
            public void startElement(String uri, String lName, String qName, Attributes attr) {
                if ("repository".equals(qName)) {
                    toGenerate.add(new Source(attr.getValue("path"), attr.getValue("package"), Set.of(), Path.of(outputDir), PatchSet.EMPTY));
                }
            }
        });
        generate(toGenerate.toArray(Source[]::new));
    }

    /**
     * Parse the provided GI source files, and generate the Java bindings
     * @param sources JavaGI.Source records with the GIR file path and name, Java package name, output location
     *                and patches to be applied
     * @throws Exception Any exceptions that occur while parsing the GIR file and generating the bindings
     */
    public static void generate(Source... sources) throws Exception {
        GirParser parser = new GirParser();
        BindingsGenerator generator = new BindingsGenerator();

        Map<String, Repository> repositories = new LinkedHashMap<>();
        Map<String, Parsed> parsed = new HashMap<>();
        
        // Parse the GI files into Repository objects
        for (Source source : sources) {
            System.out.println("PARSE " + source.path());
            Repository r = parser.parse(source.path(), source.pkg());
            repositories.put(r.namespace.name, r);
            parsed.put(r.namespace.name, new Parsed(r, source.natives, source.outputDir, source.patches));
        }
        
        // Link the type references to the GIR type definition across the GI repositories
        System.out.println("LINK " + parsed.size() + " REPOSITORIES");
        CrossReference.link(repositories);

        // Patches are specified in build.gradle.kts
        System.out.println("APPLY PATCHES");
        for (Parsed p : parsed.values()) {
            p.patches.patch(p.repository);
        }
        
        // Create lookup tables
        Conversions.cIdentifierLookupTable = CrossReference.createIdLookupTable(repositories);
        Conversions.cTypeLookupTable = CrossReference.createCTypeLookupTable(repositories);
        Conversions.repositoriesLookupTable = repositories;
        
        // Generate the Java class files
        for (Parsed p : parsed.values()) {
            Path basePath = p.outputDir.resolve(p.repository.namespace.pathName);
            System.out.println("GENERATE " + p.repository.namespace.name + " to " + basePath);
            generator.generate(p.repository, p.natives, basePath);
        }
    }

    public record Source(String path, String pkg, Set<String> natives, Path outputDir, PatchSet patches) {}
    private record Parsed(Repository repository, Set<String> natives, Path outputDir, PatchSet patches) {}
}
