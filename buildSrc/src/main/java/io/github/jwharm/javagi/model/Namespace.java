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

import io.github.jwharm.javagi.configuration.PackageNames;
import io.github.jwharm.javagi.generator.Platform;

import java.util.HashMap;
import java.util.Map;

public class Namespace extends GirElement {

    public final String version;
    public final String sharedLibrary;
    public final String cIdentifierPrefix;
    public final String cSymbolPrefix;
    public final String packageName;
    public final String globalClassName;
    public final String pathName;
    public final Map<String, RegisteredType> registeredTypeMap = new HashMap<>();
    public final Map<Platform, String> sharedLibraries = new HashMap<>();

    public Namespace(GirElement parent, String name, String version, String sharedLibrary,
                     String cIdentifierPrefix, String cSymbolPrefix) {
        super(parent);
        this.name = name;
        this.version = version;
        this.sharedLibrary = sharedLibrary;
        this.cIdentifierPrefix = cIdentifierPrefix;
        this.cSymbolPrefix = cSymbolPrefix;
        this.packageName = PackageNames.getMap().get(name);
        this.globalClassName = (name.equals("GObject") ? "GObjects" : name);
        this.pathName = packageName.replace('.', '/') + '/';
    }

    public Namespace copy() {
        return new Namespace(parent, name, version, sharedLibrary, cIdentifierPrefix, cSymbolPrefix);
    }
}
