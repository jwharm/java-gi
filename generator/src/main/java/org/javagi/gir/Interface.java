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

import org.javagi.util.PartialStatement;

import com.squareup.javapoet.ClassName;

import static org.javagi.util.CollectionUtils.*;
import static org.javagi.util.Conversions.toJavaIdentifier;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Interface extends GirElement implements RegisteredType, FieldContainer {

    @Override
    public Namespace parent() {
        return (Namespace) super.parent();
    }

    public Interface(Map<String, String> attributes, List<Node> children) {
        super(attributes, children);
    }

    @Override
    public PartialStatement constructorName() {
        return PartialStatement.of("$" + typeTag() + "_Impl:T::new",
                typeTag() + "_Impl",
                typeName().nestedClass(name() + "$Impl"));
    }

    @Override
    public PartialStatement destructorName() {
        Class base = prerequisiteBaseClass();
        String tag = base.typeTag();
        Method unrefFunc = base.unrefFunc();
        if (unrefFunc == null)
            return PartialStatement.of("(_ -> {})");
        
        return PartialStatement.of("(_p -> (($" + tag + ":T) _p).$unrefFunc:L())",
                tag, base.typeName(),
                "unrefFunc", toJavaIdentifier(unrefFunc.name()));
    }

    @Override
    public Interface mergeWith(RegisteredType rt) {
        if (rt instanceof Interface other)
            return new Interface(attributes(), union(children(), other.children()));
        return this;
    }

    public boolean generic() {
        if (attrBool("java-gi-generic", false))
            return true;

        for (var prereq : prerequisites())
            if (prereq.lookup() instanceof Interface i && i.generic())
                return true;

        return false;
    }

    @Override
    public ClassName helperClass() {
        ClassName tn = typeName();
        return ClassName.get(tn.packageName(), tn.simpleName() + "MethodHandles");
    }

    /**
     * Get the prerequisite base class that can implement this interface.
     * If no class is specified as a prerequisite, return GObject.
     */
    public Class prerequisiteBaseClass() {
        for (var prerequisite : prerequisites())
            if (prerequisite.lookup() instanceof Class c)
                return c;
        return (Class) TypeReference.lookup(namespace(), "GObject.Object");
    }

    public Record typeStruct() {
        String typeStruct = attr("glib:type-struct");
        return (Record) TypeReference.lookup(namespace(), typeStruct);
    }

    public boolean hasProperties() {
        return children().stream().anyMatch(Property.class::isInstance);
    }

    public boolean listInterface() {
        return attrBool("java-gi-list-interface", false);
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
