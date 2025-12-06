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

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.javagi.configuration.ClassNames;
import org.javagi.util.GeneratedAnnotationBuilder;
import org.javagi.util.PartialStatement;
import org.javagi.gir.*;

import javax.lang.model.element.Modifier;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.javagi.util.Conversions.*;
import static java.util.Comparator.comparing;

public class ClosureGenerator {

    private final Callable closure;
    private final CallableGenerator generator;
    private final ReturnValue returnValue;

    public ClosureGenerator(Callable closure) {
        this.closure = closure;
        this.generator = new CallableGenerator(closure);
        this.returnValue = closure.returnValue();
    }

    TypeSpec generateFunctionalInterface() {
        String name = getName();
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(name)
                .addSuperinterface(ClassNames.FUNCTION_POINTER)
                .addJavadoc("""
                        Functional interface declaration of the {@code $1L} callback.
                        <p>
                        @see $1L#run
                        """, name)
                .addAnnotation(FunctionalInterface.class)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addMethod(generateRunMethod())
                .addMethod(generateUpcallMethod(name, "upcall", "run"))
                .addMethod(generateToCallbackMethod(name));

        if (closure.deprecated())
            builder.addAnnotation(Deprecated.class);

        if (closure instanceof Callback cb && cb.parent() instanceof Namespace)
            builder.addAnnotation(GeneratedAnnotationBuilder.generate());

        return builder.build();
    }

    private String getName() {
        String name = closure.name();
        if ((closure instanceof Callback cb && cb.parent() instanceof Field)
            || (closure instanceof Signal))
            name += "Callback";
        return toJavaSimpleType(name, closure.namespace());
    }

    MethodSpec generateRunMethod() {
        MethodSpec.Builder run = MethodSpec.methodBuilder("run")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(new TypedValueGenerator(returnValue).getType());

        if (closure.deprecated())
            run.addAnnotation(Deprecated.class);
        if (closure.infoElements().doc() != null)
            run.addJavadoc(new DocGenerator(closure.infoElements().doc()).generate());
        if (closure.throws_())
            run.addException(ClassNames.GERROR_EXCEPTION);

        generator.generateMethodParameters(run, false, true);
        return run.build();
    }

    MethodSpec generateUpcallMethod(String methodName, String name, String methodToInvoke) {
        boolean returnsVoid = returnValue.anyType().isVoid();

        // Method name and return type
        MethodSpec.Builder upcall = MethodSpec.methodBuilder(name)
                .returns(returnsVoid
                        ? TypeName.VOID
                        : returnValue.anyType() instanceof Type t && t.isLong() ? TypeName.LONG
                        : getCarrierTypeName(returnValue.anyType(), false));

        // Javadoc
        if (methodToInvoke.equals("run"))
            upcall.addJavadoc("""
                    The {@code upcall} method is called from native code. The parameters
                    are marshaled and {@link #run} is executed.
                    """);

        // Visibility
        if (methodToInvoke.equals("run"))
            upcall.addModifiers(Modifier.PUBLIC, Modifier.DEFAULT);
        else
            upcall.addModifiers(Modifier.PRIVATE);

        // Add source parameter to signal callback method
        if (closure instanceof Signal signal) {
            String paramName = "source" + toCamelCase(signal.parent().name(), true);
            upcall.addParameter(TypeName.get(MemorySegment.class), paramName);
        }

        // Add parameters (native carrier types)
        if (closure.parameters() != null) {
            for (Parameter p : closure.parameters().parameters()) {
                var generator = new TypedValueGenerator(p);
                upcall.addParameter(getCarrierTypeName(p.anyType(), false),
                        generator.getName());
            }
        }

        // GError** parameter
        if (closure.throws_())
            upcall.addParameter(MemorySegment.class, "_gerrorPointer");

        // Try-catch block for uncaught exceptions
        upcall.beginControlFlow("try");

        /*
         * In the upcall method, memory allocations are only necessary for
         * memory segments that are returned back to the caller. Therefore,
         * we cannot use a confined arena, but have to fall back to the "auto"
         * arena and let the GC close it.
         */
        if (closure.allocatesMemory())
            upcall.addStatement("$1T _arena = $1T.ofAuto()", Arena.class);

        // Parameter preprocessing
        if (closure.parameters() != null)
            closure.parameters().parameters().stream()
                    // Array parameters may refer to other parameters for their
                    // length, so they must be processed last.
                    .sorted(comparing(p -> p.anyType() instanceof Array))
                    .map(PreprocessingGenerator::new)
                    .forEach(p -> p.generateUpcall(upcall));

        // Try-block for GErrorExceptions
        if (closure.throws_())
            upcall.beginControlFlow("try");

        // Callback invocation
        PartialStatement invoke = new PartialStatement();
        if (!returnsVoid) {
            invoke.add("var _result = ");
            if (methodToInvoke.endsWith("invoke"))
                invoke.add("(")
                      .add("$ret:T", "ret", new TypedValueGenerator(returnValue).getType())
                      .add(") ");
        }
        invoke.add(methodToInvoke)
              .add("(")
              .add(marshalParameters(methodToInvoke))
              .add(");\n");
        upcall.addNamedCode(invoke.format(), invoke.arguments());

        // Parameter postprocessing
        if (closure.parameters() != null)
            for (var p : closure.parameters().parameters())
                new PostprocessingGenerator(p).generateUpcall(upcall);

        // Null-check the return value
        if ((!returnsVoid)
                && getCarrierTypeName(returnValue.anyType(), false).equals(TypeName.get(MemorySegment.class))
                && (!returnValue.notNull()))
            upcall.addStatement("if (_result == null) return $T.NULL",
                    MemorySegment.class);

        // Ref returned GObjects when ownership is transferred to the caller
        if (returnValue.anyType() instanceof Type t && t.checkIsGObject()
                && returnValue.transferOwnership() != TransferOwnership.NONE)
            upcall.addStatement("if (_result instanceof $T _gobject) _gobject.ref()",
                    ClassNames.G_OBJECT);

        // Marshal return value
        if (!returnsVoid) {
            var stmt = new TypedValueGenerator(returnValue)
                    .marshalJavaToNative("_result");
            upcall.addNamedCode("return " + stmt.format() + ";\n",
                    stmt.arguments());
        }

        // Catch exceptions and set the GError** value
        if (closure.throws_()) {
            if (methodToInvoke.endsWith("invoke")) {
                upcall.nextControlFlow("catch ($T _ite)", InvocationTargetException.class)
                      .beginControlFlow("if (_ite.getCause() instanceof $T _ge)", ClassNames.GERROR_EXCEPTION);
            } else {
                upcall.nextControlFlow("catch ($T _ge)", ClassNames.GERROR_EXCEPTION);
            }
            upcall.addStatement("$1T _gerror = $1T.literal(_ge.getDomain(), _ge.getCode(), _ge.getMessage())", ClassNames.G_ERROR)
                  .addStatement("$T.yieldOwnership(_gerror)", ClassNames.MEMORY_CLEANER)
                  .addStatement("_gerrorPointer.set($T.ADDRESS, 0, _gerror.handle())", ValueLayout.class);
            if (!returnsVoid)
                returnNull(upcall);
            if (methodToInvoke.endsWith("invoke")) {
                upcall.nextControlFlow("else")
                      .addStatement("throw _ite")
                      .endControlFlow();
            }
            upcall.endControlFlow();
        }

        // Catch exceptions from reflection calls
        if (methodToInvoke.endsWith("invoke")) {
            upcall.nextControlFlow("catch ($T _ite)",
                    InvocationTargetException.class);
            upcall.addStatement("$T.log($T.LOG_DOMAIN, $T.LEVEL_WARNING, _ite.getCause() + $S)",
                    ClassNames.G_LIB,
                    ClassNames.CONSTANTS,
                    ClassNames.G_LOG_LEVEL_FLAGS,
                    " in " + methodName);
            if (!returnsVoid)
                returnNull(upcall);
        }

        // Catch other exceptions
        upcall.nextControlFlow("catch ($T _t)", Throwable.class);
        upcall.addStatement("$T.log($T.LOG_DOMAIN, $T.LEVEL_WARNING, _t + $S)",
                ClassNames.G_LIB,
                ClassNames.CONSTANTS,
                ClassNames.G_LOG_LEVEL_FLAGS,
                " in " + methodName);
        if (!returnsVoid)
            returnNull(upcall);
        upcall.endControlFlow();

        return upcall.build();
    }

    private boolean isHiddenParameter(Parameter p) {
        return p.isUserDataParameter()
                || p.isDestroyNotifyParameter()
                || p.isArrayLengthParameter();
    }

    private PartialStatement marshalParameters(String methodToInvoke) {
        PartialStatement stmt = new PartialStatement();

        if (closure.parameters() == null)
            return stmt;

        List<Parameter> parameters = closure.parameters().parameters();
        boolean first = true;

        // Determine the index of the last (non-hidden) parameter
        int last = parameters.size() - 1;
        while (last > 0 && isHiddenParameter(parameters.get(last)))
            last--;

        // Generate the parameters
        for (int i = 0; i <= last; i++) {
            Parameter p = parameters.get(i);

            if (isHiddenParameter(p))
                continue;

            if (!first)
                stmt.add(", ");
            first = false;

            if (p.anyType() instanceof Type t
                    && t.isPointer()
                    && t.lookup() instanceof Alias a
                    && a.isValueWrapper()) {
                stmt.add("_" + toJavaIdentifier(p.name()) + "Alias");
                continue;
            }

            if (p.isOutParameter()) {
                stmt.add("_" + toJavaIdentifier(p.name()) + "Out");
                continue;
            }

            // Invoking a method using reflection calls Method.invoke() which is
            // variadic. If the last parameter is an array, that will trigger a
            // compiler warning, because it is unsure if the array should be
            // treated as varargs or not.
            if (i == last && methodToInvoke.endsWith("invoke"))
                if (p.anyType() instanceof Array
                        || (p.anyType() instanceof Type t && t.isActuallyAnArray()))
                    stmt.add("($object:T) ", "object", Object.class);

            // This is unsupported; it generates a "null". Suppress the warning
            // when it's the trailing parameter.
            if (i == last && p.varargs())
                stmt.add("($object:T) ", "object", Object.class);

            stmt.add(new TypedValueGenerator(p)
                .marshalNativeToJava(toJavaIdentifier(p.name()), true));
        }
        return stmt;
    }

    private void returnNull(MethodSpec.Builder upcall) {
        if (returnValue.anyType() instanceof Type type) {
            var target = type.lookup();
            if ((type.isPrimitive()
                        || target instanceof EnumType
                        || (target instanceof Alias a && a.isValueWrapper()))
                    && (!type.isPointer())) {
                upcall.addStatement("return 0");
                return;
            }
        }
        upcall.addStatement("return $T.NULL", MemorySegment.class);
    }

    MethodSpec generateToCallbackMethod(String className) {
        return MethodSpec.methodBuilder("toCallback")
                .addJavadoc("""
                        Creates a native function pointer to the {@link #upcall} method.
                        
                        @param arena the arena in which the function pointer is allocated
                        @return the native function pointer
                        """)
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(Arena.class, "arena")
                .returns(MemorySegment.class)
                .addCode(generator.generateFunctionDescriptorDeclaration())
                .addStatement("$T _handle = $T.upcallHandle($T.lookup(), $L.class, _fdesc)",
                        MethodHandle.class, ClassNames.INTEROP, MethodHandles.class, className)
                .addStatement("return $T.nativeLinker().upcallStub(_handle.bindTo(this), _fdesc, arena)",
                        Linker.class)
                .build();
    }
}
