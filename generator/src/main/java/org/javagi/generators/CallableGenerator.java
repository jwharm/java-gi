/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2026 the Java-GI developers
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

import org.javagi.gir.Class;
import org.javagi.javapoet.*;
import org.javagi.configuration.ClassNames;
import org.javagi.gir.*;

import javax.lang.model.element.Modifier;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.*;

import static org.javagi.util.Conversions.getValueLayout;
import static java.util.function.Predicate.not;

public class CallableGenerator {

    private final Callable callable;

    public CallableGenerator(Callable callable) {
        this.callable = callable;
    }

    CodeBlock generateFunctionDescriptorDeclaration() {
        return CodeBlock.of("$T _fdesc = $L", FunctionDescriptor.class, generateFunctionDescriptor());
    }

    CodeBlock generateFunctionDescriptor() {
        List<CodeBlock> layouts = new ArrayList<>();
        var returnType = callable.returnValue().anyType();
        boolean isVoid = returnType instanceof Type t && t.isVoid();
        if (!isVoid)
            layouts.add(generateValueLayoutWithFixForSignals(returnType));

        if (callable instanceof Signal)
            layouts.add(CodeBlock.of("$T.ADDRESS", ValueLayout.class));

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
            layouts.add(CodeBlock.of("$T.ADDRESS", ValueLayout.class));

        if (layouts.isEmpty())
            return CodeBlock.of("$T.ofVoid()", FunctionDescriptor.class);

        return CodeBlock.builder().add("$T.$L", FunctionDescriptor.class, isVoid ? "ofVoid" : "of")
                .add(layouts.stream().collect(CodeBlock.joining(",$W", "(", ")")))
                .build();
    }

    /**
     * For plain struct types (i.e. struct, union, boxed types) that are not
     * passed by reference but by value, the FunctionDescriptor normally
     * contains the complete memory layout of the struct. But for signals,
     * it must be ValueLayout.ADDRESS. I'm not sure why, but the JVM segfaults
     * otherwise.
     */
    CodeBlock generateValueLayoutWithFixForSignals(AnyType anyType) {
        if (callable instanceof Signal
                && anyType instanceof Type t
                && !t.isPointer()
                && t.lookup() instanceof FieldContainer fc
                && new MemoryLayoutGenerator().canGenerate(fc)) {
            return CodeBlock.of("$T.ADDRESS", ValueLayout.class);
        }
        return generateValueLayout(anyType);
    }

    CodeBlock generateValueLayout(AnyType anyType) {
        return anyType instanceof Type type && type.isLong() && !type.isPointer()
                ? CodeBlock.of("$1T.longAsInt() ? $2T.JAVA_INT : $2T.JAVA_LONG", ClassNames.INTEROP, ValueLayout.class)
                : getValueLayout(anyType, false);
    }

    void generateMethodParameters(MethodSpec.Builder builder, boolean generic, boolean useActualType) {
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
                var type = generator.getAnnotatedType(useActualType);

                // Trailing flags parameter can be variadic
                if ((!useActualType)
                        && p.isBitfield()
                        && p.isLastParameter()
                        && (!p.isOutParameter())) {
                    type = generator.annotated(ArrayTypeName.of(type));
                    builder.varargs(true);
                }

                if (generic && type.equals(ClassNames.G_OBJECT))
                    type = ClassNames.GENERIC_T;

                var spec = ParameterSpec.builder(type, generator.getName());
                builder.addParameter(spec.build());
            }
        }
    }

    CodeBlock marshalParameters(boolean intAsLong) {
        var parameters = callable.parameters();
        if (parameters == null)
            return callable.throws_()
                    ? CodeBlock.of("_gerror")
                    : CodeBlock.of("");

        CodeBlock.Builder stmt = CodeBlock.builder();
        boolean first = true;

        // Marshal instance parameter
        InstanceParameter iParam = parameters.instanceParameter();
        if (iParam != null) {
            if (iParam.type().lookup() instanceof EnumType)
                stmt.add("getValue()"); // method in Enumeration class
            else
                stmt.add("handle()");   // method in regular TypeInstance class
            first = false;
        }

        // Marshal other parameters
        for (Parameter p : parameters.parameters()) {
            if (first)
                first = false;
            else
                stmt.add(",$W");

            var generator = new TypedValueGenerator(p);
            var name = generator.getName();

            // Generate null-check. But don't null-check parameters that are
            // hidden from the Java API, or primitive values
            boolean nullCheck = generator.checkNull();

            // Always translate a `null` GList/GSlist to native NULL
            if (p.anyType() instanceof Type t && t.checkIsGList())
                nullCheck = true;

            if (nullCheck)
                stmt.add("($1T) ($2L == null ? $1T.NULL : ", MemorySegment.class, name);

            // cast int parameter to a long
            if (intAsLong && p.anyType() instanceof Type t && t.isLong()
                    && !p.isOutParameter() && !t.isUnannotatedReference())
                stmt.add("(long) ");

            // Callback destroy
            if (p.isDestroyNotifyParameter()) {
                var notify = parameters.parameters().stream()
                        .filter(q -> q.destroy() == p)
                        .findAny();
                if (notify.isPresent()) {
                    stmt.add("$T.CLOSE_CB_SYM", ClassNames.ARENAS);
                } else {
                    stmt.add("$T.NULL", MemorySegment.class);
                }
            }

            // User_data for destroy_notify
            else if (p.isUserDataParameterForDestroyNotify()) {
                var cbParam = p.getRelatedCallbackParameter();
                var cbName = new TypedValueGenerator(cbParam).getName();
                stmt.add("$T.cacheArena(_$LScope)", ClassNames.ARENAS, cbName);
            }

            // User_data
            else if (p.isUserDataParameter())
                stmt.add("$T.NULL", MemorySegment.class);

            // Varargs
            else if (p.varargs())
                stmt.add("varargs");

            // Pointer allocation for out-parameter
            else if (p.isOutParameter()) {
                stmt.add("_$LPointer", name);
            }

            // Custom interop
            else
                stmt.add(generator.marshalJavaToNative(CodeBlock.of(name)));

            // Closing parentheses for null-check
            if (nullCheck)
                stmt.add(")");
        }

        // GError
        if (callable.throws_())
            stmt.add(", _gerror");

        return stmt.build();
    }

    boolean varargs() {
        var params = callable.parameters();
        return params != null && params.parameters().stream().anyMatch(Parameter::varargs);
    }

    public void generateModifiers(MethodSpec.Builder builder, boolean isConstructor) {
        switch (callable) {
            case Constructor _ -> {
                builder.addModifiers(Modifier.PUBLIC);
                if (!isConstructor)
                    builder.addModifiers(Modifier.STATIC);
            }
            case Function _ ->
                    builder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
            case Method m -> {
                builder.addModifiers(Modifier.PUBLIC);
                if (m.parent() instanceof Interface)
                    builder.addModifiers(Modifier.DEFAULT);
            }
            case VirtualMethod v -> {
                if (v.overrideVisibility() != null)
                    builder.addModifiers(Modifier.valueOf(v.overrideVisibility()));
                else if (v.parent() instanceof Class)
                    builder.addModifiers(Modifier.PROTECTED);
                else
                    builder.addModifiers(Modifier.PUBLIC);
                if (v.parent() instanceof Interface)
                    builder.addModifiers(Modifier.DEFAULT);
            }
            case Callback _, Signal _ ->
                    builder.addModifiers(Modifier.PUBLIC);
        }
    }

    public MethodSpec generateOverload() {
        boolean isConstructor = callable instanceof Constructor c && !c.isNamed();
        boolean generic = MethodGenerator.isGeneric(callable);
        String name = MethodGenerator.getName(callable);
        boolean first = true;

        MethodSpec.Builder builder;
        if (isConstructor)
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
        generateModifiers(builder, isConstructor);

        // Return type
        var returnValue = callable.returnValue();
        if (generic && returnValue.anyType().typeName().equals(ClassNames.G_OBJECT))
            builder.returns(ClassNames.GENERIC_T);
        else if (!isConstructor) {
            var generator = new TypedValueGenerator(returnValue);
            builder.returns(generator.getAnnotatedType(true));
        }

        // Parameters
        generateMethodParameters(builder, generic, false);

        // Exception
        if (callable.callableAttrs().throws_())
            builder.addException(ClassNames.GERROR_EXCEPTION);

        // Call the overloaded method
        CodeBlock.Builder stmt = CodeBlock.builder();
        if (isConstructor)
            stmt.add("this(");
        else
            stmt.add(returnValue.anyType().isVoid() ? "$L(" : "return $L(", name);

        // Set parameters
        for (Parameter p : callable.parameters().parameters()) {
            if (p.isUserDataParameter()
                    || p.isDestroyNotifyParameter()
                    || p.isArrayLengthParameter())
                continue;

            TypedValueGenerator gen = new TypedValueGenerator(p);
            String paramName = gen.getName();

            if (first)
                first = false;
            else
                stmt.add(",$W");

            // Create an EnumSet<> object for bitfield parameters
            if (p.isBitfield()) {
                if (p.isOutParameter())
                    stmt.add("$1L == null ? null : new $2T($3T.of($1L.get()))",
                            paramName, ClassNames.OUT, EnumSet.class);
                else if (p.isLastParameter())
                    stmt.add("($1L == null ? null : ($1L.length == 0) ? $2T.noneOf($3T.class) : $2T.of($1L[0], $1L))",
                            paramName, EnumSet.class, gen.getType(false));
                else
                    stmt.add("$T.of($L)", EnumSet.class, paramName);
            }

            // Create a Filename object for filename parameters
            else if (p.anyType() instanceof AnyType at && at.isFilename() && !(p.isOutParameter())) {
                switch (p.anyType()) {
                    case Array _ -> stmt.add("$T.convertArray($L)", ClassNames.FILENAME, paramName);
                    case Type _ -> stmt.add("new $T($L)", ClassNames.FILENAME, paramName);
                }
            }

            // All other parameters are forwarded as-is
            else
                stmt.add(paramName);
        }
        stmt.add(")");

        return builder.addStatement(stmt.build())
                      .build();
    }
}
