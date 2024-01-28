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
import io.github.jwharm.javagi.util.Platform;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.github.jwharm.javagi.util.Conversions.toJavaQualifiedType;

public abstract sealed class RegisteredType extends GirElement implements Multiplatform
        permits Alias, Boxed, Callback, Class, FlaggedType, Interface, Record, Union {

    private int platforms;

    public RegisteredType(Map<String, String> attributes, List<GirElement> children, int platforms) {
        super(attributes, children);
        this.platforms = platforms;
    }

    public abstract RegisteredType mergeWith(RegisteredType rt);

    @Override
    public void setPlatforms(int platforms) {
        this.platforms = platforms;
    }

    @Override
    public int platforms() {
        return this.platforms;
    }

    public ClassName typeName() {
        return toJavaQualifiedType(name(), namespace());
    }

    public String javaType() {
        return typeName().toString();
    }

    public String constructorName() {
        return javaType() + "::new";
    }

    public String getInteropString(String paramName, boolean isPointer, Scope scope) {
        return paramName + ".handle()";
    }

    public boolean isGObject() {
        return this instanceof Interface
                || (this instanceof Class c && c.isInstanceOf("GObject", "Object"))
                || (this instanceof Alias a && a.type().get() != null && a.type().get().isGObject());
    }

    public boolean doPlatformCheck() {
        return platforms() != Platform.ALL;
    }

    public InfoAttrs attrs() {
        return super.infoAttrs();
    }

    public String name() {
        return attr("name");
    }

    public String cType() {
        return attr("c:type");
    }

    public String glibTypeName() {
        return attr("glib:type-name");
    }

    public String getTypeFunc() {
        return attr("glib:get-type");
    }

    public InfoElements infoElements() {
        return super.infoElements();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RegisteredType) obj;
        return Objects.equals(this.name(), that.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name());
    }

    @Override
    public String toString() {
        return "%s %s %s %s".formatted(getClass().getSimpleName(), Platform.toString(platforms()), attributes(), children());
    }
}
