package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.model.Namespace;

public enum Platform {
    WINDOWS("windows"), MAC("macos"), LINUX("linux");

    public final String name;

    Platform(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String convertToJavaType(String name, boolean qualified, Namespace ns) {
        if (ns.platform != this && !(ns.platform == null && this == LINUX))
            throw new IllegalArgumentException("Namespace platform must be the same as this platform");
        return name == null ? null : switch (name.toLowerCase()) {
            case "gboolean" -> "boolean";
            case "gchar", "guchar", "gint8", "guint8" -> "byte";
            case "gshort", "gushort", "gint16", "guint16" -> "short";
            case "gint", "guint", "gint32", "guint32", "gunichar" -> "int";
            case "gint64", "gssize", "gsize", "goffset", "guint64", "gintptr", "guintptr" -> "long";
            case "glong", "gulong" -> this == WINDOWS ? "int" : "long";
            case "gdouble" -> "double";
            case "gfloat" -> "float";
            case "none" -> "void";
            case "utf8", "filename" -> "java.lang.String";
            case "gpointer", "gconstpointer" -> "java.lang.foreign.MemorySegment";
            case "gtype" -> qualified ? Conversions.toQualifiedJavaType("GLib.Type", ns) : Conversions.toSimpleJavaType("GLib.Type", ns);
            case "valist", "va_list" -> "VaList";
            case "long double" -> "double"; // unsupported data type
            default -> qualified ? Conversions.toQualifiedJavaType(name, ns) : Conversions.toSimpleJavaType(name, ns);
        };
    }
}
