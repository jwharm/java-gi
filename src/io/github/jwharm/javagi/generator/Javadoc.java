package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.GirElement;

import java.util.stream.Collectors;

public class Javadoc {

    public static String convert(String gtkdoc) {
        String javadoc = convertSpecialCharacters(gtkdoc);
        javadoc = convertCodeblocks(javadoc, true);
        javadoc = convertCodeblocks(javadoc, false);
        javadoc = convertReferences(javadoc);
        javadoc = convertConstantReferences(javadoc);
        return javadoc;
    }

    private static String convertSpecialCharacters(String gtkdoc) {
        StringBuilder out = new StringBuilder(Math.max(16, gtkdoc.length()));
        for (int i = 0; i < gtkdoc.length(); i++) {
            char c = gtkdoc.charAt(i);
            if (c > 127 || c == '"' || c == '\'' || c == '<' || c == '>' || c == '&') {
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String formatText(String gtkdoc) {
        String javadoc = gtkdoc.replace("\n\n", "\n<p>\n");
        javadoc = javadoc.lines()
                .map(Javadoc::convertHeader)
                .map(Javadoc::convertBulletPoint)
                .collect(Collectors.joining("\n"));
        return javadoc;
    }

    private static String convertHeader(String line) {
        if (line.startsWith("###")) {
            return "<h3>" + line.substring(3).trim() + "</h3>";
        } else if (line.startsWith("##")) {
            return "<h2>" + line.substring(3).trim() + "</h2>";
        } else if (line.startsWith("#")) {
            return "<h1>" + line.substring(3).trim() + "</h1>";
        } else {
            return line;
        }
    }

    private static String convertBulletPoint(String line) {
        if (line.startsWith("- ")) {
            return "<li>" + line.substring(2);
        } else {
            return line;
        }
    }

    private static String convertCodeblocks(String gtkdoc, boolean multiline) {
        int prev = 0, pos = 0;
        boolean inBlock = false;
        StringBuilder javadoc = new StringBuilder();
        while ((pos = gtkdoc.indexOf(multiline ? "```" : "`", prev)) != -1) {
            String chunk = gtkdoc.substring(prev, pos);
            if (! inBlock) {
                javadoc.append(formatText(chunk));
            } else {
                javadoc.append(chunk);
            }
            if (multiline) {
                javadoc.append(inBlock ? "</pre>" : "<pre>");
            } else {
                javadoc.append(inBlock ? "</code>" : "<code>");
            }
            inBlock = !inBlock;
            prev = pos + (multiline ? 3 : 1);
        }
        if (javadoc.length() == 0) {
            return gtkdoc;
        } else {
            javadoc.append(gtkdoc.substring(prev));
            return javadoc.toString();
        }
    }

    private static String convertReferences(String gtkdoc) {
        int prev = 0, pos = 0;
        StringBuilder javadoc = new StringBuilder();
        while ((pos = gtkdoc.indexOf('[', prev)) != -1) {
            javadoc.append(gtkdoc.substring(prev, pos));

            int end = gtkdoc.indexOf(']', pos);
            if (end == -1) {
                break;
            }
            String link = convertLink(gtkdoc.substring(pos, end + 1));
            javadoc.append("{@link ").append(link).append("}");

            prev = end + 1;
        }
        if (javadoc.length() == 0) {
            return gtkdoc;
        } else {
            javadoc.append(gtkdoc.substring(prev));
            return javadoc.toString();
        }
    }

    /*
     * input:  [method@Gtk.Widget.queue_draw] / class / func / ...
     * output: org.gtk.gtk.Widget#queueDraw
     */
    private static String convertLink(String link) {
        if (! link.contains("@")) {
            return link;
        }
        String ref = link.substring(1, link.indexOf("@"));
        String[] path = link.substring(link.indexOf("@") + 1, link.length() - 1).split("\\.");
        if (ref.equals("method") || ref.equals("vfunc")) {
            if (path.length < 3) {
                return link;
            }
            return Conversions.namespaceToJavaPackage(path[0]) + "." + path[1] + "#" + Conversions.toLowerCaseJavaName(path[2]);
        } else if (ref.equals("func")) {
            if (path.length < 2) {
                return link;
            }
            return path[0] + "#" + Conversions.toLowerCaseJavaName(path[1]);
        } else if (ref.equals("class")) {
            if (path.length < 2) {
                return link;
            }
            return Conversions.namespaceToJavaPackage(path[0]) + "." + path[1];
        } else if (ref.equals("id")) {
            if (path.length < 1) {
                return link;
            }
            GirElement girElement = Conversions.cIdentifierLookupTable.get(path[0]);
            String name = girElementToString(girElement, false);
            if (name == null) {
                return path[0] + " - id not found";
            } else {
                return name;
            }
        }

        return link + " (ref=" + ref + ")";
    }

    private static String convertConstantReferences(String gtkdoc) {
        int prev = 0, pos = 0;
        StringBuilder javadoc = new StringBuilder();
        while ((pos = gtkdoc.indexOf('%', prev)) != -1) {
            javadoc.append(gtkdoc.substring(prev, pos));

            int end = gtkdoc.indexOf(' ', pos); // FIXME: check for "," and other characters
            if (end == -1) {
                break;
            }
            String link = convertConstantReference(gtkdoc.substring(pos, end));
            javadoc.append(link).append(" ");

            prev = end + 1;
        }
        if (javadoc.length() == 0) {
            return gtkdoc;
        } else {
            javadoc.append(gtkdoc.substring(prev));
            return javadoc.toString();
        }
    }

    /*
     * input: %GTK_SIZE_REQUEST_CONSTANT_SIZE
     * output: {@link org.gtk.gtk.SizeRequestMode#CONSTANT_SIZE}
     */
    private static String convertConstantReference(String reference) {
        if (reference.equals("%NULL")) {
            return "<code>null</code>";
        } else if (reference.equals("%TRUE")) {
            return "<code>true</code>";
        } else if (reference.equals("%FALSE")) {
            return "<code>false</code>";
        }
        GirElement girElement = Conversions.cIdentifierLookupTable.get(reference.substring(1));
        String name = girElementToString(girElement, true);
        if (name == null) {
            return "<code>" + reference.substring(1) + "</code>";
        } else {
            return "{@link " + name + "}";
        }
    }

    private static String girElementToString(GirElement girElement, boolean uppercase) {
        if (girElement == null) {
            return null;
        }
        String name = "";
        String ns = girElement.parent.parent.name;
        if (ns != null) {
            name += Conversions.namespaceToJavaPackage(ns) + ".";
        }
        String type = girElement.parent.name;
        if (type != null) {
            name += type;
        }
        String call = girElement.name;
        if (call != null) {
            name += "#" + (uppercase ? call.toUpperCase() : Conversions.toLowerCaseJavaName(call));
        }
        return name;
    }
}
