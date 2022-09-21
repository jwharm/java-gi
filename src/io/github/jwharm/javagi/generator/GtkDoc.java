package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.Doc;
import io.github.jwharm.javagi.model.GirElement;
import io.github.jwharm.javagi.model.RegisteredType;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GtkDoc {

    private static final String REGEX =
              "(?<codeblock>```(?<content>(?s)[[^`][^`][^`]]+)```)"
            + "|(?<code>`[^`]+`)"
            + "|(?<link>\\[(?<type>.+)@(?<path>(?<part1>[^\\.\\]]+)?\\.?(?<part2>[^\\.\\]\\:]+)?[\\.\\:]?(?<part3>.+)?)\\])"
            + "|(?<macroref>\\%\\w+)"
            + "|(?<typeref>\\#[^\\#\\s]\\w+)"
            + "|(?<paramref>\\@[^\\@\\s]\\w+)"
            + "|(?<hyperlink>\\[(?<desc>.+)\\]\\((?<url>.+)\\))"
            + "|(?<img>\\!\\[(?<imgdesc>.+)\\]\\((?<imgurl>.+)\\))"
            + "|(?m)^(?<header>(?<headerlevel>#{1,6})\\s.+)"
            + "|(?m)^\\s*(?<bulletpoint>\\-)\\s"
            + "|(?<strong>\\*.+\\*)"
            + "|(?<tag>\\<.+\\>)"
            + "|(?<p>\\n\\n)";

    private static final String[] NAMED_GROUPS = new String[] {
            "codeblock",
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
            "tag",
            "p"
    };

    private static GtkDoc instance = null;
    private Doc doc;
    private boolean ul;

    public static GtkDoc getInstance() {
        if (instance == null) {
            instance = new GtkDoc();
        }
        return instance;
    }

    private final Pattern pattern;

    private GtkDoc() {
        this.pattern = Pattern.compile(REGEX);
    }

    public String convert(Doc doc) {
        this.doc = doc;
        this.ul = false;
        Matcher matcher = pattern.matcher(doc.contents);
        StringBuilder output = new StringBuilder();
        while (matcher.find()) {
            String replacement = convert(matcher);
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private String convert(Matcher matcher) {
        String groupName = Arrays.stream(NAMED_GROUPS)
                .filter((name) -> matcher.group(name) != null)
                .findFirst()
                .orElseThrow();

        return switch(groupName) {
            case "codeblock" -> convertCodeblock(matcher.group(), matcher.group("content"));
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
            case "tag" -> convertTag(matcher.group());
            case "p" -> convertP(matcher.group());
            default -> matcher.group();
        };
    }

    private String convertCodeblock(String codeblock, String content) {
        return "<pre>{@code " + content + "}</pre>";
    }

    private String convertCode(String code) {
        return "{@code " + code.substring(1, code.length() - 1) + "}";
    }

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
                return "{@link " + formatNS(part1) + part2 + formatMethod(part3) + "}";
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

    private String convertMacroref(String ref) {
        switch (ref) {
            case "%NULL":   return "<code>null</code>";
            case "%TRUE":   return "<code>true</code>";
            case "%FALSE":  return "<code>false</code>";
        }
        GirElement girElement = Conversions.cIdentifierLookupTable.get(ref.substring(1));
        String name = girElementToString(girElement, true);
        if (name == null) {
            return "{@code " + ref.substring(1) + "}";
        } else {
            return "{@link " + name + "}";
        }
    }

    private String convertTyperef(String ref) {
        RegisteredType rt = Conversions.cTypeLookupTable.get(ref.substring(1));
        if (rt == null) {
            return "{@code " + ref.substring(1) + "}";
        } else {
            return "{@link " + formatNS(rt.getNamespace().name) + rt.name + "}";
        }
    }

    private String convertParamref(String ref) {
        return "{@code " + ref.substring(1) + "}";
    }

    private String convertHyperlink(String link, String desc, String url) {
        return "<a href=\"" + url + "\">" + desc + "</a>";
    }

    private String convertImg(String img, String desc, String url) {
        String alt = desc.replace("\"", "\\\"");
        return "<img src=\"./doc-files/" + url + "\" alt=\"" + alt + "\">";
    }

    private String convertHeader(String header, String headerlevel) {
        int h = headerlevel.length();
        return "<h" + h + ">" + header.substring(h).trim() + "</h" + h + ">";
    }

    private String convertBulletpoint(String bulletpoint) {
        if (ul) {
            return "<li>";
        } else {
            ul = true;
            return "<ul>\n<li>";
        }
    }

    private String convertStrong(String text) {
        return "<strong>" + text.substring(1, text.length() - 1) + "</strong>";
    }

    private String convertTag(String text) {
        return "&lt;" + text.substring(1, text.length() - 1) + "&gt;";
    }

    private String convertP(String p) {
        if (ul) {
            ul = false;
            return "\n</ul>\n<p>\n";
        } else {
            return "\n<p>\n";
        }
    }

    private String formatNS(String ns) {
        return doc.getNamespace().name.equals(ns) ? "" : (Conversions.namespaceToJavaPackage(ns) + ".");
    }

    private String formatMethod(String name) {
        return "#" + Conversions.toLowerCaseJavaName(name);
    }

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
