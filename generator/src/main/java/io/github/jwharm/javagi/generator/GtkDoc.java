package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.Doc;
import io.github.jwharm.javagi.model.GirElement;
import io.github.jwharm.javagi.model.RegisteredType;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GtkDoc {

    // This is one huge regular expression that basically matches a series of separate 
    // regex patters, each with a named group, separated by an "or" ("|") operand.
    // Most of the individual patterns should be relatively self-explanatory.
    private static final String REGEX_PASS_1 =
            // ``` multiline code block ```
              "(?<codeblock>```(?<content>(?s).+?)```)"
            // |[ multiline code block ]|
            + "|(?<codeblock2>\\|\\[(?<content2>(?s).+?)\\]\\|)"
            // `code`
            + "|(?<code>`[^`]+`)"
            // [link]
            + "|(?<link>\\[(?<type>.+)@(?<path>(?<part1>[^\\.\\]]+)?\\.?(?<part2>[^\\.\\]\\:]+)?[\\.\\:]?(?<part3>.+)?)\\])"
            // %macro reference
            + "|(?<macroref>\\%\\w+)"
            // #type
            + "|(?<typeref>\\#[^\\#\\s]\\w+)"
            // @param
            + "|(?<paramref>\\@[^\\@\\s]\\w+)"
            // [url](for link)
            + "|(?<hyperlink>\\[(?<desc>.+)\\]\\((?<url>.+)\\))"
            // ! [url](for image)
            + "|(?<img>\\!\\[(?<imgdesc>.+)\\]\\((?<imgurl>.+)\\))"
            // # Header level 1, ## Header level 2 etc
            + "|(?m)^(?<header>(?<headerlevel>#{1,6})\\s.+)\\n*"
            // - Bullet lists, we generate <ul> for the first bullet and <li> for every bullet
            + "|(?m)^\\s*(?<bulletpoint>\\-)\\s"
            // Match *strong*
            + "|(?<strong>\\*.+\\*)"
            // Match entities: <, >, &
            + "|(?<entity>\\<|\\>|\\&)"
            // Match multiple newlines
            + "|(?<p>\\n{2,})"
    ;

    // These are the named groups for which the conversions to Javadoc are applied.
    // Other named groups are not matched separately, but only used as parameters 
    // for the conversion functions.
    private static final String[] NAMED_GROUPS_PASS_1 = new String[] {
            "codeblock",
            "codeblock2",
            "code",
            "link",
            "macroref",
            "typeref",
            "paramref",
            "hyperlink",
            "img",
            "header",
            "bulletpoint",
            "strong",
            "entity",
            "p"
    };
    
    private static final String REGEX_PASS_2 =
            // <p> immediately followed by <pre>
              "(?<emptyp>\\<p\\>\\s*(?<tag>\\<(pre|ul)\\>))"
    ;
    
    private static final String[] NAMED_GROUPS_PASS_2 = new String[] {
    		"emptyp"
    };

    private static GtkDoc instance = null;
    private Doc doc;
    private boolean ul;

    /**
     * Return the singleton instance of the GtkDoc class.
     */
    public static GtkDoc getInstance() {
        if (instance == null) {
            instance = new GtkDoc();
        }
        return instance;
    }

    private final Pattern patternPass1, patternPass2;

    // This class is a singleton. The regex patterns are compiled only once.
    private GtkDoc() {
        this.patternPass1 = Pattern.compile(REGEX_PASS_1);
        this.patternPass2 = Pattern.compile(REGEX_PASS_2);
    }

    /**
     * Convert the GtkDoc comments into Javadoc as best as possible.
     */
    public String convert(Doc doc) {
        this.doc = doc;
        this.ul = false;
        
        // Conversion pass 1
        Matcher matcher = patternPass1.matcher(doc.contents);
        StringBuilder output = new StringBuilder();
        while (matcher.find()) {
        	String groupName = getMatchedGroupName(matcher, NAMED_GROUPS_PASS_1);
            String replacement = convert(matcher, groupName);
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        if (ul) {
            ul = false;
            output.append("\n</ul>");
        }
        String pass1Result = output.toString();
        
        // Conversion pass 2
        matcher = patternPass2.matcher(pass1Result);
        output = new StringBuilder();
        while (matcher.find()) {
        	String groupName = getMatchedGroupName(matcher, NAMED_GROUPS_PASS_2);
            String replacement = convert(matcher, groupName);
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        String pass2Result = output.toString();
        
        return pass2Result;
    }
    
    private String getMatchedGroupName(Matcher matcher, String[] groupNames) {
        return Arrays.stream(groupNames)
                .filter((name) -> matcher.group(name) != null)
                .findFirst()
                .orElseThrow();
    }

    // Determine which matching group was matched, and apply the relevant conversion
    private String convert(Matcher matcher, String groupName) {

        return switch(groupName) {
        	// Pass 1 group names
            case "codeblock" -> convertCodeblock(matcher.group(), matcher.group("content"));
            case "codeblock2" -> convertCodeblock(matcher.group(), matcher.group("content2"));
            case "code" -> convertCode(matcher.group());
            case "link" -> convertLink(matcher.group(),
                    matcher.group("type"), matcher.group("path"),
                    matcher.group("part1"), matcher.group("part2"), matcher.group("part3"));
            case "macroref" -> convertMacroref(matcher.group());
            case "typeref" -> convertTyperef(matcher.group());
            case "paramref" -> convertParamref(matcher.group());
            case "hyperlink" -> convertHyperlink(matcher.group(),
                    matcher.group("desc"), matcher.group("url"));
            case "img" -> convertImg(matcher.group(),
                    matcher.group("imgdesc"), matcher.group("imgurl"));
            case "header" -> convertHeader(matcher.group(), matcher.group("headerlevel"));
            case "bulletpoint" -> convertBulletpoint(matcher.group());
            case "strong" -> convertStrong(matcher.group());
            case "entity" -> convertEntity(matcher.group());
            case "p" -> convertP(matcher.group());
            
            // Pass 2 group names
            case "emptyp" -> convertEmptyP(matcher.group(), matcher.group("tag"));
            
            default -> matcher.group();
        };
    }

    // Replace multi-line code blocks (starting and ending with ```) with <pre>{@code ... }</pre> blocks
    private String convertCodeblock(String codeblock, String content) {
        return "<pre>{@code " + content + "}</pre>";
    }

    // Replace `text` with {@code text}
    private String convertCode(String code) {
        return "{@code " + code.substring(1, code.length() - 1) + "}";
    }

    // Replace [...] links with {@link ...} links
    private String convertLink(String link, String type, String path,
                               String part1, String part2, String part3) {
        String name;
        switch (type) {
            case "ctor":
                if ("new".equals(part3)) {
                    return "{@link " + formatNS(part1) + part2 + "#" + part2 + "}";
                }
            case "method":
            case "vfunc":
                if (part3 == null) {
                    return "{@link " + part1 + formatMethod(part2) + "}";
                } else {
                    return "{@link " + formatNS(part1) + part2 + formatMethod(part3) + "}";
                }
            case "property":
                return "{@code " + path + "}";
            case "func":
                return "{@link " + part1 + formatMethod(part2) + "}";
            case "class":
                name = (part2 == null) ? part1 : (formatNS(part1) + part2);
                return "{@link " + name + "}";
            case "id":
                GirElement girElement = Conversions.cIdentifierLookupTable.get(part1);
                name = girElementToString(girElement, false);
                return (name == null) ? ("{@code " + path + "}") : ("{@link " + name + "}");
            default:
                return "{@code " + path + "}";
        }
    }

    // Replace %NULL, %TRUE and %FALSE with {@code true} etc, or %text with {@link text}
    private String convertMacroref(String ref) {
        switch (ref) {
            case "%NULL":   return "{@code null}";
            case "%TRUE":   return "{@code true}";
            case "%FALSE":  return "{@code false}";
        }
        GirElement girElement = Conversions.cIdentifierLookupTable.get(ref.substring(1));
        String name = girElementToString(girElement, true);
        if (name == null) {
            return "{@code " + ref.substring(1) + "}";
        } else {
            return "{@link " + name + "}";
        }
    }

    // Replace #text with {@link text}
    private String convertTyperef(String ref) {
        RegisteredType rt = Conversions.cTypeLookupTable.get(ref.substring(1));
        if (rt == null) {
            return "{@code " + ref.substring(1) + "}";
        } else {
            return "{@link " + formatNS(rt.getNamespace().name) + rt.javaName + "}";
        }
    }

    // Replace @text with {@code text}
    private String convertParamref(String ref) {
        return "{@code " + ref.substring(1) + "}";
    }

    // Replace "[...](...)" with <a href="...">...</a>
    private String convertHyperlink(String link, String desc, String url) {
        return "<a href=\"" + url + "\">" + desc + "</a>";
    }

    // Replace "! [...](...)" image links with <img src="..." alt="...">
    private String convertImg(String img, String desc, String url) {
        String alt = desc.replace("\"", "\\\"");
        return "<img src=\"./doc-files/" + url + "\" alt=\"" + alt + "\">";
    }

    // Replace "# Header" with <strong>Header</strong><br/>
    private String convertHeader(String header, String headerlevel) {
        int h = headerlevel.length();
        return "<strong>" + header.substring(h).trim() + "</strong><br/>\n";
    }

    // Replace a bullet point "- " with <li>
    private String convertBulletpoint(String bulletpoint) {
        if (ul) {
            return "<li>";
        } else {
            ul = true;
            return "<ul>\n<li>";
        }
    }

    // Replace *text* with <strong>text</strong>
    private String convertStrong(String text) {
        return "<strong>" + text.substring(1, text.length() - 1) + "</strong>";
    }

    // Replace <, > and & with &lt;, &gt; and &amp;
    private String convertEntity(String entity) {
    	return switch (entity) {
	    	case "<" -> "&lt;";
	    	case ">" -> "&gt;";
	    	case "&" -> "&amp;";
	    	default -> entity;
    	};
    }

    // Replace multiple newlines with <p>
    private String convertP(String p) {
        if (ul) {
            ul = false;
            return "\n</ul>\n<p>\n";
        } else {
            return "\n<p>\n";
        }
    }
    
    // Replace <p><pre> or <p><ul> (and any whitespace in between) with just the second tag
    private String convertEmptyP(String ph, String tag) {
    	return tag;
    }

    // Return the Java package name followed by "." for another (not our own) namespace
    private String formatNS(String ns) {
        return doc.getNamespace().name.equals(ns) ? "" : (Conversions.namespaceToJavaPackage(ns) + ".");
    }

    // Change method name to camel case Java style and prepend a "#"
    private String formatMethod(String name) {
        return "#" + Conversions.toLowerCaseJavaName(name);
    }

    // Format the type as a Java type (with org.package.Class#methodName syntax)
    private String girElementToString(GirElement girElement, boolean uppercase) {
        if (girElement == null) {
            return null;
        }
        String name = formatNS(girElement.getNamespace().name);
        String type = girElement.parent.name;
        if (type != null) {
            name += type;
        }
        String call = girElement.name;
        if (call != null) {
            name += (uppercase ? ("#" + call.toUpperCase()) : formatMethod(call));
        }
        return name;
    }
}
