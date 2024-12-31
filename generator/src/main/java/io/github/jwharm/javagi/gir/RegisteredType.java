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

import java.util.List;

import static io.github.jwharm.javagi.util.Conversions.toJavaQualifiedType;
import static io.github.jwharm.javagi.util.Conversions.uncapitalize;

public sealed interface RegisteredType
        extends Node
        permits Alias, Callback, Class, FlaggedType,
                Interface, Namespace, StandardLayoutType, FieldContainer {

    RegisteredType mergeWith(RegisteredType rt);
    int platforms();
    InfoAttrs infoAttrs();
    InfoElements infoElements();

    default boolean generic() {
        return false;
    }

    default ClassName typeName() {
        return toJavaQualifiedType(name(), namespace());
    }

    default String javaType() {
        return typeName().toString();
    }

    default String typeTag() {
        return uncapitalize(namespace().name() + name());
    }

    default PartialStatement constructorName() {
        return PartialStatement.of("$" + typeTag() + ":T::new",
                typeTag(), typeName());
    }

    default PartialStatement destructorName() {
        return PartialStatement.of("$glib:T::free", "glib", ClassNames.G_LIB);
    }

    /** Return true if this class is GObject or is derived from GObject */
    default boolean checkIsGObject() {
        return switch(this) {
            case Class c -> c.isInstanceOf("GObject", "Object");
            case Interface _ -> true; // Requires a runtime instanceof check
            case Alias a -> {
                var target = a.lookup();
                yield target != null && target.checkIsGObject();
            }
            default -> false;
        };
    }

    /** Return true if this is GList or GSList */
    default boolean checkIsGList() {
        return cType() != null && List.of("GList", "GSList").contains(cType());
    }

    /** Return true if this is GHashTable */
    default boolean checkIsGHashTable() {
        return cType() != null && "GHashTable".equals(cType());
    }

    default boolean isFloating() {
        // GObject has a ref_sink function, but we don't want to treat all
        // GObjects as floating references.
        if ("GObject".equals(cType()))
            return false;

        // GInitiallyUnowned is always a floating reference, and doesn't
        // explicitly need to be marked as such.
        if ("GInitiallyUnowned".equals(cType()))
            return false;

        // Subclasses of GInitiallyUnowned don't need to implement the
        // `Floating` interface, because GInitiallyUnowned already does.
        if (this instanceof Class cls
                && cls.isInstanceOf("GObject", "InitiallyUnowned"))
            return false;

        // Any other classes that have a ref_sink method, will be treated as
        // floating references.
        return parent().children().stream()
                .filter(Method.class::isInstance)
                .map(Method.class::cast)
                .anyMatch(m -> "ref_sink".equals(m.name()));
    }

    default ClassName helperClass() {
        return typeName().nestedClass("MethodHandles");
    }

    default String name() {
        return attr("name");
    }

    default String cType() {
        return attr("c:type");
    }

    default String glibTypeName() {
        return attr("glib:type-name");
    }

    default String getTypeFunc() {
        return attr("glib:get-type");
    }

    default String toStringTarget() {
        return attr("java-gi-to-string");
    }

    default boolean skipJava() {
        return attrBool("java-gi-skip", false);
    }

    default boolean customJava() {
        return attrBool("java-gi-custom", false);
    }
}
