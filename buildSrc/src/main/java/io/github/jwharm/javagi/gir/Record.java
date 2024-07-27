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

import com.squareup.javapoet.ClassName;
import io.github.jwharm.javagi.configuration.ClassNames;
import io.github.jwharm.javagi.util.PartialStatement;

import static io.github.jwharm.javagi.util.CollectionUtils.*;
import static io.github.jwharm.javagi.util.Conversions.*;
import static java.util.function.Predicate.not;

import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Record extends Multiplatform
        implements RegisteredType, FieldContainer {

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
                    "gobjects", ClassNames.GOBJECTS,
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

    public boolean isOpaque() {
        return fields().isEmpty() && unions().isEmpty();
    }

    public boolean disguised() {
        return attrBool("disguised", false);
    }

    public boolean opaque() {
        return attrBool("opaque", false);
    }

    public boolean pointer() {
        return attrBool("pointer", false);
    }

    public boolean foreign() {
        return attrBool("foreign", false);
    }

    public RegisteredType isGTypeStructFor() {
        Namespace ns = namespace();
        return TypeReference.get(ns, attr("glib:is-gtype-struct-for"));
    }

    public String cSymbolPrefix() {
        return attr("c:symbol-prefix");
    }

    public Callable copyFunction() {
        // copy-function specified in annotation
        String func = attr("copy-function");
        if (func != null)
            return children().stream()
                    .filter(Callable.class::isInstance)
                    .map(Callable.class::cast)
                    .filter(c -> func.equals(c.callableAttrs().cIdentifier()))
                    .findAny()
                    .orElse(null);

        // boxed types: use g_boxed_copy
        if (getTypeFunc() != null)
            return namespace().parent().lookupNamespace("GObject").functions()
                    .stream()
                    .filter(f -> "g_boxed_copy".equals(f.callableAttrs().cIdentifier()))
                    .findAny()
                    .orElse(null);

        // use heuristics: find instance method `copy()` or `ref()`
        for (var m : methods())
            if ("ref".equals(m.name()) || "copy".equals(m.name())
                    && m.parameters().parameters().isEmpty())
                return m;

        return null;
    }

    public Callable freeFunction() {
        // free-function specified in annotation
        String func = attr("free-function");
        if (func != null)
            return children().stream()
                    .filter(Callable.class::isInstance)
                    .map(Callable.class::cast)
                    .filter(c -> func.equals(c.callableAttrs().cIdentifier()))
                    .findAny()
                    .orElse(null);

        // boxed types: use g_boxed_free
        if (getTypeFunc() != null)
            return namespace().parent().lookupNamespace("GObject").functions()
                    .stream()
                    .filter(f -> "g_boxed_free".equals(f.callableAttrs().cIdentifier()))
                    .findAny()
                    .orElse(null);

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
