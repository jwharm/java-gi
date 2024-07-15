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

import com.squareup.javapoet.ClassName;

import static io.github.jwharm.javagi.util.CollectionUtils.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Interface extends Multiplatform
        implements RegisteredType, FieldContainer {

    @Override
    public Namespace parent() {
        return (Namespace) super.parent();
    }

    public Interface(Map<String, String> attributes,
                     List<Node> children,
                     int platforms) {
        super(attributes, children, platforms);
    }

    @Override
    public PartialStatement constructorName() {
        return PartialStatement.of("$" + typeTag() + "Impl:T::new",
                typeTag() + "Impl",
                typeName().nestedClass(name() + "Impl"));
    }

    @Override
    public Interface mergeWith(RegisteredType rt) {
        if (rt instanceof Interface other)
            return new Interface(
                    attributes(),
                    union(children(), other.children()),
                    platforms() | other.platforms());
        return this;
    }

    public boolean generic() {
        return attrBool("java-gi-generic", false);
    }

    @Override
    public ClassName helperClass() {
        ClassName tn = typeName();
        return ClassName.get(tn.packageName(), tn.simpleName() + "MethodHandles");
    }

    public Record typeStruct() {
        String typeStruct = attr("glib:type-struct");
        return (Record) TypeReference.get(namespace(), typeStruct);
    }

    public boolean hasProperties() {
        return children().stream().anyMatch(Property.class::isInstance);
    }

    public List<Prerequisite> prerequisites() {
        return filter(children(), Prerequisite.class);
    }

    public List<Implements> implements_() {
        return filter(children(), Implements.class);
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

    public List<Callback> callbacks() {
        return filter(children(), Callback.class);
    }

    public List<Constant> constants() {
        return filter(children(), Constant.class);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || obj.getClass() != this.getClass())
            return false;

        var that = (Interface) obj;
        return Objects.equals(this.name(), that.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name());
    }
}
