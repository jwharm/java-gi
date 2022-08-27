package util;

import girparser.generator.BindingsGenerator;
import girparser.generator.BlacklistProcessor;
import girparser.generator.CrossReference;
import girparser.model.*;
import girparser.generator.GirParser;

import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {
        long starttime = System.currentTimeMillis();

        String[] girFiles = new String[] {
                // "GObject-2.0.gir"
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

        // These types are defined in the GIR, but unavailable by default
        String[] blacklist = new String[] {
                "Gsk.BroadwayRenderer",
                "Gsk.BroadwayRendererClass"
        };

        GirParser parser = new GirParser();
        BindingsGenerator generator = new BindingsGenerator();

        Map<String, Repository> repositories = new HashMap<>();

        for (String filename : girFiles) {
            String girfile = "gir-files/" + filename;
            System.out.println("PARSE " + girfile);
            Repository r = parser.parse(girfile);
            repositories.put(r.namespace.name, r);
        }

        System.out.println("LINK " + repositories.size() + " REPOSITORIES");
        CrossReference.link(repositories);

        System.out.println("REMOVE " + blacklist.length + " BLACKLISTED TYPES");
        for (Repository repository : repositories.values()) {
            BlacklistProcessor.filterBlacklistedTypes(repository, blacklist);
        }

        for (Repository repository : repositories.values()) {
            System.out.println("GENERATE " + repository.namespace.packageName);
            generator.generate(repository);
        }

        System.out.println("COMPLETED in " + (System.currentTimeMillis() - starttime) + " ms");
    }
}
