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

package org.javagi.generators;

import com.squareup.javapoet.*;
import org.javagi.configuration.ClassNames;
import org.javagi.util.PartialStatement;
import org.javagi.gir.*;

import javax.lang.model.element.Modifier;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.*;

import static org.javagi.util.Conversions.getValueLayout;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;

public class CallableGenerator {

    private final Callable callable;

    public CallableGenerator(Callable callable) {
        this.callable = callable;
    }

    CodeBlock generateFunctionDescriptorDeclaration() {
        return CodeBlock.builder()
                .add("$[$T _fdesc = ", FunctionDescriptor.class)
                .add(generateFunctionDescriptor())
                .add(";\n$]")
                .build();
    }

    CodeBlock generateFunctionDescriptor() {
        List<PartialStatement> layouts = new ArrayList<>();
        var addressLayout = PartialStatement.of("$valueLayout:T.ADDRESS", "valueLayout", ValueLayout.class);

        var returnType = callable.returnValue().anyType();
        boolean isVoid = returnType instanceof Type t && t.isVoid();
        if (!isVoid)
            layouts.add(generateValueLayoutWithFixForSignals(returnType));

        if (callable instanceof Signal)
            layouts.add(addressLayout);

        if (callable.parameters() != null) {
            var iParam = callable.parameters().instanceParameter();
            if (iParam != null)
                layouts.add(generateValueLayout(iParam.anyType()));
            layouts.addAll(
                    callable.parameters().parameters().stream()
                            .filter(not(Parameter::varargs))
                            .map(Parameter::anyType)
                            .map(this::generateValueLayoutWithFixForSignals)
                            .toList());
        }

        if (callable.throws_())
            layouts.add(addressLayout);

        if (layouts.isEmpty())
            return CodeBlock.of("$T.ofVoid()", FunctionDescriptor.class);

        var stmt = PartialStatement.of("$functionDescriptor:T." + (isVoid ? "ofVoid" : "of"),
                        "functionDescriptor", FunctionDescriptor.class)
                .add(layouts.stream().map(PartialStatement::format).collect(joining(",$W", "(", ")")));
        for (var layout : layouts)
            stmt.arguments().putAll(layout.arguments());
        return stmt.toCodeBlock();
    }

    /**
     * For plain struct types (i.e. struct, union, boxed types) that are not
     * passed by reference but by value, the FunctionDescriptor normally
     * contains the complete memory layout of the struct. But for signals,
     * it must be ValueLayout.ADDRESS. I'm not sure why, but the JVM segfaults
     * otherwise.
     */
    PartialStatement generateValueLayoutWithFixForSignals(AnyType anyType) {
        if (callable instanceof Signal
                && anyType instanceof Type t
                && !t.isPointer()
                && t.lookup() instanceof StandardLayoutType slt
                && new MemoryLayoutGenerator().canGenerate(slt)) {
            return PartialStatement.of("$valueLayout:T.ADDRESS", "valueLayout", ValueLayout.class);
        }
        return generateValueLayout(anyType);
    }

    PartialStatement generateValueLayout(AnyType anyType) {
        return anyType instanceof Type type && type.isLong()
                ? PartialStatement.of("$interop:T.longAsInt() ? $valueLayout:T.JAVA_INT : $valueLayout:T.JAVA_LONG",
                        "valueLayout", ValueLayout.class,
                        "interop", ClassNames.INTEROP)
                : getValueLayout(anyType, false);
    }

    void generateMethodParameters(MethodSpec.Builder builder,
                                  boolean generic,
                                  boolean setOfBitfield) {
        if (callable.parameters() == null)
            return;

        for (var p : callable.parameters().parameters()) {
            if (p.isUserDataParameter()
                    || p.isDestroyNotifyParameter()
                    || p.isArrayLengthParameter())
                continue;

            if (p.varargs()) {
                builder.addParameter(Object[].class, "varargs");
                builder.varargs(true);
            } else {
                var generator = new TypedValueGenerator(p);
                var type = generator.getType(setOfBitfield);

                // Trailing flags parameter can be variadic
                if ((!setOfBitfield)
                        && p.isBitfield()
                        && p.isLastParameter()
                        && (!p.isOutParameter())) {
                    type = ArrayTypeName.of(type);
                    builder.varargs(true);
                }

                if (generic && type.equals(ClassNames.G_OBJECT))
                    type = ClassNames.GENERIC_T;

                var spec = ParameterSpec.builder(generator.annotated(type), generator.getName());
                builder.addParameter(spec.build());
            }
        }
    }

    PartialStatement marshalParameters(boolean intAsLong) {
        var parameters = callable.parameters();
        if (parameters == null)
            return callable.throws_()
                    ? PartialStatement.of("_gerror")
                    : new PartialStatement();

        PartialStatement stmt = PartialStatement.of(null,
                "memorySegment", MemorySegment.class);

        // Marshal instance parameter
        InstanceParameter iParam = parameters.instanceParameter();
        if (iParam != null) {
            if (iParam.type().lookup() instanceof EnumType)
                stmt.add("getValue()"); // method in Enumeration class
            else
                stmt.add("handle()");   // method in regular TypeInstance class
        }

        // Marshal other parameters
        for (Parameter p : parameters.parameters()) {
            if (!stmt.format().isEmpty()) stmt.add(", ");
            stmt.add("$Z"); // emit newline
            var generator = new TypedValueGenerator(p);
            var name = generator.getName();

            // Generate null-check. But don't null-check parameters that are
            // hidden from the Java API, or primitive values
            boolean nullCheck = generator.checkNull();

            // Always translate a `null` GList/GSlist to native NULL
            if (p.anyType() instanceof Type t && t.checkIsGList())
                nullCheck = true;

            if (nullCheck)
                stmt.add("($memorySegment:T) (" + name + " == null ? $memorySegment:T.NULL : ");

            // cast int parameter to a long
            if (intAsLong && p.anyType() instanceof Type t && t.isLong())
                stmt.add("(long) ");

            // Callback destroy
            if (p.isDestroyNotifyParameter()) {
                var notify = parameters.parameters().stream()
                        .filter(q -> q.destroy() == p)
                        .findAny();
                if (notify.isPresent()) {
                    stmt.add("$arenas:T.CLOSE_CB_SYM",
                            "arenas", ClassNames.ARENAS);
                } else {
                    stmt.add("$memorySegment:T.NULL");
                }
            }

            // User_data for destroy_notify
            else if (p.isUserDataParameterForDestroyNotify()) {
                var cbParam = p.getRelatedCallbackParameter();
                var cbName = new TypedValueGenerator(cbParam).getName();
                stmt.add("$arenas:T.cacheArena(_" + cbName + "Scope)",
                        "arenas", ClassNames.ARENAS);
            }

            // User_data
            else if (p.isUserDataParameter())
                stmt.add("$memorySegment:T.NULL");

            // Varargs
            else if (p.varargs())
                stmt.add("varargs");

            // Preprocessing statement
            else if (p.isOutParameter()
                    || (p.anyType() instanceof Type type
                        && type.lookup() instanceof Alias a
                        && a.isValueWrapper()
                        && type.isPointer())) {
                stmt.add("_" + name + "Pointer");
            }

            // Custom interop
            else
                stmt.add(generator.marshalJavaToNative(name));

            // Closing parentheses for null-check
            if (nullCheck)
                stmt.add(")");
        }

        // GError
        if (callable.throws_())
            stmt.add(", _gerror");

        return stmt;
    }

    boolean varargs() {
        var params = callable.parameters();
        return params != null
                && params.parameters().stream().anyMatch(Parameter::varargs);
    }
    
    public MethodSpec generateBitfieldOverload() {
        // Check if this is a (named) constructor
        boolean ctor = callable instanceof Constructor;
        boolean namedCtor = ctor && (!callable.name().equals("new"));

        boolean generic = MethodGenerator.isGeneric(callable);

        // Method name
        String name = ctor
                ? ConstructorGenerator.getName((Constructor) callable, false)
                : MethodGenerator.getName(callable);

        MethodSpec.Builder builder;
        if (ctor && (!namedCtor))
            builder = MethodSpec.constructorBuilder();
        else
            builder = MethodSpec.methodBuilder(name);

        // Javadoc
        if (callable.infoElements().doc() != null) {
            String doc = new DocGenerator(callable.infoElements().doc()).generate();
            builder.addJavadoc(doc);
        }

        // Deprecated annotation
        if (callable.callableAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        // Modifiers
        builder.addModifiers(Modifier.PUBLIC);
        if (callable instanceof Function || namedCtor)
            builder.addModifiers(Modifier.STATIC);
        else if (callable.parent() instanceof Interface)
            builder.addModifiers(Modifier.DEFAULT);

        // Return type
        var returnValue = callable.returnValue();
        if (generic && returnValue.anyType().typeName().equals(ClassNames.G_OBJECT))
            builder.returns(ClassNames.GENERIC_T);
        else if ((!ctor) || namedCtor) {
            var generator = new TypedValueGenerator(returnValue);
            builder.returns(generator.annotated(generator.getType()));
        }

        // Parameters
        generateMethodParameters(builder, generic, false);

        // Exception
        if (callable.callableAttrs().throws_())
            builder.addException(ClassNames.GERROR_EXCEPTION);

        // Call the overloaded method
        PartialStatement stmt = PartialStatement.of("");
        if (ctor && (!namedCtor))
            stmt.add("this");
        else
            stmt.add((returnValue.anyType().isVoid() ? "" : "return ") + name);

        // Set parameters
        StringJoiner params = new StringJoiner(",$W", "(", ");\n");
        for (Parameter p : callable.parameters().parameters()) {
            if (p.isUserDataParameter()
                    || p.isDestroyNotifyParameter()
                    || p.isArrayLengthParameter())
                continue;

            TypedValueGenerator gen = new TypedValueGenerator(p);
            String paramName = gen.getName();

            if (p.isBitfield()) {
                if (p.isOutParameter()) {
                    params.add(paramName + " == null ? null : new $out:T($enumSet:T.of(" + paramName + ".get()))");
                } else if (p.isLastParameter()) {
                    params.add("(" + paramName + " == null ? null : (" + paramName + ".length == 0) ? $enumSet:T.noneOf($typeName:T.class) : $enumSet:T.of(" + paramName + "[0], " + paramName + "))");
                    stmt.add(null, "typeName", gen.getType(false));
                } else {
                    params.add("$enumSet:T.of(" + paramName + ")");
                }
            } else {
                params.add(paramName);
            }
        }
        stmt.add(params.toString(),
                "out", ClassNames.OUT,
                "enumSet", EnumSet.class);
        builder.addNamedCode(stmt.format(), stmt.arguments());
        return builder.build();
    }
}
