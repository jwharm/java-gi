/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 the Java-GI developers
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

package io.github.jwharm.javagi.gir;

import com.squareup.javapoet.TypeName;
import io.github.jwharm.javagi.util.Conversions;

import static io.github.jwharm.javagi.util.Conversions.toJavaBaseType;
import static io.github.jwharm.javagi.util.CollectionUtils.*;
import static io.github.jwharm.javagi.util.Conversions.uncapitalize;

import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Type extends AnyType implements TypeReference {

    public Type(Map<String, String> attributes, List<GirElement> children) {
        super(attributes, children);
    }

    @Override
    public String name() {
        String name = attr("name");
        return switch(name) {
            case null -> null;
            case "GType" -> "GLib.Type";
            case "gulong" -> overrideLongValue() ? "guint" : "gulong";
            case "glong" -> overrideLongValue() ? "gint" : "glong";
            default -> name;
        };
    }

    public boolean introspectable() {
        return attrBool("introspectable", true);
    }

    public InfoElements infoElements() {
        return super.infoElements();
    }

    public List<AnyType> anyTypes() {
        return filter(children(), AnyType.class);
    }

    @Override
    public TypeName typeName() {
        String javaBaseType = toJavaBaseType(name());
        return switch(javaBaseType) {
            case "void" -> TypeName.VOID;
            case "boolean", "byte", "char", "double", "float", "int", "long", "short" -> Conversions.primitiveTypeName(javaBaseType);
            case "java.lang.String" -> TypeName.get(String.class);
            case "java.lang.foreign.MemorySegment" -> TypeName.get(MemorySegment.class);
            case null, default -> TypeReference.super.typeName();
        };
    }

    public boolean isPrimitive() {
        String type = toJavaBaseType(name());
        return type != null && List.of("boolean", "byte", "char", "double", "float", "int", "long", "short")
                .contains(type);
    }

    public boolean isBoolean() {
        return "gboolean".equals(name()) && (!"_Bool".equals(cType()));
    }

    public boolean isPointer() {
        return cType() != null && (cType().endsWith("*") || cType().endsWith("gpointer"));
    }

    public boolean isProxy() {
        if (cType() != null && cType().endsWith("**")) return false; // A pointer to a proxy is not a proxy
        return switch(get()) {
            case Alias a -> a.type().isProxy();
            case Class _, Interface _, Record _, Union _ -> true;
            case null, default -> false;
        };
    }

    public boolean checkIsGObject() {
        RegisteredType target = get();
        return target != null && target.checkIsGObject();
    }

    public boolean isActuallyAnArray() {
        return cType() != null && cType().endsWith("**")
                && (! (parent() instanceof Parameter p && p.isOutParameter()));
    }

    /**
     * Generate a string that uniquely identifies this type, and can be used as the
     * name of a named argument in a JavaPoet code block.
     * @return a name for this type that can be used as a named argument in a code block,
     *         or {@code null} if the unique identifier could not be generated.
     */
    public String toTypeTag() {
        String cType = cType();
        if (cType != null) {
            int start = cType.lastIndexOf(" ");
            int end = cType.indexOf("*");
            if (start == -1) start = 0; else start++;
            if (end == -1) end = cType.length();
            return uncapitalize(cType.substring(start, end));
        }
        RegisteredType target = get();
        return target == null ? null : uncapitalize(target.namespace().name() + target.name());
    }

    private boolean overrideLongValue() {
        GirElement parent = parent();
        if (parent instanceof Array)
            parent = parent.parent();

        return switch (parent) {
            case Property _, Alias _, ReturnValue _, Parameter _ -> true;
            case null, default -> false;
        };
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Type) obj;
        return Objects.equals(this.name(), that.name()) &&
                Objects.equals(this.cType(), that.cType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name(), cType());
    }
}
