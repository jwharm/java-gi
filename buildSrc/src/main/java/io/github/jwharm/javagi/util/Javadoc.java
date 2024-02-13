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

import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Record;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.jwharm.javagi.util.Conversions.replaceKnownType;
import static io.github.jwharm.javagi.util.Conversions.toJavaIdentifier;

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
            + "|(?m)^(?<header>(?<headerlevel>#{1,6})\\s.+)\\n*"
            + "|(?m)^\\s*(?<bulletpoint>-)\\s"
            + "|(?<strong>\\*\\*.*?\\w\\*\\*)"
            + "|(?<em>\\*.*?\\w\\*)"
            + "|(?<entity>[<>&])"
            + "|(?<p>\\n{2,})"
    ;

    // These are the named groups for which the conversions to Javadoc are applied.
    // Other named groups are not matched separately, but only used as parameters 
    // for the conversion functions.
    private static final List<String> NAMED_GROUPS_PASS_1 = List.of(
            "codeblock", "codeblock2", "code", "link", "constantref", "typeref",
            "paramref", "hyperlink", "img", "header", "bulletpoint", "strong", "em", "entity", "p");
    
    private static final String REGEX_PASS_2 =
              "(?<emptyp><p>\\s*(?<tag><(pre|ul)>))"
            + "|(?m)(?<blockquote>(^&gt;\\s+.*\\n?)+)"
    ;
    
    private static final List<String> NAMED_GROUPS_PASS_2 = List.of("emptyp", "blockquote");

    private static Javadoc instance = null;
    private Documentation doc;
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
    public String convert(Documentation doc) {
        this.doc = doc;
        this.ul = false;
        
        // Conversion pass 1
        Matcher matcher = patternPass1.matcher(doc.text());
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

        // Escape "$" to prevent errors from JavaPoet
        return pass2Result.replace("$", "$$");
    }

    // Helper function to find out which named group was matched
    private String getMatchedGroupName(Matcher matcher, List<String> groupNames) {
        return groupNames.stream().filter((name) -> matcher.group(name) != null).findFirst().orElseThrow();
    }

    // Apply the relevant conversion for the provided named group
    private String convert(Matcher matcher, String groupName) {
        return switch(groupName) {
            // Pass 1 group names
            case "codeblock" -> convertCodeblock(matcher.group(), matcher.group("content"));
            case "codeblock2" -> convertCodeblock(matcher.group(), matcher.group("content2"));
            case "code" -> convertCode(matcher.group());
            case "link" -> convertLink(matcher.group(), matcher.group("type"), matcher.group("path"), matcher.group("part1"), matcher.group("part2"), matcher.group("part3"));
            case "constantref" -> convertConstantref(matcher.group());
            case "typeref" -> convertTyperef(matcher.group());
            case "paramref" -> convertParamref(matcher.group());
            case "hyperlink" -> convertHyperlink(matcher.group(), matcher.group("desc"), matcher.group("url"));
            case "img" -> convertImg(matcher.group(), matcher.group("imgdesc"), matcher.group("imgurl"));
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

    // Replace ```multi-line code blocks``` with <pre>{@code ... }</pre> blocks.
    // For valid javadoc, we must balance all curly braces.
    private String convertCodeblock(String codeblock, String content) {
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
                        return checkLink(part1) + part1 + "#" + part1 + "}";
                    } else {
                        return checkLink(part1, part2) + part1 + formatMethod(stripNewPrefix(part2)) + "}";
                    }
                } else {
                    Namespace ns = getNamespace(part1);
                    String className = (ns == null) ? part2 : replaceKnownType(part2, ns);
                    if ("new".equals(part3)) {
                        return checkLink(part1, part2) + formatNS(part1) + className + "#" + className + "}";
                    } else {
                        return checkLink(part1, part2, part3) + formatNS(part1) + className + formatMethod(stripNewPrefix(part3)) + "}";
                    }
                }
            case "method":
            case "vfunc":
                if (part3 == null) {
                    return checkLink(part1, part2) + replaceKnownType(part1, doc.namespace()) + formatMethod(part2) + "}";
                } else {
                    Namespace ns = getNamespace(part1);
                    return checkLink(part1, part2, part3) + formatNS(part1) + replaceKnownType(part2, ns) + formatMethod(part3) + "}";
                }
            case "func":
                if (part3 == null) {
                    if (part2 == null) {
                        return checkLink(part1) + doc.namespace().javaType() + formatMethod(part1) + "}";
                    } else {
                        Namespace ns = getNamespace(part1);
                        String className = (ns == null) ? part1 : ns.globalClassName();
                        return checkLink(part1, part2) + formatNS(part1) + className + formatMethod(part2) + "}";
                    }
                } else {
                    return checkLink(part1, part2, part3) + formatNS(part1) + part2 + formatMethod(part3) + "}";
                }
            case "iface":
            case "class":
                if (part2 == null) {
                    return checkLink(part1) + replaceKnownType(part1, doc.namespace()) + "}";
                } else {
                    Namespace ns = getNamespace(part1);
                    return checkLink(part1, part2) + formatNS(part1) + replaceKnownType(part2, ns) + "}";
                }
            case "id":
                name = formatCIdentifier(part1);
                return (name == null) ? "{@code " + path + "}" : "{@link " + name + "}";
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
        String name = formatCIdentifier(ref.substring(1));
        if (name == null) {
            return "{@code " + ref.substring(1) + "}";
        } else {
            return "{@link " + name + "}";
        }
    }

    // Replace #text with {@link text}
    private String convertTyperef(String ref) {
        RegisteredType rt = TypeReference.get(doc.namespace(), ref.substring(1));
        if (rt == null) {
            return "{@code " + ref.substring(1) + "}";
        } else {
            String typeName = rt.javaType();
            // If the link refers to the typestruct for a class, find the enclosing class and add it to the link.
            if (rt instanceof Record rec && rec.isGTypeStructFor() != null) {
                RegisteredType baseType = rec.isGTypeStructFor();
                if (baseType != null) {
                    typeName = baseType.javaType() + "." + typeName;
                }
            }
            return "{@link " + formatNS(rt.namespace().name()) + "." + typeName + "}";
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
        String fullUrl = url;
        if (! url.startsWith("http")) fullUrl = doc.namespace().docUrlPrefix() + url;
        String alt = desc.replace("\"", "\\\"");
        return "<img src=\"" + fullUrl + "\" alt=\"" + alt + "\">";
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
        Namespace namespace = doc.namespace();
        if (namespace.name().equals(ns)) return "";
        return namespace.parent().lookupNamespace(ns).packageName() + ".";
    }

    // Change method name to camel case Java style and prepend a "#"
    private String formatMethod(String name) {
        return "#" + toJavaIdentifier(name);
    }

    // Strip "new_" prefix from named constructors
    private String stripNewPrefix(String name) {
        return name.startsWith("new_") ? name.substring(4) : name;
    }

    // Format the C identifier as a Java type (with org.package.Class#memberName syntax)
    private String formatCIdentifier(String cIdentifier) {
        GirElement node = doc.namespace().parent().lookupCIdentifier(cIdentifier);
        if (node == null) return null;

        String type = switch(node.parent()) {
            case RegisteredType rt -> rt.javaType();
            case Namespace ns -> formatNS(node.namespace().name()) + ns.globalClassName();
            default -> "";
        };

        return type + switch(node) {
            case AbstractCallable c -> formatMethod(c.attrs().name());
            case Member m -> "#" + m.name().toUpperCase();
            case Constant c -> "#" + c.name().toUpperCase();
            default -> "";
        };
    }

    // Get the Namespace node for the provided namespace prefix
    private Namespace getNamespace(String ns) {
        return doc.namespace().parent().lookupNamespace(ns);
    }

    // Check if this type exists in the GIR file. If it does, generate a "{@link" tag,
    // otherwise, generate a "{@code" tag.
    private String checkLink(String identifier) {
        return checkLink(doc.namespace().name(), identifier);
    }

    // Check if this type exists in the GIR file. If it does, generate a "{@link" tag,
    // otherwise, generate a "{@code" tag.
    private String checkLink(String ns, String identifier) {
        try {
            var namespace = getNamespace(ns);
            if (namespace.registeredTypes().containsKey(identifier)) return "{@link ";
            return namespace.functions().stream()
                    .anyMatch(f -> identifier.equals(f.name())) ? "{@link " : "{@code ";
        } catch (NoSuchElementException e) {
            return checkLink(doc.namespace().name(), ns, identifier);
        }
    }

    // Check if this type exists in the GIR file. If it does, generate a "{@link" tag,
    // otherwise, generate a "{@code" tag.
    private String checkLink(String ns, String type, String identifier) {
        RegisteredType rt = TypeReference.get(getNamespace(ns), type);

        if (rt == null)
            return "{@code ";

        // Generating a link to an inner class is not implemented, it is used very little
        if (rt instanceof Record rec && rec.isGTypeStructFor() != null)
            return "{@code ";

        if (rt instanceof VirtualMethodContainer vmc) {
            for (VirtualMethod vm : vmc.virtualMethods()) {
                if (identifier.equals(vm.name()) && (! vm.skip())) return "{@link ";
            }
        }
        if (rt instanceof MethodContainer mc) {
            for (Method m : mc.methods()) {
                if (identifier.equals(m.name()) && (! m.skip())) return "{@link ";
            }
        }
        if (rt instanceof ConstructorContainer cc) {
            for (Constructor c : cc.constructors()) {
                if (identifier.equals(c.name()) && (! c.skip())) return "{@link ";
            }
        }
        if (rt instanceof FunctionContainer fc) {
            for (Function f : fc.functions()) {
                if (identifier.equals(f.name()) && (! f.skip())) return "{@link ";
            }
        }
        return "{@code ";
    }
}
