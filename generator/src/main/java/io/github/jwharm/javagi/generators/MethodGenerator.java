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

import com.squareup.javapoet.*;
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
import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.function.Predicate;

import static io.github.jwharm.javagi.util.Conversions.*;
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
        String name = toJavaIdentifier(func.name());
        return replaceJavaObjectMethodNames(name, func.parent() instanceof Interface);
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
            if (func instanceof Multiplatform mp && mp.doPlatformCheck())
                builder.addException(ClassNames.UNSUPPORTED_PLATFORM_EXCEPTION)
                        .addJavadoc(javadoc, ClassNames.UNSUPPORTED_PLATFORM_EXCEPTION);
            else
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
        else
            builder.returns(new TypedValueGenerator(returnValue).getType());

        // Parameters
        generator.generateMethodParameters(builder, generic, true);

        // Exception
        if (func.callableAttrs().throws_())
            builder.addException(ClassNames.GERROR_EXCEPTION);

        // Platform check
        if (func instanceof Multiplatform mp && mp.doPlatformCheck())
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
            generateOwnershipTransfer();
        }

        // End try-block for arena
        if (func.allocatesMemory())
            builder.endControlFlow();

        return builder.build();
    }

    private void generateOwnershipTransfer() {
        // Prepare a statement that marshals the return value to Java
        RegisteredType target = returnValue.anyType() instanceof Type type
                ? type.lookup() : null;
        var generator = new TypedValueGenerator(returnValue);
        PartialStatement stmt = PartialStatement.of("");
        if (generic && returnValue.anyType().typeName().equals(ClassNames.G_OBJECT))
            stmt.add("($generic:T) ", "generic", ClassNames.GENERIC_T);
        stmt.add(generator.marshalNativeToJava("_result", false));

        // Ref GObject when ownership is not transferred
        if (target != null && target.checkIsGObject()
                && returnValue.transferOwnership() == TransferOwnership.NONE
                // don't call ref() from ref() itself
                && (! "ref".equals(func.name()))
                && (! "ref_sink".equals(func.name()))) {
            builder.addNamedCode(PartialStatement.of("var _object = ")
                    .add(stmt).format() + ";\n", stmt.arguments())
                    .beginControlFlow("if (_object instanceof $T _gobject)",
                            ClassNames.G_OBJECT)
                    .addStatement("$T.debug($S, _gobject.handle().address())",
                            ClassNames.GLIB_LOGGER,
                            "Ref " + generator.getType() + " %ld")
                    .addStatement("_gobject.ref()")
                    .endControlFlow()
                    .addStatement("return _object");
        }

        // Add cleaner to struct/union pointer.
        // * Exclude foreign types
        // * GList/GSList have their own cleaner
        // * GTypeInstance/Class/Interface are special cases
        else if (((target instanceof Record record
                        && !record.foreign()
                        && !record.checkIsGList())
                    || target instanceof Boxed
                    || target instanceof Union)
                && (!List.of("org.gnome.gobject.TypeInstance",
                             "org.gnome.gobject.TypeClass",
                             "org.gnome.gobject.TypeInterface")
                        .contains(target.javaType()))) {

            // With ownership transfer: Don't copy/ref the struct
            if (returnValue.transferOwnership() != TransferOwnership.NONE) {
                builder.addNamedCode(PartialStatement.of("var _instance = ")
                                .add(stmt).add(";\n").format(),
                                stmt.arguments())
                        .beginControlFlow("if (_instance != null)")
                        .addStatement("$T.takeOwnership(_instance)",
                                ClassNames.MEMORY_CLEANER);
                new RegisteredTypeGenerator(target)
                        .setFreeFunc(builder, "_instance", target.typeName());
                builder.endControlFlow();
            }

            // No ownership transfer: Copy/ref the struct
            else {
                // First check for NULL
                if (returnValue.nullable()) {
                    builder.beginControlFlow("if (_result == null || _result.equals($T.NULL))",
                                    MemorySegment.class)
                            .addStatement("return null")
                            .endControlFlow();
                }

                // Lookup the copy/ref function and the memory layout
                var slt = (StandardLayoutType) target;
                var copyFunc = slt.copyFunction();
                var hasMemoryLayout = slt instanceof FieldContainer fc
                        && new MemoryLayoutGenerator().canGenerate(fc);

                // Don't automatically copy the return values of GLib functions
                var skipNamespace = List.of("GLib", "GModule")
                        .contains(target.namespace().name());

                // No copy function, and unknown size: copying is impossible
                if (skipNamespace || (!hasMemoryLayout && copyFunc == null)) {
                    builder.addNamedCode(PartialStatement.of("return ")
                            .add(stmt)
                            .add(";\n").format(), stmt.arguments());
                    return;
                }

                // don't copy the result of ref() or copy()
                if ("ref".equals(func.name()) || "copy".equals(func.name())
                        || (copyFunc != null && copyFunc.name().equals(func.name()))) {
                    builder.addNamedCode(PartialStatement.of("var _instance = ")
                            .add(stmt)
                            .add(";\n").format(), stmt.arguments());
                }

                // No copy function, but know memory layout size: malloc() a new
                // struct, and copy the contents manually
                else if (hasMemoryLayout && copyFunc == null) {
                    builder.addStatement("$T _copy = $T.malloc($T.getMemoryLayout().byteSize())",
                            MemorySegment.class,
                            ClassNames.G_LIB,
                            returnValue.anyType().typeName());
                    stmt = PartialStatement.of("var _instance = ")
                            .add(generator.marshalNativeToJava("_copy", false))
                            .add(";\n");
                    builder.addNamedCode(stmt.format(), stmt.arguments());
                    builder.addStatement("$T.copy(_result, _instance.handle())",
                            ClassNames.INTEROP);
                }

                // Copy function is an instance method
                else if (copyFunc instanceof Method m) {
                    builder.addNamedCode(PartialStatement.of("var _instance = ")
                            .add(stmt)
                            .add("." + getName(m) + "()")
                            .add(";\n").format(), stmt.arguments());
                }

                // Copy function is a function (static method)
                else if (copyFunc instanceof Function f
                        && f.parent() instanceof RegisteredType rt) {
                    // Call g_boxed_copy
                    if ("g_boxed_copy".equals(f.callableAttrs().cIdentifier())) {
                        builder.addStatement(
                                "_result = $T.$L($T.getType(), _result)",
                                rt.typeName(),
                                getName(f),
                                returnValue.anyType().typeName());
                        builder.addNamedCode(
                                PartialStatement.of("var _instance = ")
                                        .add(stmt).add(";\n")
                                        .format(),
                                stmt.arguments());
                    }
                    // Call the copy/ref function
                    else {
                        builder.addStatement("var _instance = $T.$L(_instance)",
                                rt.typeName(),
                                getName(f));
                    }
                }

                // Register the returned instance with the memory cleaner
                builder.addStatement("$T.takeOwnership(_instance)",
                        ClassNames.MEMORY_CLEANER);
                new RegisteredTypeGenerator(target)
                        .setFreeFunc(builder, "_instance", target.typeName());
            }

            builder.addStatement("return _instance");
        }

        // No ownership transfer, just marshal the return value
        else {
            builder.addNamedCode(PartialStatement.of("return ")
                    .add(stmt).format() + ";\n", stmt.arguments());
        }
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
        // Function descriptor
        var generator = new CallableGenerator(vm);
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

        // Function pointer null-check
        builder.addStatement("if (_func.equals($T.NULL)) throw new $T()",
                MemorySegment.class,
                NullPointerException.class);

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
