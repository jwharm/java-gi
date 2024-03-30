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

import io.github.jwharm.javagi.util.PartialStatement;

import static io.github.jwharm.javagi.util.CollectionUtils.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Class extends Multiplatform
        implements RegisteredType, FieldContainer {

    public Class(Map<String, String> attributes, List<Node> children, int platforms) {
        super(attributes, children, platforms);
    }

    @Override
    public Namespace parent() {
        return (Namespace) super.parent();
    }

    @Override
    public PartialStatement constructorName() {
        return abstract_()
                ? PartialStatement.of("$" + typeTag() + "Impl:T::new",
                        typeTag() + "Impl", typeName().nestedClass(name() + "Impl"))
                : RegisteredType.super.constructorName();
    }

    @Override
    public RegisteredType mergeWith(RegisteredType rt) {
        if (rt instanceof Class other)
            return new Class(
                    attributes(),
                    union(children(), other.children()),
                    platforms() | other.platforms());
        return this;
    }

    public boolean generic() {
        return attrBool("java-gi-generic", false);
    }

    public boolean autoCloseable() {
        return attrBool("java-gi-auto-closeable", false);
    }

    public boolean isOpaque() {
        return fields().isEmpty() && unions().isEmpty();
    }

    public Class parentClass() {
        return (Class) TypeReference.get(namespace(), attr("parent"));
    }

    public boolean isInstanceOf(String ns, String name) {
        if (parent().name().equals(ns) && name().equals(name))
            return true;

        Class parentClass = parentClass();
        return parentClass != null
                && parentClass.isInstanceOf(ns, name);
    }

    public Record typeStruct() {
        return (Record) TypeReference.get(namespace(), attr("glib:type-struct"));
    }

    public String refFunc() {
        return attr("glib:ref-func");
    }

    public String unrefFunc() {
        return attr("glib:unref-func");
    }

    public String setValueFunc() {
        return attr("glib:set-value-func");
    }

    public String getValueFunc() {
        return attr("glib:get-value-func");
    }

    public String cSymbolPrefix() {
        return attr("c:symbol-prefix");
    }

    public boolean abstract_() {
        return attrBool("abstract", false);
    }

    public boolean fundamental() {
        return attrBool("fundamental", false);
    }

    public boolean final_() {
        return attrBool("final", false);
    }

    public List<Implements> implements_() {
        return filter(children(), Implements.class);
    }

    public List<Constructor> constructors() {
        return filter(children(), Constructor.class);
    }

    public List<Method> methods() {
        return filter(children(), Method.class);
    }

    public List<Function> functions() {
        return filter(children(), Function.class);
    }

    public List<VirtualMethod> virtualMethods() {
        return filter(children(), VirtualMethod.class);
    }

    public List<Field> fields() {
        return filter(children(), Field.class);
    }

    public List<Property> properties() {
        return filter(children(), Property.class);
    }

    public List<Signal> signals() {
        return filter(children(), Signal.class);
    }

    public List<Union> unions() {
        return filter(children(), Union.class);
    }

    public List<Constant> constants() {
        return filter(children(), Constant.class);
    }

    public List<Record> records() {
        return filter(children(), Record.class);
    }

    public List<Callback> callbacks() {
        return filter(children(), Callback.class);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || obj.getClass() != this.getClass())
            return false;

        var that = (Class) obj;
        return Objects.equals(this.name(), that.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name());
    }
}
