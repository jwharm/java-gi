/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 Jan-Willem Harmannij
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package io.github.jwharm.javagi.util;

import io.github.jwharm.javagi.configuration.ModuleInfo;
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Record;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.jwharm.javagi.util.CollectionUtils.filter;
import static io.github.jwharm.javagi.util.Conversions.*;

public class Javadoc {

    // This regular expression matches a series of separate regex patterns, 
    // each with a named group, separated by an "or" ("|") operand.
    // Most of the individual patterns should be relatively self-explanatory.
    private static final String REGEX_PASS_1 =
              "(?<codeblock>```(?<content>(?s).+?)```)"
            + "|(?<codeblock2>\\|\\[(?<content2>(?s).+?)]\\|)"
            + "|(?<code>`[^`]+?`)"
            + "|(?<link>\\[(?<type>.+?)@(?<path>(?<part1>[^.\\]]+)?\\.?(?<part2>[^.\\]:]+)?[.:]?(?<part3>[^]]+?)?)])"
            + "|(?<constantref>%\\w+)"
            + "|(?<typeref>#[^#\\s]\\w*)"
            + "|(?<paramref>@{1,2}[^@\\s]\\w*)"
            + "|(?<hyperlink>\\[(?<desc>.+?)]\\((?<url>.+?)\\))"
            + "|(?<img>!\\[(?<imgdesc>.*?)]\\((?<imgurl>.+?)\\))"
            + "|(?s)(?<picture><picture.*?>.*?<img src=\"(?<pictureurl>[^\"]+?)\".+?alt=\"(?<alt>[^\"]+?)\".+?</picture>)"
            + "|(?m)^(?<header>(?<headerlevel>#{1,6})\\s.+?)\\n+"
            + "|(?m)^(?<container>:::\\s.+)\\n*"
            + "|(?m)^\\s*(?<bulletpoint>[-*])\\s"
            + "|(?<strong>\\s(?<strongcontent>\\*\\*\\w+?\\*\\*)\\s)"
            + "|(?<em>\\s(?<emcontent>\\*\\w+?\\*)\\s)"
            + "|(?<entity>[<>&])"
            + "|(?<p>\\n{2,})"
    ;

    // A second regex to run on the results of the first one.
    private static final String REGEX_PASS_2 =
              "(?<emptyp><p>\\s*(?<tag><(pre|ul)>))"
            + "|(?m)(?<blockquote>(^&gt;\\s+.*\\n?)+)"
            + "|(?<code>`[^`]+?`)"
            + "|(?<kbd>&lt;kbd&gt;(?<kbdcontent>.+?)&lt;/kbd&gt;)"
    ;

    /*
     * These are the named groups for which the conversions to Javadoc are
     * applied. Other named groups are not matched separately, but only used as
     * parameters for the conversion functions.
     */
    private static final List<String> NAMED_GROUPS_PASS_1 = List.of(
            "codeblock", "codeblock2", "code", "link", "constantref",
            "typeref", "paramref", "hyperlink", "img", "picture", "header",
            "container", "bulletpoint", "strong", "em", "entity", "p");
    
    private static final List<String> NAMED_GROUPS_PASS_2 = List.of(
            "emptyp", "blockquote", "code", "kbd");

    // The compiled regex patterns
    private static final Pattern PATTERN_PASS_1 = Pattern.compile(REGEX_PASS_1);
    private static final Pattern PATTERN_PASS_2 = Pattern.compile(REGEX_PASS_2);

    private Documentation doc;
    private InstanceParameter instanceParameter;
    private boolean ul;

    // Lookup the instance parameter that this docstring might refer to, so
    // the reference to the parameter can be replaced with "this [type]".
    private InstanceParameter findInstanceParameter(Node node) {
        return switch (node) {
            case InstanceParameter p ->
                    p;
            case Parameter p ->
                    p.parent().instanceParameter();
            case ReturnValue rv when rv.parent() instanceof Callable c ->
                    findInstanceParameter(c);
            case Callable c when c.parameters() != null ->
                    c.parameters().instanceParameter();
            default ->
                    null;
        };
    }

    /**
     * Convert comments into Javadoc.
     */
    public String convert(Documentation doc) {
        this.doc = doc;
        this.instanceParameter = findInstanceParameter(doc.parent());
        this.ul = false;
        String input = doc.text();

        // Conversion pass 1
        String output = process(PATTERN_PASS_1, NAMED_GROUPS_PASS_1, input);

        // If the docstring ends with a list, append </ul>
        if (ul) {
            ul = false;
            output += "\n</ul>";
        }

        // Conversion pass 2
        output = process(PATTERN_PASS_2, NAMED_GROUPS_PASS_2, output);

        // Escape "$" to prevent errors from JavaPoet
        return output.replace("$", "$$");
    }

    private String process(Pattern pattern, List<String> groups, String input) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder output = new StringBuilder();
        while (matcher.find()) {
            String group = getMatchedGroupName(matcher, groups);
            String result = convert(matcher, group);
            matcher.appendReplacement(output, Matcher.quoteReplacement(result));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    // Helper function to find out which named group was matched
    private String getMatchedGroupName(Matcher matcher,
                                       List<String> groupNames) {
        return groupNames.stream()
                .filter((name) -> matcher.group(name) != null)
                .findFirst()
                .orElseThrow();
    }

    // Apply the relevant conversion for the provided named group
    private String convert(Matcher m, String group) {
        return switch(group) {
            // Pass 1 group names
            case "codeblock"   -> convertCodeblock(m.group(),
                                                   m.group("content"));
            case "codeblock2"  -> convertCodeblock(m.group(),
                                                   m.group("content2"));
            case "code"        -> convertCode(m.group());
            case "link"        -> convertLink(m.group(),
                                              m.group("type"),
                                              m.group("path"),
                                              m.group("part1"),
                                              m.group("part2"),
                                              m.group("part3"));
            case "constantref" -> convertConstantref(m.group());
            case "typeref"     -> convertTyperef(m.group());
            case "paramref"    -> convertParamref(m.group());
            case "hyperlink"   -> convertHyperlink(m.group(),
                                                   m.group("desc"),
                                                   m.group("url"));
            case "img"         -> convertImg(m.group(),
                                             m.group("imgdesc"),
                                             m.group("imgurl"));
            case "picture"     -> convertImg(m.group(),
                                             m.group("alt"),
                                             m.group("pictureurl"));
            case "header"      -> convertHeader(m.group(),
                                                m.group("headerlevel"));
            case "container"   -> convertContainer(m.group());
            case "bulletpoint" -> convertBulletpoint(m.group());
            case "strong"      -> convertStrong(m.group(),
                                                m.group("strongcontent"));
            case "em"          -> convertEm(m.group(),
                                            m.group("emcontent"));
            case "entity"      -> convertEntity(m.group());
            case "p"           -> convertP(m.group());

            // Pass 2 group names
            case "emptyp"      -> convertEmptyP(m.group(),
                                                m.group("tag"));
            case "blockquote"  -> convertBlockquote(m.group());
            case "kbd"         -> convertKbd(m.group(), m.group("kbdcontent"));

            // Ignored
            default            -> m.group();
        };
    }

    // Replace ```multi-line code blocks``` with <pre>{@code ... }</pre> blocks.
    // The (optional) language identifier is removed.
    // For valid javadoc, we must balance all curly braces.
    private String convertCodeblock(String codeblock, String content) {
        // Remove language identifier
        int newline = content.indexOf('\n');
        if (newline > 0 && newline < (content.length() - 1))
            content = content.substring(newline + 1);

        long count1 = content.chars().filter(ch -> ch == '{').count();
        long count2 = content.chars().filter(ch -> ch == '}').count();
        if (count1 < count2)
            content = "{".repeat((int) (count2 - count1)) + content;
        else if (count1 > count2)
            content += "}".repeat((int) (count1 - count2));
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
                        String tag = checkLink(part1);
                        return tag + part1 + "#" + part1 + "}";
                    } else {
                        String tag = checkLink(part1, part2);
                        String method = formatMethod(stripNewPrefix(part2));
                        return tag + part1 + method + "}";
                    }
                } else {
                    Namespace ns = getNamespace(part1);
                    String cls = (ns == null)
                            ? part2
                            : replaceKnownType(part2, ns);
                    if ("new".equals(part3)) {
                        String tag = checkLink(part1, part2);
                        return tag + formatNS(part1) + cls + "#" + cls + "}";
                    } else {
                        String tag = checkLink(part1, part2, part3);
                        String method = formatMethod(stripNewPrefix(part3));
                        return tag + formatNS(part1) + cls + method + "}";
                    }
                }
            case "method":
            case "vfunc":
                if (part3 == null) {
                    String tag = checkLink(part1, part2);
                    String typeName = replaceKnownType(part1, doc.namespace());
                    String method = formatMethod(part2);
                    return tag + typeName + method + "}";
                } else {
                    String tag = checkLink(part1, part2, part3);
                    Namespace ns = getNamespace(part1);
                    String typeName = replaceKnownType(part2, ns);
                    String method = formatMethod(part3);
                    return tag + formatNS(part1) + typeName + method + "}";
                }
            case "func":
                if (part3 == null) {
                    if (part2 == null) {
                        String tag = checkLink(part1);
                        String ns = doc.namespace().javaType();
                        String method = formatMethod(part1);
                        return tag + ns + method + "}";
                    } else {
                        String tag = checkLink(part1, part2);
                        Namespace ns = getNamespace(part1);
                        String className = (ns == null)
                                ? part1
                                : ns.globalClassName();
                        String method = formatMethod(part2);
                        return tag + formatNS(part1) + className + method + "}";
                    }
                } else {
                    String tag = checkLink(part1, part2, part3);
                    String ns = formatNS(part1);
                    String method = formatMethod(part3);
                    return tag + ns + part2 + method + "}";
                }
            case "iface":
            case "class":
                if (part2 == null) {
                    String tag = checkLink(part1);
                    String typeName = replaceKnownType(part1, doc.namespace());
                    return tag + typeName + "}";
                } else {
                    String tag = checkLink(part1, part2);
                    Namespace ns = getNamespace(part1);
                    String typeName = replaceKnownType(part2, ns);
                    return tag + formatNS(part1) + typeName + "}";
                }
            case "id":
                name = formatCIdentifier(part1);
                return (name == null)
                        ? "{@code " + path + "}"
                        : "{@link " + name + "}";
            default:
                return "{@code " + path + "}";
        }
    }

    /*
     * Replace %NULL, %TRUE and %FALSE with {@code true} etc, or %text with
     * {@link text}
     */
    private String convertConstantref(String ref) {
        switch (ref) {
            case "%NULL":   return "{@code null}";
            case "%TRUE":   return "{@code true}";
            case "%FALSE":  return "{@code false}";
        }
        String name = formatCIdentifier(ref.substring(1));
        if (name == null) {
            return "{@code " + ref.substring(1) + "}";
        } else {
            return "{@link " + name + "}";
        }
    }

    // Replace #text with {@link text}
    private String convertTyperef(String ref) {
        String type = ref.substring(1);
        RegisteredType rt = TypeReference.lookup(doc.namespace(), type);
        if (rt == null) {
            return "{@code " + ref.substring(1) + "}";
        } else {
            String typeName = rt.javaType();
            // If the link refers to the typestruct for a class, find the
            // enclosing class and add it to the link.
            if (rt instanceof Record rec && rec.isGTypeStructFor() != null) {
                RegisteredType baseType = rec.isGTypeStructFor();
                if (baseType != null) {
                    typeName = baseType.javaType() + "." + typeName;
                }
            }
            String ns = formatNS(rt.namespace().name());
            return "{@link " + ns + "." + typeName + "}";
        }
    }

    // Replace reference to instance parameter with "this [type]" and other
    // parameters with {@code parameterName}
    private String convertParamref(String ref) {
        String paramName = ref.substring(ref.lastIndexOf('@') + 1);
        if (instanceParameter != null
                && instanceParameter.name().equals(paramName))
            return "this " + instanceParameter.type().name();
        return "{@code " + toJavaIdentifier(paramName) + "}";
    }

    // Replace "[...](...)" with <a href="...">...</a>
    private String convertHyperlink(String link, String desc, String url) {
        return "<a href=\"" + url + "\">" + desc + "</a>";
    }

    // Replace "! [...](...)" image links with <img src="..." alt="...">
    private String convertImg(String img, String desc, String url) {
        String fullUrl = url;
        if (! url.startsWith("http"))
            fullUrl = ModuleInfo.docUrlPrefix(doc.namespace().name()) + url;
        String alt = desc.replace("\"", "\\\"");
        return "<img src=\"" + fullUrl + "\" alt=\"" + alt + "\">";
    }

    // Replace "# Header" with <strong>Header</strong><br/>
    private String convertHeader(String header, String headerlevel) {
        int h = headerlevel.length();
        return "<strong>" + header.substring(h).trim() + "</strong><br/>\n";
    }

    // Replace "::: note" with <em>Note</em><br/>
    private String convertContainer(String name) {
        String title = capitalize(name.substring(3).trim());
        return "<em>" + title + "</em><br/>\n";
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
    private String convertStrong(String match, String text) {
        return " <strong>" + text.substring(2, text.length() - 2) + "</strong> ";
    }

    // Replace *text* with <em>text</em>
    private String convertEm(String match, String text) {
        return " <em>" + text.substring(1, text.length() - 1) + "</em> ";
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

    // Replace <kbd>...</kbd> with {@code ...}
    private String convertKbd(String kbd, String kbdContent) {
        return "{@code " + kbdContent + "}";
    }

    /*
     * Replace <p><pre> or <p><ul> (and any whitespace in between) with just the
     * second tag
     */
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

    /*
     * Return the Java package name followed by "." for another (not our own)
     * namespace
     */
    private String formatNS(String ns) {
        Namespace namespace = doc.namespace();
        if (namespace.name().equals(ns)) return "";
        try {
            return ModuleInfo.packageName(ns) + ".";
        } catch (NoSuchElementException e) {
            return "";
        }
    }

    // Change method name to camel case Java style and prepend a "#"
    private String formatMethod(String name) {
        return "#" + toJavaIdentifier(name);
    }

    // Strip "new_" prefix from named constructors
    private String stripNewPrefix(String name) {
        return name.startsWith("new_") ? name.substring(4) : name;
    }

    /*
     * Format the C identifier as a Java type (with org.package.Class#memberName
     * syntax)
     */
    private String formatCIdentifier(String cIdentifier) {
        Node node = doc.namespace().parent().lookupCIdentifier(cIdentifier);
        if (node == null) return null;

        String type = switch(node.parent()) {
            case Namespace ns -> formatNS(node.namespace().name())
                                    + ns.globalClassName();
            case RegisteredType rt -> rt.javaType();
            default -> "";
        };

        return type + switch(node) {
            case Callable c -> formatMethod(c.callableAttrs().name());
            case Member m -> "#" + toJavaConstantUpperCase(m.name());
            case Constant c -> "#" + toJavaConstant(c.name());
            default -> "";
        };
    }

    // Get the Namespace node for the provided namespace prefix
    private Namespace getNamespace(String ns) {
        try {
            return doc.namespace().parent().lookupNamespace(ns);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /*
     * Check if this type exists in the GIR file. If it does, generate a
     * "{@link" tag, otherwise, generate a "{@code" tag.
     */
    private String checkLink(String identifier) {
        return checkLink(doc.namespace().name(), identifier);
    }

    /*
     * Check if this type exists in the GIR file. If it does, generate a
     * "{@link" tag, otherwise, generate a "{@code" tag.
     */
    private String checkLink(String ns, String identifier) {
        // Try [namespace.type]
        var namespace = getNamespace(ns);

        // Try [type.func]
        if (namespace == null)
            return checkLink(doc.namespace().name(), ns, identifier);

        // Valid [namespace.type] ?
        if (namespace.registeredTypes().containsKey(identifier))
            return "{@link ";

        // Valid [namespace.func] ?
        return namespace.functions().stream()
                .anyMatch(f -> identifier.equals(f.name()))
                    ? "{@link "
                    : "{@code ";
    }

    /*
     * Check if this type exists in the GIR file. If it does, generate a
     * "{@link" tag, otherwise, generate a "{@code" tag.
     */
    private String checkLink(String ns, String type, String identifier) {
        RegisteredType rt = TypeReference.lookup(getNamespace(ns), type);

        if (rt == null)
            return "{@code ";

        // Generating a link to an inner class is not implemented
        if (rt instanceof Record rec && rec.isGTypeStructFor() != null)
            return "{@code ";

        for (VirtualMethod vm : filter(rt.children(), VirtualMethod.class))
                if (identifier.equals(vm.name()) && (! vm.skip()))
                    return "{@link ";

        for (Method m : filter(rt.children(), Method.class))
            if (identifier.equals(m.name()) && (! m.skip()))
                return "{@link ";

        for (Constructor c : filter(rt.children(), Constructor.class))
            if (identifier.equals(c.name()) && (! c.skip()))
                return "{@link ";

        for (Function f : filter(rt.children(), Function.class))
            if (identifier.equals(f.name()) && (! f.skip()))
                return "{@link ";

        return "{@code ";
    }
}
