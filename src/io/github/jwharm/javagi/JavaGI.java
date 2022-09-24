package io.github.jwharm.javagi;

import io.github.jwharm.javagi.generator.*;
import io.github.jwharm.javagi.model.Repository;

import java.util.HashMap;
import java.util.Map;

public class JavaGI {

    public static void main(String[] args) throws Exception {
        long starttime = System.currentTimeMillis();
        
        if (args.length == 0) {
            System.err.println("ERROR: No input file provided.");
            return;
        }
        
        InputFile inputFile = new InputFile(args[0]);
        
        GirParser parser = new GirParser();
        BindingsGenerator generator = new BindingsGenerator();

        Map<String, Repository> repositories = new HashMap<>();

        for (InputFile.Line inputLine : inputFile.lines) {
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
            System.out.println("GENERATE " + repository.namespace.packageName);
            generator.generate(repository);
        }

        System.out.println("COMPLETED in " + (System.currentTimeMillis() - starttime) + " ms");
    }
}
