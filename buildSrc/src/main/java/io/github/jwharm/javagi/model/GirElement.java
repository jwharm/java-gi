/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2023 Jan-Willem Harmannij
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

package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Platform;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;
import java.util.*;

public abstract class GirElement {

    private static GirElement previouslyCreated = null;

    public GirElement parent;
    public GirElement next;
    public Array array;
    public Type type = null;
    public String name = null;
    public String deprecated = null;
    public Doc doc = null;
    public DocDeprecated docDeprecated = null;
    public DocVersion docVersion = null;
    public final List<Member> memberList = new ArrayList<>();
    public final List<Attribute> attributeList = new ArrayList<>();
    public final List<Field> fieldList = new ArrayList<>();
    public final List<Function> functionList = new ArrayList<>();
    public final List<Include> includeList = new ArrayList<>();
    public final List<Implements> implementsList = new ArrayList<>();
    public final List<Method> methodList = new ArrayList<>();
    public final List<Property> propertyList = new ArrayList<>();
    public final List<Signal> signalList = new ArrayList<>();
    public final List<VirtualMethod> virtualMethodList = new ArrayList<>();
    public final List<Constructor> constructorList = new ArrayList<>();
    public final List<Alias> aliasList = new ArrayList<>();
    public final List<Callback> callbackList = new ArrayList<>();
    public final List<Bitfield> bitfieldList = new ArrayList<>();
    public final List<Class> classList = new ArrayList<>();
    public final List<Constant> constantList = new ArrayList<>();
    public final List<Docsection> docsectionList = new ArrayList<>();
    public final List<Enumeration> enumerationList = new ArrayList<>();
    public final List<FunctionMacro> functionMacroList = new ArrayList<>();
    public final List<Interface> interfaceList = new ArrayList<>();
    public final List<Record> recordList = new ArrayList<>();
    public final List<Union> unionList = new ArrayList<>();

    public final Set<Platform> platforms = new HashSet<>();

    public GirElement(GirElement parent) {
        this.parent = parent;

        // Create a link to the previously created element, so we can easily traverse the entire tree later.
        // Don't link from one repository to the next.
        if (! (previouslyCreated == null || this instanceof Repository)) {
            previouslyCreated.next = this;
        }
        previouslyCreated = this;
    }

    public Type getType() {
        return this.type;
    }

    public Namespace getNamespace() {
        if (this instanceof Repository r) {
            return r.namespace;
        } else if (this instanceof Namespace ns) {
            return ns;
        } else {
            return this.parent.getNamespace();
        }
    }

    public Module module() {
        return ((Repository) getNamespace().parent).module;
    }

    /**
     * Check if this method must do a platorm compatibility check and throw UnsupportedPlatformException.
     * When an entire class is not cross-platform, the check is not done on regular methods, but only on
     * the constructors and on functions (static methods).
     * @return whether to do a cross-platform availability check.
     */
    public boolean doPlatformCheck() {
        if (parent instanceof RegisteredType && parent.platforms.size() < 3 && platforms.size() < 3) {
            return this instanceof Constructor || this instanceof Function;
        }
        return platforms.size() < 3;
    }

    public void generatePlatformCheck(SourceWriter writer) throws IOException {
        // No platform check neccessary
        if (! doPlatformCheck()) {
            return;
        }

        // Generate platform check; this will throw UnsupportedPlatformException based on the runtime platform
        StringJoiner joiner = new StringJoiner(", ", "Platform.checkSupportedPlatform(", ");\n");
        for (Platform platform : platforms) {
            joiner.add("\"" + platform.name.toLowerCase() + "\"");
        }
        writer.write(joiner.toString());
    }

    public String toString() {
        return this.getClass().getSimpleName() + " " + this.name;
    }
}
