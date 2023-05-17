package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;

public class Docsection extends GirElement {

    public Docsection(GirElement parent, String name) {
        super(parent);
        this.name = name;
    }

    public void generate(SourceWriter writer) throws IOException {
        if (doc != null) {
            writer.write(" * \n");
            writer.write(" * <h2>");
            boolean first = true;
            for (var word : name.split("\\_")) {
                if (! first) {
                    writer.write(" ");
                }
                writer.write(Conversions.toCamelCase(word, true));
                first = false;
            }
            writer.write("</h2>\n");
            writer.write(" * \n");
            doc.generate(writer, false, false);
        }
    }
}
