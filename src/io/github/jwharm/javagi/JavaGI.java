package io.github.jwharm.javagi;

import io.github.jwharm.javagi.generator.*;
import io.github.jwharm.javagi.model.Repository;

import java.util.HashMap;
import java.util.Map;

public class JavaGI {

    public static void main(String[] args) throws Exception {
        long starttime = System.currentTimeMillis();

        String[] girFiles = new String[] {
                "GObject-2.0.gir",
                "GLib-2.0.gir",
                "Gio-2.0.gir",
                "Gdk-4.0.gir",
                "GdkPixbuf-2.0.gir",
                "Gsk-4.0.gir",
                "HarfBuzz-0.0.gir",
                "cairo-1.0.gir",
                "Pango-1.0.gir",
                "Graphene-1.0.gir",
                "Gtk-4.0.gir"
        };

        GirParser parser = new GirParser();
        BindingsGenerator generator = new BindingsGenerator();

        Map<String, Repository> repositories = new HashMap<>();

        for (String filename : girFiles) {
            String girfile = "/usr/share/gir-1.0/" + filename;
            System.out.println("PARSE " + girfile);
            Repository r = parser.parse(girfile);
            repositories.put(r.namespace.name, r);
        }

        System.out.println("LINK " + repositories.size() + " REPOSITORIES");
        CrossReference.link(repositories);
        Conversions.cIdentifierLookupTable = CrossReference.createIdLookupTable(repositories);

        System.out.println("APPLY PATCHES");
        RepositoryEditor.applyPatches(repositories);

        for (Repository repository : repositories.values()) {
            System.out.println("GENERATE " + repository.namespace.packageName);
            generator.generate(repository);
        }

        System.out.println("COMPLETED in " + (System.currentTimeMillis() - starttime) + " ms");
    }
}
