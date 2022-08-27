package girparser.model;

import java.io.IOException;
import java.io.Writer;

public class Doc extends GirElement {
    public String space, contents;

    public Doc(GirElement parent, String space) {
        super(parent);
        this.space = space;
    }

    public void generate(Writer writer, int indent) throws IOException {
        if (contents == null || contents.length() == 0) {
            return;
        }
        writer.write(" ".repeat(indent * 4) + "/**\n");
        for (String line : contents.trim().lines().toList()) {
            String escapedLine = line.replace("\\", "\\\\");
            writer.write(" ".repeat(indent * 4) + " * " + escapedLine + "\n");
        }
        writer.write(" ".repeat(indent * 4) + " */\n");
    }
}
