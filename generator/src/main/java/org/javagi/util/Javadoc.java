/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 Jan-Willem Harmannij
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

package org.javagi.util;

import org.javagi.configuration.ModuleInfo;
import org.javagi.gir.*;
import org.javagi.gir.Record;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.javagi.util.CollectionUtils.filter;
import static org.javagi.util.Conversions.*;

public class Javadoc {

    // This regular expression matches a series of separate regex patterns, 
    // each with a named group, separated by an "or" ("|") operand.
    // Most of the individual patterns should be relatively self-explanatory.
    private static final String REGEX_MAIN =
              "(?<codeblock>```(?<content>(?s).+?)```)"
            + "|(?<codeblock2>\\|\\[(?<content2>(?s).+?)]\\|)"
            + "|(?<code>`[^`]+?`)"
            + "|(?s)(?<hyperlink>\\[(?<desc>[^]]+)]\\((?<url>[^)]+)\\))"
            + "|(?<link>\\[(?<type>.+?)@(?<path>(?<part1>[^.\\]]+)?\\.?(?<part2>[^.\\]:]+)?[.:]?(?<part3>[^]]+?)?)])"
            + "|(?<imglink>src(?:set)?=\"(?<imgurl>.+?)\")"
            + "|(?<constantref>%\\w+)"
            + "|(?<typeref>#[^#\\s]\\w*)"
            + "|(?<paramref>@{1,2}[^@\\s][^\\s`]*)" // "[^\s`]" matches until whitespace or `code`
    ;

    // Regular expression that will match a language tag specified in an XML comment
    private static final String REGEX_LANGUAGE_STRING = "<!--\\s*language=\"(?<tag>.+?)\"\\s*-->";

    // These are the named groups for which the conversions to Javadoc are
    // applied. Other named groups are not matched separately, but only used as
    // parameters for the conversion functions.
    private static final List<String> NAMED_GROUPS = List.of(
            "code", "codeblock", "codeblock2", "hyperlink", "link",
            "constantref", "typeref", "paramref", "imglink");

    // The compiled regex patterns
    private static final Pattern PATTERN_MAIN = Pattern.compile(REGEX_MAIN);
    private static final Pattern PATTERN_LANGUAGE_STRING = Pattern.compile(REGEX_LANGUAGE_STRING);

    private Documentation doc;
    private InstanceParameter instanceParameter;

    // Lookup the instance parameter that this docstring might refer to, so
    // the reference to the parameter can be replaced with "this [type]".
    private InstanceParameter findInstanceParameter(Node node) {
        return switch (node) {
            case InstanceParameter p -> p;
            case Parameter p -> p.parent().instanceParameter();
            case ReturnValue rv when rv.parent() instanceof Callable c -> findInstanceParameter(c);
            case Callable c when c.parameters() != null -> c.parameters().instanceParameter();
            default -> null;
        };
    }

    /**
     * Convert comments into Javadoc. Assumes Markdown-format (JEP 467) Javadoc
     * output.
     */
    public String convert(Documentation doc) {
        this.doc = doc;
        this.instanceParameter = findInstanceParameter(doc.parent());
        String input = doc.text();

        Matcher matcher = Javadoc.PATTERN_MAIN.matcher(input);
        StringBuilder output = new StringBuilder();
        while (matcher.find()) {
            String group = getMatchedGroupName(matcher);
            String result = convert(matcher, group);
            matcher.appendReplacement(output, Matcher.quoteReplacement(result));
        }
        matcher.appendTail(output);

        // Escape "$" to prevent errors from JavaPoet
        return output.toString().replace("$", "$$");
    }

    // Helper function to find out which named group was matched
    private String getMatchedGroupName(Matcher matcher) {
        return Javadoc.NAMED_GROUPS.stream()
                .filter((name) -> matcher.group(name) != null)
                .findFirst()
                .orElseThrow();
    }

    // Apply the relevant conversion for the provided named group
    private String convert(Matcher m, String group) {
        return switch(group) {
            case "codeblock2"  -> convertCodeblock(m.group("content2"));
            case "link"        -> convertLink(m.group("type"), m.group("path"), m.group("part1"),
                                              m.group("part2"), m.group("part3"));
            case "constantref" -> convertConstantref(m.group());
            case "typeref"     -> convertTyperef(m.group());
            case "paramref"    -> convertParamref(m.group());
            case "imglink"     -> convertImg(m.group(), m.group("imgurl"));
            // Return the contents of "code", "codeblock" and "hyperlink" unchanged
            default            -> m.group();
        };
    }

    private String convertCodeblock(String content) {
        Matcher matcher = PATTERN_LANGUAGE_STRING.matcher(content);
        String output = matcher.find() ? matcher.replaceFirst(matcher.group("tag")) : content;
        return "```%s```".formatted(output);
    }

    // Replace [...] links with Javadoc links, or with `...` if the link seems
    // to be invalid.
    private String convertLink(String type, String path, String part1, String part2, String part3) {
        switch (type) {
            case "ctor":
                if (part3 == null) {
                    if ("new".equals(part2)) {
                        return checkLink(part1)
                                ? "[%s#%s]".formatted(part1, part1)
                                : "`%s#%s`".formatted(part1, part1);
                    } else {
                        String method = toJavaIdentifier(stripNewPrefix(part2));
                        return checkLink(part1, part2)
                                ? "[%s#%s][%s#%s]".formatted(part1, method, part1, method)
                                : "`%s%s`".formatted(part1, method);
                    }
                } else {
                    Namespace ns = getNamespace(part1);
                    String pkg = formatNS(part1);
                    String cls = (ns == null)
                            ? part2
                            : replaceKnownType(part2, ns);
                    if ("new".equals(part3)) {
                        return checkLink(part1, part2)
                                ? "[%s%s#%s]".formatted(pkg, cls, cls)
                                : "`%s%s#%s`".formatted(pkg, cls, cls);
                    } else {
                        String method = toJavaIdentifier(stripNewPrefix(part3));
                        return checkLink(part1, part2, part3)
                                ? "[%s#%s][%s%s#%s]".formatted(cls, method, pkg, cls, method)
                                : "`%s%s.%s`".formatted(pkg, cls, method);
                    }
                }
            case "method":
            case "vfunc":
                if (part3 == null) {
                    String cls = replaceKnownType(part1, doc.namespace());
                    String method = toJavaIdentifier(part2);
                    return checkLink(part1, part2)
                            ? "[%s#%s][%s#%s]".formatted(cls, method, cls, method)
                            : "`%s.%s`".formatted(cls, method);
                } else {
                    Namespace ns = getNamespace(part1);
                    String pkg = formatNS(part1);
                    String cls = replaceKnownType(part2, ns);
                    String method = toJavaIdentifier(part3);
                    return checkLink(part1, part2, part3)
                            ? "[%s#%s][%s%s#%s]".formatted(cls, method, pkg, cls, method)
                            : "`%s%s.%s`".formatted(pkg, cls, method);
                }
            case "func":
                if (part3 == null) {
                    if (part2 == null) {
                        String ns = doc.namespace().javaType();
                        String method = toJavaIdentifier(part1);
                        return checkLink(part1)
                                ? "[%s#%s][%s#%s]".formatted(ns, method, ns, method)
                                : "`%s.%s`".formatted(ns, method);
                    } else {
                        Namespace ns = getNamespace(part1);
                        String pkg = formatNS(part1);
                        String cls = (ns == null)
                                ? part1
                                : ns.globalClassName();
                        String method = toJavaIdentifier(part2);
                        return checkLink(part1, part2)
                                ? "[%s#%s][%s%s#%s]".formatted(cls, method, pkg, cls, method)
                                : "`%s%s.%s`".formatted(pkg, cls, method);
                    }
                } else {
                    String pkg = formatNS(part1);
                    String method = toJavaIdentifier(part3);
                    return checkLink(part1, part2, part3)
                            ? "[%s#%s][%s%s#%s]".formatted(part2, method, pkg, part2, method)
                            : "`%s%s.%s`".formatted(pkg, part2, method);
                }
            case "iface":
            case "class":
                if (part2 == null) {
                    String typeName = replaceKnownType(part1, doc.namespace());
                    return checkLink(part1)
                            ? "[%s]".formatted(typeName)
                            : "`%s`".formatted(typeName);
                } else {
                    Namespace ns = getNamespace(part1);
                    String pkg = formatNS(part1);
                    String cls = replaceKnownType(part2, ns);
                    return checkLink(part1, part2)
                            ? "[%s%s]".formatted(pkg, cls)
                            : "`%s%s`".formatted(pkg, cls);
                }
            case "id":
                String name = formatCIdentifier(part1);
                return (name == null)
                        ? "`%s`".formatted(path)
                        : "[%s][%s]".formatted(name, name);
            default:
                return "`%s`".formatted(path);
        }
    }

    /*
     * Replace %NULL, %TRUE and %FALSE with `null` etc, or %text with
     * [text] if it's a valid link
     */
    private String convertConstantref(String ref) {
        switch (ref) {
            case "%NULL":   return "`null`";
            case "%TRUE":   return "`true`";
            case "%FALSE":  return "`false`";
        }
        String name = formatCIdentifier(ref.substring(1));
        return name != null
                ? "[%s]".formatted(name)
                : "`%s`".formatted(ref.substring(1));
    }

    // Replace #text with a link
    private String convertTyperef(String ref) {
        String type = ref.substring(1);
        RegisteredType rt = TypeReference.lookup(doc.namespace(), type);
        if (rt == null || rt.skipJava()) {
            return "`%s`".formatted(ref.substring(1));
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
            return "[%s][%s.%s]".formatted(typeName, ns, typeName);
        }
    }

    // Replace reference to instance parameter with "this [type]" and other
    // parameters with `parameterName`
    private String convertParamref(String ref) {
        String paramName = ref.substring(ref.lastIndexOf('@') + 1);
        if (instanceParameter != null
                && instanceParameter.name().equals(paramName))
            return "this " + instanceParameter.type().name();
        return "`%s`".formatted(toJavaIdentifier(paramName));
    }

    // Prefix image sources with a full URL
    private String convertImg(String img, String url) {
        if (url == null)
            return img;

        if (! url.startsWith("http")) {
            String fullUrl = ModuleInfo.docUrlPrefix(doc.namespace().name()) + url;
            return img.replace(url, fullUrl);
        }

        return img;
    }

    // Return the Java package name followed by "." for another (not our own)
    // namespace.
    private String formatNS(String ns) {
        Namespace namespace = doc.namespace();
        if (namespace.name().equals(ns)) return "";
        try {
            return ModuleInfo.javaPackage(ns) + ".";
        } catch (NoSuchElementException e) {
            return "";
        }
    }

    // Strip "new_" prefix from named constructors
    private String stripNewPrefix(String name) {
        return name.startsWith("new_") ? name.substring(4) : name;
    }

    // Format a C identifier with Javadoc "package.Class#member" syntax
    private String formatCIdentifier(String cIdentifier) {
        Node node = doc.namespace().parent().lookupCIdentifier(cIdentifier);
        if (node == null || node.skipJava())
            return null;

        String type = switch(node.parent()) {
            case Namespace ns -> formatNS(node.namespace().name()) + ns.globalClassName();
            case RegisteredType rt -> rt.javaType();
            default -> "";
        };

        return type + switch(node) {
            case Callable c -> toJavaIdentifier(c.callableAttrs().name());
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

    // Check if this type exists in the GIR file
    private boolean checkLink(String identifier) {
        return checkLink(doc.namespace().name(), identifier);
    }

    // Check if this type exists in the GIR file
    private boolean checkLink(String ns, String identifier) {
        // Try [namespace.type]
        var namespace = getNamespace(ns);

        // Try [type.func]
        if (namespace == null)
            return checkLink(doc.namespace().name(), ns, identifier);

        // Valid [namespace.type] ?
        if (namespace.registeredTypes().values().stream().anyMatch(rt ->
                identifier.equals(rt.name()) && !rt.skipJava()))
            return true;

        // Valid [namespace.func] ?
        return namespace.functions().stream().anyMatch(f ->
                identifier.equals(f.name()) && !f.skipJava());
    }

    // Check if this type exists in the GIR file
    private boolean checkLink(String ns, String type, String identifier) {
        RegisteredType rt = TypeReference.lookup(getNamespace(ns), type);

        if (rt == null || rt.skipJava())
            return false;

        // Generating a link to an inner class is not implemented
        if (rt instanceof Record rec && rec.isGTypeStructFor() != null)
            return false;

        for (VirtualMethod vm : filter(rt.children(), VirtualMethod.class))
            if (identifier.equals(vm.name()) && (! vm.skip()))
                return true;

        for (Method m : filter(rt.children(), Method.class))
            if (identifier.equals(m.name()) && (! m.skip()))
                return true;

        for (Constructor c : filter(rt.children(), Constructor.class))
            if (identifier.equals(c.name()) && (! c.skip()))
                return true;

        for (Function f : filter(rt.children(), Function.class))
            if (identifier.equals(f.name()) && (! f.skip()))
                return true;

        return false;
    }
}
