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
        writer.write(" ".repeat(indent * 4) + "/**\n");
        
        // Convert to javadoc
        String javadoc = GtkDoc.getInstance().convert(this);
        
        // Write doc
        writeDoc(writer, indent, javadoc, null);
        
        // Deprecated
        if (parent instanceof Method m && "1".equals(m.deprecated)) {
        	if (parent.docDeprecated != null) {
        		String deprecatedJavadoc = GtkDoc.getInstance().convert(parent.docDeprecated);
        		writeDoc(writer, indent, deprecatedJavadoc, "@deprecated");
        	}
        }
        
        writer.write(" ".repeat(indent * 4) + " */\n");
    }
    
    private void writeDoc(Writer writer, int indent, String javadoc, String tag) throws IOException {
    	int count = 0;
        for (String line : javadoc.trim().lines().toList()) {
            String escapedLine = line.replace("\\", "\\\\");
            // Write tag (optional)
        	if (count == 0 && tag != null) {
        		escapedLine = tag + " " + escapedLine;
        	}
            writer.write(" ".repeat(indent * 4) + " * " + escapedLine + "\n");
            count++;
        }
    }
}
