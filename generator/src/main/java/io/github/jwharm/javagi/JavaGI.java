package io.github.jwharm.javagi;

import io.github.jwharm.javagi.generator.*;
import io.github.jwharm.javagi.model.Repository;

import java.util.HashMap;
import java.util.Map;

public class JavaGI {

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

    public static void run(String inputFile, String outputDir) throws Exception {
        long starttime = System.currentTimeMillis();
        InputFile input = new InputFile(inputFile);
        if (!(outputDir.endsWith("/") || outputDir.endsWith("\\"))) {
            outputDir = outputDir + "/";
        }

        GirParser parser = new GirParser();
        BindingsGenerator generator = new BindingsGenerator();

        Map<String, Repository> repositories = new HashMap<>();

        for (InputFile.Line inputLine : input.lines) {
            System.out.println("PARSE " + inputLine.path());
            Repository r = parser.parse(inputLine.path(), inputLine.pkg());
            repositories.put(r.namespace.name, r);
        }

        System.out.println("LINK " + repositories.size() + " REPOSITORIES");
        CrossReference.link(repositories);
        Conversions.cIdentifierLookupTable = CrossReference.createIdLookupTable(repositories);
        Conversions.cTypeLookupTable = CrossReference.createCTypeLookupTable(repositories);

        System.out.println("APPLY PATCHES");
        RepositoryEditor.applyPatches(repositories);

        for (Repository repository : repositories.values()) {
            System.out.println("GENERATE " + repository.namespace.name
                    + " to " + outputDir + repository.namespace.pathName);
            generator.generate(repository, outputDir);
        }

        System.out.println("COMPLETED in " + (System.currentTimeMillis() - starttime) + " ms");
    }
}
