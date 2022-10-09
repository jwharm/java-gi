package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.GtkDoc;

import java.io.IOException;
import java.io.Writer;

public class Doc extends GirElement {
    public final String space;
    public String contents;

    public Doc(GirElement parent, String space) {
        super(parent);
        this.space = space;
    }

    public void generate(Writer writer, int indent) throws IOException {
        if (contents == null || contents.length() == 0) {
            return;
        }
        String javadoc = GtkDoc.getInstance().convert(this);
        writer.write(" ".repeat(indent * 4) + "/**\n");
        for (String line : javadoc.trim().lines().toList()) {
            String escapedLine = line.replace("\\", "\\\\");
            writer.write(" ".repeat(indent * 4) + " * " + escapedLine + "\n");
        }
        writer.write(" ".repeat(indent * 4) + " */\n");
    }
}
