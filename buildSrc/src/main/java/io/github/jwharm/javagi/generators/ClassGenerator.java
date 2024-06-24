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
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.GeneratedAnnotationBuilder;

import javax.lang.model.element.Modifier;

import java.lang.foreign.MemorySegment;

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

        Class parentClass = cls.parentClass();
        if (parentClass != null)
            builder.superclass(parentClass.typeName());
        else
            builder.superclass(ClassNames.TYPE_INSTANCE);

        for (var impl : cls.implements_())
            builder.addSuperinterface(impl.get().typeName());

        if (cls.autoCloseable())
            builder.addSuperinterface(ClassNames.AUTO_CLOSEABLE);

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
                    .addMethod(gobjectGetProperty())
                    .addMethod(gobjectSetProperty())
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
         * named unref() or free(), use that one. If not, the MemoryCleaner
         * will fallback to g_free().
         */
        if (! cls.isInstanceOf("GObject", "Object")) {
            for (var m : cls.methods()) {
                if (("free".equals(m.name()) || "unref".equals(m.name()))
                        && m.parameters().instanceParameter() != null
                        && m.parameters().parameters().isEmpty()
                        && (m.returnValue().anyType().isVoid())) {
                    spec.addStatement("$T.setFreeFunc(this, $S)",
                            ClassNames.MEMORY_CLEANER,
                            m.callableAttrs().cIdentifier());
                    break;
                }
            }
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
                .returns(ClassNames.GTYPE)
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
                .addTypeVariable(TypeVariableName.get("T", ClassNames.GOBJECT))
                .returns(TypeVariableName.get("T"))
                .addParameter(ClassNames.GTYPE, "objectType")
                .addStatement("var _result = constructNew(objectType, null)")
                .addStatement("T _object = (T) $T.getForType(_result, $T::new, true)",
                        ClassNames.INSTANCE_CACHE, ClassNames.GOBJECT)
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
                .addTypeVariable(TypeVariableName.get("T", ClassNames.GOBJECT))
                .returns(TypeVariableName.get("T"))
                .addParameter(ClassNames.GTYPE, "objectType")
                .addParameter(Object[].class, "propertyNamesAndValues")
                .varargs(true)
                .addStatement("return $T.newGObjectWithProperties(objectType, propertyNamesAndValues)",
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
                        ClassNames.GOBJECTS)
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
}
