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
import static io.github.jwharm.javagi.util.CollectionUtils.*;
import static io.github.jwharm.javagi.util.Conversions.toJavaQualifiedType;

import io.github.jwharm.javagi.configuration.ModuleInfo;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class Namespace extends GirElement implements Multiplatform, FunctionContainer {
    private int platforms;
    private final Map<Integer, String> sharedLibraries;

    public Namespace(Map<String, String> attributes, List<GirElement> children,
                     int platforms, Map<Integer, String> sharedLibraries) {
        super(attributes, children);
        this.platforms = platforms;
        this.sharedLibraries = sharedLibraries;
        this.sharedLibraries.put(platforms, sharedLibrary());
    }

    @Override
    public Repository parent() {
        return (Repository) super.parent();
    }

    @Override
    public Namespace namespace() {
        return this;
    }

    @Override
    public void setPlatforms(int platforms) {
        this.platforms = platforms;
    }

    @Override
    public int platforms() {
        return platforms;
    }

    /**
     * Create a map of all registered types in this namespace (aliases, classes,
     * interfaces, records, enumerations, bitfields, callbacks and boxed types),
     * indexed by name.
     */
    public Map<String, RegisteredType> registeredTypes() {
        return filter(children(), RegisteredType.class).stream().collect(
                Collectors.toMap(RegisteredType::name, java.util.function.Function.identity()));
    }

    public Namespace mergeWith(Namespace other) {
        return new Namespace(
                attributes(),
                union(children(), other.children()),
                platforms() | other.platforms(),
                union(sharedLibraries, other.sharedLibraries)
        );
    }

    public ClassName typeName() {
        String name = name();
        if ("GObject".equals(name)) name = "GObjects";
        return toJavaQualifiedType(name, this);
    }

    public String javaType() {
        return typeName().toString();
    }

    public String name() {
        return attr("name");
    }

    public String version() {
        return attr("version");
    }

    public String cIdentifierPrefix() {
        return attr("c:identifier-prefixes");
    }

    public String cSymbolPrefix() {
        return attr("c:symbol-prefixes");
    }

    @Deprecated
    public String cPrefix() {
        return attr("c:prefix");
    }

    public String sharedLibrary() {
        return attr("shared-library");
    }

    public String sharedLibrary(int platform) {
        return sharedLibraries.get(platform);
    }

    public Map<Integer, String> sharedLibraries() {
        return sharedLibraries;
    }

    public List<Alias> aliases() {
        return filter(children(), Alias.class);
    }

    public List<Class> classes() {
        return filter(children(), Class.class);
    }

    public List<Docsection> docsections() {
        return filter(children(), Docsection.class);
    }

    public List<Interface> interfaces() {
        return filter(children(), Interface.class);
    }

    public List<Record> records() {
        return filter(children(), Record.class);
    }

    public List<Enumeration> enumerations() {
        return filter(children(), Enumeration.class);
    }

    public List<Function> functions() {
        return filter(children(), Function.class);
    }

    public List<Union> unions() {
        return filter(children(), Union.class);
    }

    public List<Bitfield> bitfields() {
        return filter(children(), Bitfield.class);
    }

    public List<Callback> callbacks() {
        return filter(children(), Callback.class);
    }

    public List<Constant> constants() {
        return filter(children(), Constant.class);
    }

    public List<Attribute> annotations() {
        return filter(children(), Attribute.class);
    }

    public List<Boxed> boxeds() {
        return filter(children(), Boxed.class);
    }

    public String packageName() {
        return Objects.requireNonNullElse(ModuleInfo.getPackageName(name()), name());
    }

    public String docUrlPrefix() {
        return Objects.requireNonNullElse(ModuleInfo.getDocUrlPrefix(name()), "");
    }

    public String globalClassName() {
        return name().equals("GObject") ? "GObjects" : name();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Namespace) obj;
        return Objects.equals(this.name(), that.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + attributes() + " " + Platform.toString(platforms());
    }
}
