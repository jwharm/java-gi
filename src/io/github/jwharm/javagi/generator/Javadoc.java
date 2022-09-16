package io.github.jwharm.javagi.generator;

public class Javadoc {

    public static String convert(String gtkdoc) {
        String javadoc = convertSpecialCharacters(gtkdoc);
        javadoc = convertLineBreaks(javadoc);
        javadoc = convertCodeblocks(javadoc, true);
        javadoc = convertCodeblocks(javadoc, false);
        javadoc = convertReferences(javadoc);
        return javadoc;
    }

    private static String convertLineBreaks(String gtkdoc) {
        return gtkdoc.replace("\n\n", "\n<p>\n");
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

    private static String convertCodeblocks(String gtkdoc, boolean multiline) {
        int prev = 0, pos = 0;
        boolean inBlock = false;
        StringBuilder javadoc = new StringBuilder();
        while ((pos = gtkdoc.indexOf(multiline ? "```" : "`", prev)) != -1) {
            javadoc.append(gtkdoc.substring(prev, pos));
            if (multiline) {
                javadoc.append(inBlock ? "</pre>" : "<pre>");
            } else {
                javadoc.append(inBlock ? "}" : "{@code ");
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
            String link = gtkdoc.substring(pos, end + 1);
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
     * gtkdoc:  [method@Gtk.Widget.queue_draw]
     * javadoc: {@link Gtk.Widget#queueDraw}
     */
    private static String convertMethodLink(String link) {
        return link;
    }
}
