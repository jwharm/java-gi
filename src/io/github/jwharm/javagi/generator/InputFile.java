package io.github.jwharm.javagi.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class InputFile {
    
    public record Line (String path, String pkg) {}
    
    public final List<Line> lines;
    
    public InputFile(String name) throws Exception {
        lines = new ArrayList<>();
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        parser.parse(name, new DefaultHandler() {
            @Override
            public void startElement(String uri, String lName, String qName, Attributes attr) {
                if ("repository".equals(qName)) {
                    lines.add(new Line(attr.getValue("path"), attr.getValue("package")));
                }
            }
        });
    }
}
