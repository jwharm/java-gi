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

package io.github.jwharm.javagi.generators;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import io.github.jwharm.javagi.configuration.ClassNames;
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.Conversions;
import io.github.jwharm.javagi.util.PartialStatement;
import io.github.jwharm.javagi.util.Platform;

import javax.lang.model.element.Modifier;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Comparator;
import java.util.List;

import static io.github.jwharm.javagi.generators.RegisteredTypeGenerator.GENERIC_T;
import static io.github.jwharm.javagi.generators.RegisteredTypeGenerator.GOBJECT;
import static io.github.jwharm.javagi.util.Conversions.*;

public class MethodGenerator {

    private final AbstractCallable func;
    private final VirtualMethod vm;
    private final ReturnValue returnValue;
    private final boolean generic;
    private final MethodSpec.Builder builder;
    private final CallableGenerator generator;

    public MethodGenerator(AbstractCallable func) {
        this(func, getName(func));
    }

    private static String getName(AbstractCallable func) {
        String name = toJavaIdentifier(func.name());
        return func.parent() instanceof Interface
                ? replaceJavaObjectMethodNames(name)
                : name;
    }

    public MethodGenerator(AbstractCallable func, String name) {
        this.func = func;
        this.builder = MethodSpec.methodBuilder(name);
        this.generator = new CallableGenerator(func);

        if (func instanceof Method method) {
            vm = method.invokerFor();
            // Sometimes the return value of the invoker is not the same.
            // In that case we choose the one that isn't void.
            if (vm != null && func.returnValue().anyType().isVoid())
                returnValue = vm.returnValue();
            else
                returnValue = func.returnValue();
        } else if (func instanceof VirtualMethod virtualMethod) {
            vm = virtualMethod;
            returnValue = virtualMethod.returnValue();
        } else {
            vm = null;
            returnValue = func.returnValue();
        }

        generic = switch(func.parent()) {
            case Class c -> c.generic();
            case Record r -> r.generic();
            default -> false;
        };
    }

    public MethodSpec generate() {
        // Javadoc
        if (func.infoElements().doc() != null)
            builder.addJavadoc(new DocGenerator(func.infoElements().doc()).generate());

        // Deprecated annotation
        if (func.attrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        // Modifiers
        switch (func) {
            case VirtualMethod v when v.overrideVisibility() != null ->
                    builder.addModifiers(Modifier.valueOf(v.overrideVisibility()));
            case VirtualMethod _ when func.parent() instanceof Class ->
                    builder.addModifiers(Modifier.PROTECTED);
            case Constructor _ ->
                    builder.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
            default ->
                    builder.addModifiers(Modifier.PUBLIC);
        }

        if (func instanceof Function)
            builder.addModifiers(Modifier.STATIC);
        else if (func.parent() instanceof Interface)
            builder.addModifiers(Modifier.DEFAULT);

        // Return type
        if (generic && returnValue.anyType().typeName().equals(GOBJECT))
            builder.returns(GENERIC_T);
        else if (func instanceof Constructor)
            builder.returns(MemorySegment.class);
        else
            builder.returns(new TypedValueGenerator(returnValue).getType());

        // Parameters
        generator.generateMethodParameters(builder, generic);

        // Exception
        if (func.attrs().throws_())
            builder.addException(ClassNames.GERROR_EXCEPTION);

        // Platform check
        if (func.doPlatformCheck())
            builder.addStatement("$T.checkSupportedPlatform($L)",
                    ClassNames.PLATFORM,
                    Platform.toStringLiterals(func.platforms()));

        // try-block for arena
        if (func.allocatesMemory())
            builder.beginControlFlow("try (var _arena = $T.ofConfined())",
                    Arena.class);

        // Preprocessing
        if (func.parameters() != null)
            func.parameters().parameters().stream()
                    // Array parameters may refer to other parameters for their length,
                    // so they must be processed last.
                    .sorted((Comparator.comparing(p -> p.anyType() instanceof Array)))
                    .forEach(p -> new PreprocessingGenerator(p).generate(builder));

        // Allocate GError
        if (func.attrs().throws_())
            builder.addStatement("$T _gerror = _arena.allocate($T.ADDRESS)",
                    MemorySegment.class,
                    ValueLayout.class);

        // Declare return value
        if (!returnValue.anyType().isVoid())
            builder.addStatement("$T _result",
                    Conversions.getCarrierTypeName(returnValue.anyType()));

        // Try-catch for function invocation
        builder.beginControlFlow("try");

        // Function invocation
        if (vm != null && vm != func) {
            if (func.parent() instanceof Interface)
                builder.beginControlFlow("if ((($T) this).callParent())",
                        ClassName.get("org.gnome.gobject", "TypeInstance"));
            else
                builder.beginControlFlow("if (callParent())");
            functionPointerInvocation();
            builder.nextControlFlow("else");
            functionNameInvocation();
            builder.endControlFlow();
        } else if (vm != null) {
            functionPointerInvocation();
        } else {
            functionNameInvocation();
        }

        // Catch function invocation exceptions
        builder.nextControlFlow("catch (Throwable _err)")
                .addStatement("throw new AssertionError($S, _err)",
                        "Unexpected exception occurred: ")
                .endControlFlow();

        // Throw GErrorException
        if (func.attrs().throws_())
            builder.beginControlFlow("if ($T.isErrorSet(_gerror))",
                            ClassNames.GERROR_EXCEPTION)
                    .addStatement("throw new $T(_gerror)",
                            ClassNames.GERROR_EXCEPTION)
                    .endControlFlow();

        // Postprocessing
        if (func.parameters() != null)
            for (var p : func.parameters().parameters())
                new PostprocessingGenerator(p).generate(builder);

        // Private static helper method for constructors return the result as-is
        if (func instanceof Constructor) {
            builder.addStatement("return _result");
        }

        // Marshal return value and handle ownership transfer
        else if (!returnValue.anyType().isVoid()) {
            RegisteredType target = returnValue.anyType() instanceof Type type ? type.get() : null;
            var generator = new TypedValueGenerator(returnValue);
            PartialStatement stmt = PartialStatement.of("");
            if (generic && returnValue.anyType().typeName().equals(GOBJECT))
                stmt.add("($generic:T) ", "generic", GENERIC_T);
            stmt.add(generator.marshalNativeToJava("_result", false));

            // Ref GObject
            if (target != null && target.checkIsGObject()
                    && returnValue.transferOwnership() == TransferOwnership.NONE
                    && (! "ref".equals(func.name()))) {
                builder.addNamedCode(PartialStatement.of("var _object = ").add(stmt).format() + ";\n", stmt.arguments())
                        .beginControlFlow("if (_object instanceof $T _gobject)",
                                ClassName.get("org.gnome.gobject", "GObject"))
                        .addStatement("$T.debug($S, _gobject.handle())",
                                ClassNames.GLIB_LOGGER, "Ref " + generator.getType() + " %ld\\n")
                        .addStatement("_gobject.ref()")
                        .endControlFlow()
                        .addStatement("return _object");
            }

            // Add cleaner to struct/union pointer
            else if (target instanceof Record record && (! List.of(
                    "org.gnome.gobject.TypeInstance",
                    "org.gnome.gobject.TypeClass",
                    "org.gnome.gobject.TypeInterface").contains(target.javaType()))) {
                builder.addNamedCode(PartialStatement.of("var _instance = ").add(stmt).add(";\n").format(), stmt.arguments())
                        .beginControlFlow("if (_instance != null)")
                        .addStatement("$T.takeOwnership(_instance.handle())", ClassNames.MEMORY_CLEANER);
                new RecordGenerator(record).setFreeFunc(builder, "_instance", target.typeName());
                builder.endControlFlow()
                        .addStatement("return _instance");
            }

            // No ownership transfer, just marshal the return value
            else {
                builder.addNamedCode(PartialStatement.of("return ").add(stmt).format() + ";\n", stmt.arguments());
            }
        }

        // End try-block for arena
        if (func.allocatesMemory())
            builder.endControlFlow();

        return builder.build();
    }

    private void functionNameInvocation() {
        // Function descriptor
        generator.generateFunctionDescriptor(builder);

        // Result assignment
        PartialStatement invoke = new PartialStatement();
        if (!func.returnValue().anyType().isVoid())
            invoke.add("_result = (")
                    .add(Conversions.getCarrierTypeString(func.returnValue().anyType()))
                    .add(") ");

        // Function invocation
        invoke.add("$interop:T.downcallHandle($cIdentifier:S, _fdesc, $variadic:L).invokeExact(",
                        "interop", ClassNames.INTEROP,
                        "cIdentifier", func.attrs().cIdentifier(),
                        "variadic", generator.varargs())
                .add(generator.marshalParameters())
                .add(");\n");

        builder.addNamedCode(invoke.format(), invoke.arguments());

        // Override result with a default value
        if (!returnValue.equals(func.returnValue())) {
            String value = "null";
            if ("gboolean".equals(returnValue.anyType().name()))
                value = "1";
            builder.addStatement("_result = $L", value);
        }
    }

    private void functionPointerInvocation() {
        // Function descriptor
        var generator = new CallableGenerator(vm);
        generator.generateFunctionDescriptor(builder);

        // Function pointer lookup
        switch (vm.parent()) {
            case Class c ->
                    builder.addStatement("$T _func = $T.lookupVirtualMethodParent(handle(), $L.getMemoryLayout(), $S)",
                            MemorySegment.class,
                            ClassNames.OVERRIDES,
                            toJavaSimpleType(c.typeStruct().name(), c.namespace()), vm.name());
            case Interface i ->
                    builder.addStatement("$T _func = $T.lookupVirtualMethodParent(handle(), $T.getMemoryLayout(), $S, $T.getType())",
                            MemorySegment.class,
                            ClassNames.OVERRIDES,
                            i.typeStruct().typeName(),
                            vm.name(),
                            i.typeName());
            default -> throw new IllegalStateException("Virtual Method parent must be a class or an interface");
        }

        // Function pointer null-check
        builder.addStatement("if (_func.equals($T.NULL)) throw new $T()",
                MemorySegment.class,
                NullPointerException.class);

        // Result assignment
        PartialStatement invoke = new PartialStatement();
        if (!returnValue.anyType().isVoid())
            invoke.add("_result = (")
                    .add(Conversions.getCarrierTypeString(returnValue.anyType()))
                    .add(") ");

        // Function pointer invocation
        invoke.add("$interop:T.downcallHandle(_func, _fdesc).invokeExact(", "interop",
                        ClassNames.INTEROP)
                .add(generator.marshalParameters())
                .add(");\n");

        builder.addNamedCode(invoke.format(), invoke.arguments());
    }
}
