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
import org.javagi.gir.*;
import org.javagi.util.Conversions;
import org.javagi.util.PartialStatement;
import org.javagi.gir.Class;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.Modifier;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.function.Predicate;

import static org.javagi.util.Conversions.*;
import static java.util.Comparator.comparing;

public class MethodGenerator {

    private final Callable func;
    private final VirtualMethod vm;
    private final ReturnValue returnValue;
    private final boolean generic;
    private final MethodSpec.Builder builder;
    private final CallableGenerator generator;

    public MethodGenerator(Callable func) {
        this(func, getName(func));
    }

    public static String getName(Callable func) {
        return toJavaIdentifier(func.name());
    }

    public static boolean isGeneric(Callable func) {
        return func.parent() instanceof RegisteredType rt
                && rt.generic();
    }

    public MethodGenerator(Callable func, String name) {
        this.func = func;
        this.builder = MethodSpec.methodBuilder(name);
        this.generator = new CallableGenerator(func);
        this.generic = isGeneric(func);

        if (func instanceof Method method) {
            vm = method.invokerFor();
            // When the return value of the invoker is not the same, we choose
            // the one that isn't void.
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
    }

    public FieldSpec generateNamedDowncallHandle(Modifier... modifiers) {
        return FieldSpec.builder(
                        MethodHandle.class,
                        func.callableAttrs().cIdentifier(),
                        modifiers)
                .initializer(CodeBlock.builder()
                        .add("$T.downcallHandle($Z$S,$W",
                                ClassNames.INTEROP,
                                func.callableAttrs().cIdentifier())
                        .add(generator.generateFunctionDescriptor())
                        .add(",$W$L)", generator.varargs())
                        .build())
                .build();
    }

    public MethodSpec generate() {
        // Javadoc
        if ((! (func instanceof Constructor)) // not for private constructor helper methods
                && (func.infoElements().doc() != null)) {
            String javadoc = new DocGenerator(func.infoElements().doc()).generate();
            builder.addJavadoc(javadoc);
        }

        // Deprecated annotation
        if (func.callableAttrs().deprecated())
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
        if (generic && returnValue.anyType().typeName().equals(ClassNames.G_OBJECT))
            builder.returns(ClassNames.GENERIC_T);
        else if (func instanceof Constructor)
            builder.returns(MemorySegment.class);
        else {
            TypeName tn = new TypedValueGenerator(returnValue).getType();
            if (returnValue.nullable())
                builder.returns(tn.annotated(AnnotationSpec.builder(Nullable.class).build()));
            else
                builder.returns(tn);
        }

        // Parameters
        generator.generateMethodParameters(builder, generic, true);

        // Exception
        if (func.callableAttrs().throws_())
            builder.addException(ClassNames.GERROR_EXCEPTION);

        // try-block for arena
        if (func.allocatesMemory())
            builder.beginControlFlow("try (var _arena = $T.ofConfined())",
                    Arena.class);

        // When ownership of the instance parameter is transferred away, the
        // instance is consumed. Add a ref() call to prevent this.
        if (func.parameters() != null
                && func.parameters().instanceParameter() != null
                && func.parameters().instanceParameter().transferOwnership() == TransferOwnership.FULL) {
            if (func.parameters().instanceParameter().type().lookup().checkIsGObject())
                builder.addStatement("ref()");
            else
                builder.addStatement("$T.yieldOwnership(this)", ClassNames.MEMORY_CLEANER);
        }

        // Preprocessing
        if (func.parameters() != null)
            func.parameters().parameters().stream()
                    // Array parameters may refer to other parameters for their
                    // length, so they must be processed last.
                    .sorted((comparing(p -> p.anyType() instanceof Array)))
                    .map(PreprocessingGenerator::new)
                    .forEach(p -> p.generate(builder));

        // Allocate GError
        if (func.callableAttrs().throws_())
            builder.addStatement("$T _gerror = _arena.allocate($T.ADDRESS)",
                    MemorySegment.class,
                    ValueLayout.class);

        // Declare return value
        if (!returnValue.anyType().isVoid())
            builder.addStatement("$T _result",
                    Conversions.getCarrierTypeName(returnValue.anyType(), true));

        // Try-catch for function invocation
        builder.beginControlFlow("try");

        // Function invocation
        if (vm != null && vm != func) {
            if (func.parent() instanceof Interface)
                builder.beginControlFlow("if ((($T) this).callParent())",
                        ClassNames.G_TYPE_INSTANCE);
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

        // Wrap function invocation exceptions into runtime AssertionErrors
        builder.nextControlFlow("catch (Throwable _err)")
                .addStatement("throw new AssertionError(_err)")
                .endControlFlow();

        // Throw GErrorException
        if (func.callableAttrs().throws_())
            builder.beginControlFlow("if ($T.isErrorSet(_gerror))",
                            ClassNames.GERROR_EXCEPTION)
                    .addStatement("throw new $T(_gerror)",
                            ClassNames.GERROR_EXCEPTION)
                    .endControlFlow();

        // Postprocessing
        if (func.parameters() != null)
            func.parameters().parameters().stream()
                    // Process Array parameters last
                    .sorted((comparing(p -> p.anyType() instanceof Array)))
                    .map(PostprocessingGenerator::new)
                    .forEach(p -> p.generate(builder));

        // Private static helper method for constructors return the result as-is
        if (func instanceof Constructor) {
            builder.addStatement("return _result");
        }

        // Marshal return value and handle ownership transfer
        else if (!returnValue.anyType().isVoid()) {
            var generator = new PostprocessingGenerator(returnValue);
            // First check for NULL
            if (generator.checkNull()) {
                builder.beginControlFlow("if (_result == null || _result.equals($T.NULL))",
                                MemorySegment.class)
                        .addStatement("return null")
                        .endControlFlow();
            }

            // If this is an instance method and returns an instance of the
            // surrounding class, check if we can simply "return this".
            if (couldReturnThis()) {
                builder.beginControlFlow("if (handle().equals(_result))")
                        .addStatement("return this")
                        .endControlFlow();
            }

            // Marshal "_result" to "_returnValue"
            marshalReturnValue();

            // Generate postprocessing statements
            generator.generate(builder);

            // Return "_returnValue"
            builder.addStatement("return _returnValue");
        }

        // End try-block for arena
        if (func.allocatesMemory())
            builder.endControlFlow();

        return builder.build();
    }

    // Prepare a statement that marshals the return value to Java
    private void marshalReturnValue() {
        var isGeneric = generic && returnValue.anyType().typeName().equals(ClassNames.G_OBJECT);
        var generator = new TypedValueGenerator(returnValue);
        var typeName = isGeneric ? ClassNames.GENERIC_T : generator.getType();
        PartialStatement stmt = PartialStatement.of("$returnType:T", "returnType", typeName);
        stmt.add(" _returnValue = ");
        if (isGeneric)
            stmt.add("($generic:T) ", "generic", ClassNames.GENERIC_T);
        stmt.add(generator.marshalNativeToJava("_result", false));
        stmt.add(";\n");
        builder.addNamedCode(stmt.format(), stmt.arguments());
    }

    // Check if this is an instance method that returns an instance of the
    // surrounding class
    private boolean couldReturnThis() {
        if (func instanceof Method || func instanceof VirtualMethod) {
            var returnType = returnValue.anyType().typeName();
            var thisType = ((RegisteredType) func.parent()).typeName();
            return returnType.equals(thisType);
        }
        return false;
    }

    private void functionNameInvocation() {
        Predicate<Node> predicate = n -> n instanceof Type t && t.isLong();
        if (func.deepMatch(predicate, Callback.class)) {
            builder.beginControlFlow("if ($T.longAsInt())", ClassNames.INTEROP);
            functionNameInvocation(false);
            builder.nextControlFlow("else");
            functionNameInvocation(true);
            builder.endControlFlow();
        } else {
            functionNameInvocation(false);
        }
    }

    private void functionNameInvocation(boolean longAsInt) {
        // Result assignment
        PartialStatement invoke = new PartialStatement();
        var returnType = func.returnValue().anyType();
        if (!returnType.isVoid()) {
            if (longAsInt && returnType instanceof Type t && t.isLong()) {
                // First cast to long, this is used by the MethodHandle to
                // determine the return type. Then cast to int, because that is
                // returned to the caller.
                invoke.add("_result = (int) (long) ");
            } else {
                String typeTag = getCarrierTypeTag(func.returnValue().anyType());
                TypeName typeName = getCarrierTypeName(func.returnValue().anyType(), false);
                invoke.add("_result = ($" + typeTag + ":T) ", typeTag, typeName);
            }
        }

        // Function invocation
        invoke.add("$helperClass:T.$cIdentifier:L.invokeExact($Z",
                        "helperClass", ((RegisteredType) func.parent()).helperClass(),
                        "cIdentifier", func.callableAttrs().cIdentifier())
                .add(generator.marshalParameters(longAsInt))
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
        Predicate<Node> predicate = n -> n instanceof Type t && t.isLong();
        if (func.deepMatch(predicate, Callback.class)) {
            builder.beginControlFlow("if ($T.longAsInt())", ClassNames.INTEROP);
            functionPointerInvocation(true);
            builder.nextControlFlow("else");
            functionPointerInvocation(false);
            builder.endControlFlow();
        } else {
            functionPointerInvocation(false);
        }
    }

    private void functionPointerInvocation(boolean longAsInt) {
        // Base the generator on func, not on vm, because sometimes the virtual
        // method has different parameter names than the invoker method
        var generator = new CallableGenerator(func);

        // Generate function descriptor
        builder.addCode(generator.generateFunctionDescriptorDeclaration());

        // Function pointer lookup
        switch (vm.parent()) {
            case Class c ->
                    builder.addStatement("$T _func = $T.lookupVirtualMethodParent(handle(),$W$T.getMemoryLayout(),$W$S)",
                            MemorySegment.class,
                            ClassNames.OVERRIDES,
                            c.typeStruct().typeName(),
                            vm.name());
            case Interface i ->
                    builder.addStatement("$T _func = $T.lookupVirtualMethodParent(handle(),$W$T.getMemoryLayout(),$W$S,$W$T.getType())",
                            MemorySegment.class,
                            ClassNames.OVERRIDES,
                            i.typeStruct().typeName(),
                            vm.name(),
                            i.typeName());
            default -> throw new IllegalStateException("Virtual Method parent must be a class or an interface");
        }

        // Result assignment
        PartialStatement invoke = new PartialStatement();
        var returnType = returnValue.anyType();
        if (!returnType.isVoid()) {
            if (longAsInt && returnType instanceof Type t && t.isLong()) {
                // First cast to long, this is used by the MethodHandle to
                // determine the return type. Then cast to int, because that is
                // returned to the caller.
                invoke.add("_result = (int) (long) ");
            } else {
                String typeTag = getCarrierTypeTag(returnValue.anyType());
                TypeName typeName = getCarrierTypeName(returnValue.anyType(),
                        false);
                invoke.add("_result = ($" + typeTag + ":T) ", typeTag,
                        typeName);
            }
        }

        // Function pointer invocation
        invoke.add("$interop:T.downcallHandle(_func, _fdesc)$Z.invokeExact($Z", "interop",
                        ClassNames.INTEROP)
                .add(generator.marshalParameters(longAsInt))
                .add(");\n");

        builder.addNamedCode(invoke.format(), invoke.arguments());
    }
}
