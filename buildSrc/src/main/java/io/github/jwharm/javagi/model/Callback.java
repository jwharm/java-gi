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

import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;

public class Callback extends RegisteredType implements CallableType, Closure {

    public ReturnValue returnValue;
    public Parameters parameters;
    public String throws_;

    public Callback(GirElement parent, String name, String cType, String throws_, String version) {
        super(parent, name, null, cType, null, version);
        this.throws_ = throws_;
    }

    public void generate(SourceWriter writer) throws IOException {
        generateCopyrightNoticeAndPackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);
        generateFunctionalInterface(writer, javaName);
    }

    @Override
    public String getInteropString(String paramName, boolean isPointer, Scope scope) {
        String arena = scope == null ? "Arena.global()" : switch(scope) {
            case BOUND -> "Interop.attachArena(Arena.ofConfined(), this)";
            case CALL, ASYNC -> "_arena";
            case NOTIFIED -> "_" + paramName + "Scope";
            case FOREVER -> "Arena.global()";
        };
        return "%s.toCallback(%s)".formatted(paramName, arena);
    }

    @Override
    public Parameters getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Parameters ps) {
        this.parameters = ps;
    }

    @Override
    public ReturnValue getReturnValue() {
        return returnValue;
    }

    @Override
    public void setReturnValue(ReturnValue rv) {
        this.returnValue = rv;
    }

    @Override
    public Doc getDoc() {
        return doc;
    }

    @Override
    public String getThrows() {
        return throws_;
    }
    
    public String getConstructorString() {
        return this.javaName + "::new";
    }
}
