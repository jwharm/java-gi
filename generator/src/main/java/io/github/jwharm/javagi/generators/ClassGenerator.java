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
import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.gir.Interface;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.GeneratedAnnotationBuilder;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;

import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public class ClassGenerator extends RegisteredTypeGenerator {

    private final Class cls;
    private final TypeSpec.Builder builder;

    public ClassGenerator(Class cls) {
        super(cls);
        this.cls = cls;
        this.builder = TypeSpec.classBuilder(cls.typeName());
        this.builder.addAnnotation(GeneratedAnnotationBuilder.generate());
    }

    public TypeSpec generate() {
        if (cls.infoElements().doc() != null)
            builder.addJavadoc(
                    new DocGenerator(cls.infoElements().doc()).generate());
        if (cls.infoAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        builder.addModifiers(Modifier.PUBLIC);
        if (cls.abstract_()) builder.addModifiers(Modifier.ABSTRACT);
        if (cls.final_()) builder.addModifiers(Modifier.FINAL);
        if (cls.generic()) builder.addTypeVariable(ClassNames.GENERIC_T);
        TypeName actualGeneric = cls.actualGeneric();

        Class parentClass = cls.parentClass();
        if (parentClass != null)
            builder.superclass(parentClass.generic()
                    ? ParameterizedTypeName.get(parentClass.typeName(),
                    actualGeneric)
                    : parentClass.typeName());
        else
            builder.superclass(ClassNames.G_TYPE_INSTANCE);

        // Add "implements" clause for all implemented interfaces.
        // For generic interfaces, add "<GObject>" generic type.
        for (var impl : cls.implements_())
            if (impl.lookup() instanceof Interface iface)
                builder.addSuperinterface(iface.generic()
                        ? ParameterizedTypeName.get(iface.typeName(),
                        actualGeneric)
                        : iface.typeName());

        if (cls.autoCloseable())
            builder.addSuperinterface(ClassNames.AUTO_CLOSEABLE);

        if (cls.mutableList()) {
            builder.addSuperinterface(ParameterizedTypeName.get(ClassNames.LIST_MODEL_JAVA_LIST_MUTABLE, actualGeneric));
            if (actualGeneric.equals(ClassNames.STRING_OBJECT))
                builder.addMethod(appendStringObjectUnwrapper());
        }

        if (cls.spliceableList()) {
            if (actualGeneric instanceof TypeVariableName) throw new IllegalArgumentException("actualGeneric is a TypeVariableName");
            builder.addSuperinterface(ParameterizedTypeName.get(ClassNames.LIST_MODEL_JAVA_LIST_SPLICEABLE, actualGeneric));
            builder.addMethod(spliceCollectionWrapper(actualGeneric));
            if (actualGeneric.equals(ClassNames.STRING_OBJECT)) {
                builder.addMethod(spliceStringObjectUnwrapper());
                builder.addMethod(appendStringObjectUnwrapper());
            }
        }

        if (cls.isFloating())
            builder.addSuperinterface(ClassNames.FLOATING);

        builder.addStaticBlock(staticBlock());

        for (var constant : cls.constants()) {
            var fieldSpec = new TypedValueGenerator(constant)
                    .generateConstantDeclaration();
            if (fieldSpec != null) builder.addField(fieldSpec);
        }

        if (hasTypeMethod())
            builder.addMethod(getTypeMethod());
        else if (cls.isInstanceOf("GObject", "ParamSpec"))
            builder.addMethod(paramSpecGetTypeMethod());

        MethodSpec memoryLayout = new MemoryLayoutGenerator()
                .generateMemoryLayout(cls);
        if (memoryLayout != null)
            builder.addMethod(memoryLayout);

        builder.addMethod(parentAccessor());
        builder.addMethod(memoryAddressConstructor());

        if (cls.toStringTarget() != null)
            builder.addMethod(toStringRedirect(cls.toStringTarget()));

        addConstructors(builder);
        addFunctions(builder);
        addMethods(builder);
        addVirtualMethods(builder);
        addSignals(builder);

        Record typeStruct = cls.typeStruct();
        if (typeStruct != null)
            builder.addType(new RecordGenerator(typeStruct).generate());

        if (cls.abstract_())
            builder.addType(implClass());

        if (cls.isInstanceOf("GObject", "Object")) {
            BuilderGenerator generator = new BuilderGenerator(cls);
            builder.addMethod(generator.generateBuilderMethod());
            builder.addType(generator.generateBuilderClass());
        }

        if ("GObject".equals(cls.cType()))
            builder.addMethod(gobjectConstructor())
                    .addMethod(gobjectConstructorVarargs())
                    .addMethod(gobjectClassConstructor())
                    .addMethod(gobjectClassConstructorVarargs())
                    .addMethod(gobjectGetProperty())
                    .addMethod(gobjectSetProperty())
                    .addMethod(gobjectBindProperty())
                    .addMethod(gobjectConnect())
                    .addMethod(gobjectConnectAfter())
                    .addMethod(gobjectEmit());

        if (hasDowncallHandles())
            builder.addType(downcallHandlesClass());

        return builder.build();
    }

    private MethodSpec parentAccessor() {
        ClassName className = cls.abstract_()
                ? cls.typeName().nestedClass(cls.name() + "Impl")
                : cls.typeName();

        return MethodSpec.methodBuilder("asParent")
                .addJavadoc("""
                    Returns this instance as if it were its parent type. This is mostly
                    synonymous to the Java {@code super} keyword, but will set the native
                    typeclass function pointers to the parent type. When overriding a native
                    virtual method in Java, "chaining up" with {@code super.methodName()}
                    doesn't work, because it invokes the overridden function pointer again.
                    To chain up, call {@code asParent().methodName()}. This will call the
                    native function pointer of this virtual method in the typeclass of the
                    parent type.
                    """)
                .addModifiers(Modifier.PROTECTED)
                .returns(cls.typeName())
                .addStatement("$T _parent = new $T(handle())", cls.typeName(), className)
                .addStatement("_parent.callParent(true)")
                .addStatement("return _parent")
                .build();
    }

    @Override
    protected MethodSpec memoryAddressConstructor() {
        var spec = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("""
                    Create a $L proxy instance for the provided memory address.
                    
                    @param address the memory address of the native object
                    """, name())
                .addParameter(MemorySegment.class, "address")
                .addStatement("super($T.reinterpret(address, getMemoryLayout().byteSize()))",
                        ClassNames.INTEROP);

        /*
         * Register a free-function for this class. If the class has a method
         * named unref(), use that one. If not, the MemoryCleaner will fallback
         * to g_free().
         */
        if (! cls.isInstanceOf("GObject", "Object")) {
            var m = cls.unrefFunc();
            if (m != null)
                spec.addStatement("$T.setFreeFunc(this, $S)",
                        ClassNames.MEMORY_CLEANER,
                        m.callableAttrs().cIdentifier());
        }
        return spec.build();
    }

    protected MethodSpec paramSpecGetTypeMethod() {
        return MethodSpec.methodBuilder("getType")
                .addJavadoc("""
                    Get the GType of the $L class
                    
                    @return always {@link $T#PARAM}
                    """, cls.cType(), ClassNames.TYPES)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassNames.G_TYPE)
                .addStatement("return $T.PARAM", ClassNames.TYPES)
                .build();
    }

    private MethodSpec gobjectConstructor() {
        return MethodSpec.methodBuilder("newInstance")
                .addJavadoc("""
                    Creates a new GObject instance of the provided GType.
                    
                    @param objectType the GType of the new GObject
                    @return the newly created GObject instance
                    """)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariable(ClassNames.GENERIC_T)
                .returns(TypeVariableName.get("T"))
                .addParameter(ClassNames.G_TYPE, "objectType")
                .addStatement("var _result = constructNew(objectType, null)")
                .addStatement("T _object = (T) $T.getForType(_result, $T::new, true)",
                        ClassNames.INSTANCE_CACHE, ClassNames.G_OBJECT)
                .addStatement("return _object")
                .build();
    }

    private MethodSpec gobjectConstructorVarargs() {
        return MethodSpec.methodBuilder("newInstance")
                .addJavadoc("""
                    Creates a new GObject instance of the provided GType and with the
                    provided property values.
                    
                    @param  objectType the GType of the new GObject
                    @param  propertyNamesAndValues pairs of property names and values
                            (Strings and Objects)
                    @return the newly created GObject instance
                    @throws IllegalArgumentException invalid property name
                    """)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariable(ClassNames.GENERIC_T)
                .returns(TypeVariableName.get("T"))
                .addParameter(ClassNames.G_TYPE, "objectType")
                .addParameter(Object[].class, "propertyNamesAndValues")
                .varargs(true)
                .addStatement("return $T.newGObjectWithProperties(objectType, propertyNamesAndValues)",
                        ClassNames.PROPERTIES)
                .build();
    }

    private MethodSpec gobjectClassConstructor() {
        var paramType = ParameterizedTypeName.get(
                ClassName.get(java.lang.Class.class), TypeVariableName.get("T"));

        return MethodSpec.methodBuilder("newInstance")
                .addJavadoc("""
                    Creates a new instance of a GObject-derived class. For your own
                    GObject-derived Java classes, a GType must have been registered using
                    {@link io.github.jwharm.javagi.gobject.types.Types#register(Class<?>)}
                    
                    @param  objectClass the Java class of the new GObject
                    @return the newly created GObject instance
                    """)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariable(ClassNames.GENERIC_T)
                .returns(TypeVariableName.get("T"))
                .addParameter(paramType, "objectClass")
                .addStatement("var _type = $T.getType(objectClass)",
                        ClassNames.TYPE_CACHE)
                .addStatement("var _result = constructNew(_type, null)")
                .addStatement("T _object = (T) $T.getForType(_result, $T::new, true)",
                        ClassNames.INSTANCE_CACHE, ClassNames.G_OBJECT)
                .addStatement("return _object")
                .build();
    }

    private MethodSpec gobjectClassConstructorVarargs() {
        var paramType = ParameterizedTypeName.get(
                ClassName.get(java.lang.Class.class), TypeVariableName.get("T"));

        return MethodSpec.methodBuilder("newInstance")
                .addJavadoc("""
                    Creates a new instance of a GObject-derived class with the provided
                    property values. For your own GObject-derived Java classes, a GType
                    must have been registered using
                    {@link io.github.jwharm.javagi.gobject.types.Types#register(Class<?>)}
                    
                    @param  objectClass the Java class of the new GObject
                    @param  propertyNamesAndValues pairs of property names and values
                            (Strings and Objects)
                    @return the newly created GObject instance
                    @throws IllegalArgumentException invalid property name
                    """)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariable(ClassNames.GENERIC_T)
                .returns(TypeVariableName.get("T"))
                .addParameter(paramType, "objectClass")
                .addParameter(Object[].class, "propertyNamesAndValues")
                .varargs(true)
                .addStatement("var _type = $T.getType(objectClass)",
                        ClassNames.TYPE_CACHE)
                .addStatement("return $T.newGObjectWithProperties(_type, propertyNamesAndValues)",
                        ClassNames.PROPERTIES)
                .build();
    }

    private MethodSpec gobjectGetProperty() {
        return MethodSpec.methodBuilder("getProperty")
                .addJavadoc("""
                    Get a property of an object.
                    
                    @param  propertyName the name of the property to get
                    @return the property value
                    @throws IllegalArgumentException invalid property name
                    """)
                .addModifiers(Modifier.PUBLIC)
                .returns(Object.class)
                .addParameter(String.class, "propertyName")
                .addStatement("return $T.getProperty(this, propertyName)",
                        ClassNames.PROPERTIES)
                .build();
    }

    private MethodSpec gobjectSetProperty() {
        return MethodSpec.methodBuilder("setProperty")
                .addJavadoc("""
                    Set a property of an object.
                    
                    @param  propertyName the name of the property to set
                    @param  value the new property value
                    @throws IllegalArgumentException invalid property name
                    """)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "propertyName")
                .addParameter(Object.class, "value")
                .addStatement("$T.setProperty(this, propertyName, value)",
                        ClassNames.PROPERTIES)
                .build();
    }

    private MethodSpec gobjectBindProperty() {
        var S = TypeVariableName.get("S");
        var T = TypeVariableName.get("T");
        return MethodSpec.methodBuilder("bindProperty")
                .addJavadoc("""
                    Creates a binding between {@code sourceProperty} on this Object and
                    {@code targetProperty} on {@code target}.
                    <p>
                    Whenever the {@code sourceProperty} is changed the {@code targetProperty}
                    is updated using the same value. For instance:
                    <pre>{@code   action.bindProperty ("active", widget, "sensitive").build();
                    }</pre>
                    <p>
                    Will result in the "sensitive" property of the widget {@code GObject}
                    instance to be updated with the same value of the "active" property of
                    the action {@code GObject} instance.
                    <p>
                    If {@link BindingBuilder#bidirectional()} is set then the binding will
                    be mutual: if {@code targetProperty} on {@code target} changes then the
                    {@code sourceProperty} on this Object will be updated as well.
                    <p>
                    The binding will automatically be removed when either the this Object
                    or the {@code target} instances are finalized. To remove the binding
                    without affecting the this Object and the {@code target} you can just
                    call {@code unref()} on the returned {@code Binding} instance.
                    <p>
                    Removing the binding by calling {@code unref()} on it must only be done
                    if the binding, this GObject and {@code target} are only used from a
                    single thread and it is clear that both this GObject and {@code target}
                    outlive the binding. Especially it is not safe to rely on this if the
                    binding, this GObject or {@code target} can be finalized from different
                    threads. Keep another reference to the binding and use
                    {@link Binding#unbind()} instead to be on the safe side.
                    <p>
                    A {@code GObject} can have multiple bindings.
                    
                    @param  sourceProperty the property on this Object to bind
                    @param  target         the target {@code GObject}
                    @param  targetProperty the property on {@code target} to bind
                    @return the {@code GBinding} instance representing the binding between
                            the two {@code GObject} instances. The binding is released
                            whenever the {@code GBinding} reference count reaches zero.
                    """)
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(S)
                .addTypeVariable(T)
                .returns(ParameterizedTypeName.get(ClassNames.BINDING_BUILDER, S, T))
                .addParameter(String.class, "sourceProperty")
                .addParameter(ClassNames.G_OBJECT, "target")
                .addParameter(String.class, "targetProperty")
                .addStatement("return new $T<S, T>(this, sourceProperty, target, targetProperty)",
                        ClassNames.BINDING_BUILDER)
                .build();
    }

    private MethodSpec gobjectConnect() {
        return MethodSpec.methodBuilder("connect")
                .addJavadoc("""
                    Connect a callback to a signal for this object. The handler will be
                    called before the default handler of the signal.
                    
                    @param  detailedSignal a string of the form "signal-name::detail"
                    @param  callback       the callback to connect
                    @return a SignalConnection object to track, block and disconnect the
                            signal connection
                    """)
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(TypeVariableName.get("T"))
                .returns(ClassNames.SIGNAL_CONNECTION)
                .addParameter(String.class, "detailedSignal")
                .addParameter(TypeVariableName.get("T"), "callback")
                .addStatement("return connect(detailedSignal, callback, false)")
                .build();
    }

    private MethodSpec gobjectConnectAfter() {
        return MethodSpec.methodBuilder("connect")
                .addJavadoc("""
                    Connect a callback to a signal for this object.
                    
                    @param detailedSignal a string of the form "signal-name::detail"
                    @param callback       the callback to connect
                    @param after          whether the handler should be called before or
                                          after the default handler of the signal
                    @return a SignalConnection object to track, block and disconnect the
                            signal connection
                    """)
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(TypeVariableName.get("T"))
                .returns(ClassNames.SIGNAL_CONNECTION)
                .addParameter(String.class, "detailedSignal")
                .addParameter(TypeVariableName.get("T"), "callback")
                .addParameter(boolean.class, "after")
                .addStatement("$1T closure = new $1T(callback)",
                        ClassNames.JAVA_CLOSURE)
                .addStatement("int handlerId = $T.signalConnectClosure(this, detailedSignal, closure, after)",
                        ClassNames.G_OBJECTS)
                .addStatement("return new $T(handle(), handlerId, closure)",
                        ClassNames.SIGNAL_CONNECTION)
                .build();
    }

    private MethodSpec gobjectEmit() {
        return MethodSpec.methodBuilder("emit")
                .addJavadoc("""
                    Emits a signal from this object.
                    
                    @param  detailedSignal a string of the form "signal-name::detail"
                    @param  params         the parameters to emit for this signal
                    @return the return value of the signal, or {@code null} if the signal
                            has no return value
                    @throws IllegalArgumentException if a signal with this name is not found
                            for the object
                    """)
                .addModifiers(Modifier.PUBLIC)
                .returns(Object.class)
                .addParameter(String.class, "detailedSignal")
                .addParameter(Object[].class, "params")
                .varargs(true)
                .addStatement("return $T.emit(this, detailedSignal, params)",
                        ClassNames.SIGNALS)
                .build();
    }

    private MethodSpec spliceCollectionWrapper(TypeName actualGeneric) {
        return MethodSpec.methodBuilder("splice")
                .addJavadoc("""
                        Modifies this list by removing {@code nRemovals} elements starting at
                        {@code index} and replacing them with the elements in {@code additions}.
                        
                        @param index the index at which to splice the list
                        @param nRemovals the number of elements to remove
                        @param elements the elements to insert at the index
                        @throws IndexOutOfBoundsException if the index is out of range
                        """)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(int.class, "index")
                .addParameter(int.class, "nRemovals")
                .addParameter(ParameterizedTypeName.get(
                        ClassName.get(Collection.class),
                        WildcardTypeName.subtypeOf(actualGeneric)
                ).annotated(AnnotationSpec.builder(NotNull.class).build()), "additions")
                .addStatement("splice(index, nRemovals, additions.toArray($T[]::new))", actualGeneric)
                .build();
    }

    private MethodSpec spliceStringObjectUnwrapper() {
        return MethodSpec.methodBuilder("splice")
                .addJavadoc("""
                        Modifies this list by removing {@code nRemovals} elements starting at
                        {@code index} and replacing them with the elements in {@code additions}.
                        
                        @param index the index at which to splice the list
                        @param nRemovals the number of elements to remove
                        @param additions the elements to insert at the index
                        @throws IndexOutOfBoundsException if the index is out of range
                        """)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(int.class, "index")
                .addParameter(int.class, "nRemovals")
                .addParameter(ArrayTypeName.of(ClassNames.STRING_OBJECT), "additions")
                .addStatement("splice(index, nRemovals, additions == null ? null : $T.stream(additions).map($T::getString).toArray(String[]::new))", Arrays.class, ClassNames.STRING_OBJECT)
                .build();
    }

    private MethodSpec appendStringObjectUnwrapper() {
            return MethodSpec.methodBuilder("append")
                .addJavadoc("""
                        Adds the specified element to the end of this list.
                        
                        @param e element to be appended to this list
                        """)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassNames.STRING_OBJECT, "e")
                .addStatement("append(e.getString())")
                .build();
    }

    private MethodSpec toStringRedirect(String target) {
        return MethodSpec.methodBuilder("toString")
                .addJavadoc("""
                        Returns a string representation of the object.
                        
                        @return a string representation of the object
                        """)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $T.toString($L())", Objects.class, target)
                .build();
    }
}
