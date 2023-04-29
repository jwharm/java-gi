package io.github.jwharm.javagi.generator;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.jwharm.javagi.model.*;
import io.github.jwharm.javagi.model.Record;

public class Javadoc {

    // This regular expression matches a series of separate regex patterns, 
    // each with a named group, separated by an "or" ("|") operand.
    // Most of the individual patterns should be relatively self-explanatory.
    private static final String REGEX_PASS_1 =
            // ``` multiline code block ```
              "(?<codeblock>```(?<content>(?s).+?)```)"
            // |[ multiline code block ]|
            + "|(?<codeblock2>\\|\\[(?<content2>(?s).+?)\\]\\|)"
            // `code`
            + "|(?<code>`[^`]+?`)"
            // [link]
            + "|(?<link>\\[(?<type>.+?)@(?<path>(?<part1>[^\\.\\]]+)?\\.?(?<part2>[^\\.\\]\\:]+)?[\\.\\:]?(?<part3>.+?)?)\\])"
            // %constant reference
            + "|(?<constantref>\\%\\w+)"
            // #type
            + "|(?<typeref>\\#[^\\#\\s]\\w*)"
            // @param and @@param
            + "|(?<paramref>\\@{1,2}[^\\@\\s]\\w*)"
            // [url](for link)
            + "|(?<hyperlink>\\[(?<desc>.+?)\\]\\((?<url>.+?)\\))"
            // ! [url](for image)
            + "|(?<img>\\!\\[(?<imgdesc>.*?)\\]\\((?<imgurl>.+?)\\))"
            // # Header level 1, ## Header level 2 etc
            + "|(?m)^(?<header>(?<headerlevel>#{1,6})\\s.+)\\n*"
            // - Bullet lists, we generate <ul> for the first bullet and <li> for every bullet
            + "|(?m)^\\s*(?<bulletpoint>\\-)\\s"
            // Match **strong text**
            + "|(?<strong>\\*\\*.*?\\w\\*\\*)"
            // Match *emphasized text*
            + "|(?<em>\\*.*?\\w\\*)"
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
            "constantref",
            "typeref",
            "paramref",
            "hyperlink",
            "img",
            "header",
            "bulletpoint",
            "strong",
            "em",
            "entity",
            "p"
    };
    
    private static final String REGEX_PASS_2 =
            // <p> immediately followed by <pre>
              "(?<emptyp>\\<p\\>\\s*(?<tag>\\<(pre|ul)\\>))"
            // > one or more blockquote lines
            + "|(?m)(?<blockquote>(^\\&gt;\\s+.*\\n?)+)"
    ;
    
    private static final String[] NAMED_GROUPS_PASS_2 = new String[] {
            "emptyp",
            "blockquote"
    };

    private static Javadoc instance = null;
    private Doc doc;
    private boolean ul;

    /**
     * Return the singleton instance of the Javadoc class.
     */
    public static Javadoc getInstance() {
        if (instance == null) {
            instance = new Javadoc();
        }
        return instance;
    }

    private final Pattern patternPass1;
    private final Pattern patternPass2;

    // This class is a singleton. The regex patterns are compiled only once.
    private Javadoc() {
        this.patternPass1 = Pattern.compile(REGEX_PASS_1);
        this.patternPass2 = Pattern.compile(REGEX_PASS_2);
    }

    /**
     * Convert comments into Javadoc.
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
        // If the docstring ends with a list, append </ul>
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

    // Helper function to find out which named group was matched
    private String getMatchedGroupName(Matcher matcher, String[] groupNames) {
        return Arrays.stream(groupNames)
                .filter((name) -> matcher.group(name) != null)
                .findFirst()
                .orElseThrow();
    }

    // Apply the relevant conversion for the provided named group
    private String convert(Matcher matcher, String groupName) {

        return switch(groupName) {
            // Pass 1 group names
            case "codeblock" -> convertCodeblock(matcher.group(), matcher.group("content"));
            case "codeblock2" -> convertCodeblock(matcher.group(), matcher.group("content2"));
            case "code" -> convertCode(matcher.group());
            case "link" -> convertLink(matcher.group(),
                    matcher.group("type"), matcher.group("path"),
                    matcher.group("part1"), matcher.group("part2"), matcher.group("part3"));
            case "constantref" -> convertConstantref(matcher.group());
            case "typeref" -> convertTyperef(matcher.group());
            case "paramref" -> convertParamref(matcher.group());
            case "hyperlink" -> convertHyperlink(matcher.group(),
                    matcher.group("desc"), matcher.group("url"));
            case "img" -> convertImg(matcher.group(),
                    matcher.group("imgdesc"), matcher.group("imgurl"));
            case "header" -> convertHeader(matcher.group(), matcher.group("headerlevel"));
            case "bulletpoint" -> convertBulletpoint(matcher.group());
            case "strong" -> convertStrong(matcher.group());
            case "em" -> convertEm(matcher.group());
            case "entity" -> convertEntity(matcher.group());
            case "p" -> convertP(matcher.group());
            
            // Pass 2 group names
            case "emptyp" -> convertEmptyP(matcher.group(), matcher.group("tag"));
            case "blockquote" -> convertBlockquote(matcher.group());
            
            default -> matcher.group();
        };
    }

    // Replace multi-line code blocks (starting and ending with ```) with <pre>{@code ... }</pre> blocks
    // If the codeblock contains curly braces, we ensure they are matched with the same number of closing braces.
    // This is required for valid javadoc.
    private String convertCodeblock(String codeblock, String content) {
        long count1 = content.chars().filter(ch -> ch == '{').count();
        long count2 = content.chars().filter(ch -> ch == '}').count();
        if (count1 < count2) {
            content = "{".repeat((int) (count2 - count1)) + content;
        } else if (count1 > count2) {
            content += "}".repeat((int) (count1 - count2));
        }
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
                if (part3 == null) {
                    if ("new".equals(part2)) {
                        return checkLink(part1) + part1 + "#" + part1 + "}";
                    } else {
                        return checkLink(part1, part2) + part1 + formatMethod(part2) + "}";
                    }
                } else {
                    if ("new".equals(part3)) {
                        return checkLink(part1, part2) + formatNS(part1) + part2 + "#" + part2 + "}";
                    } else {
                        return checkLink(part1, part2, part3) + formatNS(part1) + part2 + formatMethod(part3) + "}";
                    }
                }
            case "method":
            case "vfunc":
                if (part3 == null) {
                    return checkLink(part1, part2) + Conversions.replaceKnownType(part1, doc.getNamespace()) + formatMethod(part2) + "}";
                } else {
                    Namespace ns = getNamespace(part1);
                    return checkLink(part1, part2, part3) + formatNS(part1) + Conversions.replaceKnownType(part2, ns) + formatMethod(part3) + "}";
                }
            case "property":
                return "{@code " + path + "}";
            case "func":
                if (part3 == null) {
                    if (part2 == null) {
                        return checkLink(part1) + doc.getNamespace().name + formatMethod(part1) + "}";
                    } else {
                        return checkLink(part1, part2) + formatNS(part1) + part1 + formatMethod(part2) + "}";
                    }
                } else {
                    return checkLink(part1, part2, part3) + formatNS(part1) + part2 + formatMethod(part3) + "}";
                }
            case "class":
                if (part2 == null) {
                    return checkLink(part1) + Conversions.replaceKnownType(part1, doc.getNamespace()) + "}";
                } else {
                    Namespace ns = getNamespace(part1);
                    return checkLink(part1, part2) + formatNS(part1) + Conversions.replaceKnownType(part2, ns) + "}";
                }
            case "id":
                GirElement girElement = Conversions.cIdentifierLookupTable.get(part1);
                name = girElementToString(girElement, false);
                return (name == null) ? ("{@code " + path + "}") : ("{@link " + name + "}");
            default:
                return "{@code " + path + "}";
        }
    }

    // Replace %NULL, %TRUE and %FALSE with {@code true} etc, or %text with {@link text}
    private String convertConstantref(String ref) {
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
            String typeName = rt.javaName;
            // If the link refers to the typestruct for a class, find the enclosing class and add it to the link.
            if (rt instanceof Record rec && rec.isGTypeStructFor != null) {
                RegisteredType baseType = rt.getNamespace().registeredTypeMap.get(rec.isGTypeStructFor);
                if (baseType != null) {
                    typeName = baseType.javaName + "." + typeName;
                }
            }
            return "{@link " + formatNS(rt.getNamespace().name) + typeName + "}";
        }
    }

    // Replace @text with {@code text}
    private String convertParamref(String ref) {
        return "{@code " + ref.substring(ref.lastIndexOf('@') + 1) + "}";
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

    // Replace **text** with <strong>text</strong>
    private String convertStrong(String text) {
        return "<strong>" + text.substring(2, text.length() - 2) + "</strong>";
    }

    // Replace *text* with <em>text</em>
    private String convertEm(String text) {
        return "<em>" + text.substring(1, text.length() - 1) + "</em>";
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
    
    // Convert quote lines (starting with "> ") to html <blockquote>s
    private String convertBlockquote(String blockquoteLines) {
        String result = blockquoteLines.replace("\n&gt;", "\n");
        if (result.startsWith("&gt;")) {
            result = result.substring(4);
        }
        if (! result.endsWith("\n")) {
            result += "\n";
        }
        return "<blockquote>\n" + result + "</blockquote>";
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
            name += ((uppercase && (girElement instanceof Member)) ? ("#" + call.toUpperCase()) : formatMethod(call));
        }
        return name;
    }

    // Get the Namespace node for the provided namespace prefix
    private Namespace getNamespace(String ns) {
        Repository gir = Conversions.repositoriesLookupTable.get(ns);
        return gir == null ? null : gir.namespace;
    }
    
    // Check if this type exists in the GIR file. If it does, generate a "{@link" tag,
    // otherwise, generate a "{@code" tag.
    private String checkLink(String identifier) {
        return checkLink(doc.getNamespace().name, identifier);
    }
    
    // Check if this type exists in the GIR file. If it does, generate a "{@link" tag,
    // otherwise, generate a "{@code" tag.
    private String checkLink(String ns, String identifier) {
        Repository gir = Conversions.repositoriesLookupTable.get(ns);
        if (gir == null || gir.namespace == null) {
            return "{@code ";
        }
        Namespace namespace = gir.namespace;
        if (namespace.registeredTypeMap.containsKey(identifier)) {
            return "{@link ";
        }
        for (Function f : namespace.functionList) {
            if (identifier.equals(f.name)) {
                return "{@link ";
            }
        }
        return "{@code ";
    }

    // Check if this type exists in the GIR file. If it does, generate a "{@link" tag,
    // otherwise, generate a "{@code" tag.
    private String checkLink(String ns, String type, String identifier) {
        Repository gir = Conversions.repositoriesLookupTable.get(ns);
        if (gir == null || gir.namespace == null) {
            return "{@code ";
        }
        Namespace namespace = gir.namespace;
        RegisteredType rt = namespace.registeredTypeMap.get(type);
        if (rt == null) {
            return "{@code ";
        }
        for (Method m : rt.methodList) {
            if (identifier.equals(m.name)) {
                return "{@link ";
            }
        }
        for (Constructor c : rt.constructorList) {
            if (identifier.equals(c.name)) {
                return "{@link ";
            }
        }
        for (Function f : rt.functionList) {
            if (identifier.equals(f.name)) {
                return "{@link ";
            }
        }
        return "{@code ";
    }
}
