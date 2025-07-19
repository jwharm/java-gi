/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 the Java-GI developers
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

package org.javagi.gir;

import com.squareup.javapoet.ClassName;
import org.javagi.configuration.ClassNames;
import org.javagi.util.PartialStatement;

import static org.javagi.util.CollectionUtils.*;
import static org.javagi.util.Conversions.*;
import static java.util.function.Predicate.not;

import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class Record extends Multiplatform
        implements StandardLayoutType {

    public Record(Map<String, String> attributes,
                  List<Node> children,
                  int platforms) {
        super(attributes, children, platforms);
    }

    @Override
    public Namespace parent() {
        return (Namespace) super.parent();
    }

    @Override
    public PartialStatement destructorName() {
        Callable freeFunc = freeFunction();
        if (freeFunc == null)
            return PartialStatement.of("(_ -> {})");

        String tag = ((RegisteredType) freeFunc.parent()).typeTag();

        if ("g_boxed_free".equals(freeFunc.callableAttrs().cIdentifier()))
            return PartialStatement.of("(_b -> $gobjects:T.boxedFree($" + tag + ":T.getType(), _b == null ? $memorySegment:T.NULL : _b.handle()))",
                    tag, typeName(),
                    "gobjects", ClassNames.G_OBJECTS,
                    "memorySegment", MemorySegment.class);

        return PartialStatement.of("$" + tag + ":T::$freeFunc:L",
                tag, typeName(),
                "freeFunc", toJavaIdentifier(freeFunc.name()));
    }

    @Override
    public Record mergeWith(RegisteredType rt) {
        if (rt instanceof Record other) {
            /*
             * If this record has different fields on different platforms,
             * remove all fields to prevent generating field accessors that
             * don't work across all platforms.
             */
            if (!this.fields().equals(other.fields()))
                return new Record(
                        attributes(),
                        union(children(), other.children())
                                .stream()
                                .filter(not(Field.class::isInstance))
                                .toList(),
                        platforms() | other.platforms());
            else
                return new Record(
                        attributes(),
                        union(children(), other.children()),
                        platforms() | other.platforms());
        }
        return this;
    }

    @Override
    public ClassName typeName() {
        var outerClass = isGTypeStructFor();
        if (outerClass != null)
            return outerClass.typeName()
                    .nestedClass(toJavaSimpleType(name(), namespace()));
        else
            return toJavaQualifiedType(name(), namespace());
    }

    public boolean generic() {
        return attrBool("java-gi-generic", false);
    }

    public boolean disguised() {
        return attrBool("disguised", false);
    }

    public boolean opaque() {
        if (attributes().containsKey("opaque"))
            return attrBool("opaque", false);
        else
            return fields().isEmpty() && unions().isEmpty();
    }

    public boolean pointer() {
        return attrBool("pointer", false);
    }

    public boolean foreign() {
        return attrBool("foreign", false);
    }

    public RegisteredType isGTypeStructFor() {
        return TypeReference.lookup(namespace(), attr("glib:is-gtype-struct-for"));
    }

    @Override
    public Callable copyFunction() {
        // copy-function specified in annotation
        var callable = StandardLayoutType.super.copyFunction();
        if (callable != null)
            return callable;

        // GValues are not boxed types
        if ("GValue".equals(cType()))
            return methods().stream()
                    .filter(m -> "g_value_copy".equals(m.callableAttrs().cIdentifier()))
                    .findAny().orElse(null);

        // boxed types: use g_boxed_copy
        if (getTypeFunc() != null)
            try {
                return namespace().parent().lookupNamespace("GObject").functions()
                        .stream()
                        .filter(f -> "g_boxed_copy".equals(f.callableAttrs().cIdentifier()))
                        .findAny()
                        .orElse(null);
            } catch (NoSuchElementException ignored) {
                // GObject namespace is not imported: g_boxed_copy not available
            }

        // use heuristics: find instance method `copy()` or `ref()`
        for (var m : methods())
            if ("ref".equals(m.name()) || "copy".equals(m.name())
                    && m.parameters().parameters().isEmpty())
                return m;

        return null;
    }

    public Callable freeFunction() {
        // free-function specified in annotation
        var callable = StandardLayoutType.super.freeFunction();
        if (callable != null)
            return callable;

        // boxed types: use g_boxed_free
        if (getTypeFunc() != null)
            try {
                return namespace().parent().lookupNamespace("GObject").functions()
                        .stream()
                        .filter(f -> "g_boxed_free".equals(f.callableAttrs().cIdentifier()))
                        .findAny()
                        .orElse(null);
            } catch (NoSuchElementException ignored) {
                // GObject namespace is not imported: g_boxed_free not available
            }

        // use heuristics: find function or method `free()` or `unref()`
        for (var n : children())
            if (n instanceof Callable c)
                if ("unref".equals(c.name()) || "free".equals(c.name())
                        && c.parameters().parameters().isEmpty())
                    return c;

        return null;
    }

    public List<Field> fields() {
        return filter(children(), Field.class);
    }

    public List<Function> functions() {
        return filter(children(), Function.class);
    }

    public List<Constructor> constructors() {
        return filter(children(), Constructor.class);
    }

    public List<Method> methods() {
        return filter(children(), Method.class);
    }

    public List<Union> unions() {
        return filter(children(), Union.class);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || obj.getClass() != this.getClass())
            return false;

        var that = (Record) obj;
        return Objects.equals(this.name(), that.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name());
    }
}
