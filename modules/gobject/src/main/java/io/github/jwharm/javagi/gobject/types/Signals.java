/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 Jan-Willem Harmannij
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

package io.github.jwharm.javagi.gobject.types;

import io.github.jwharm.javagi.base.Out;
import io.github.jwharm.javagi.base.Proxy;
import io.github.jwharm.javagi.base.ProxyInstance;
import io.github.jwharm.javagi.gobject.ValueUtil;
import io.github.jwharm.javagi.gobject.annotations.Signal;
import io.github.jwharm.javagi.interop.Interop;
import org.gnome.glib.Quark;
import org.gnome.glib.Type;
import org.gnome.gobject.*;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Helper class to register signals in a new GType.
 */
public class Signals {

    /*
     * Ensure that the "gobject-2.0" library has been loaded. This is
     * required for the downcall handles.
     */
    static {
        GObjects.javagi$ensureInitialized();
    }

    /**
     * The method handle for g_signal_connect_data is used by all generated
     * signal-connection methods.
     */
    public static final MethodHandle g_signal_connect_data =
            Interop.downcallHandle(
                "g_signal_connect_data",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_INT),
                false);

    /**
     * The method handle for g_signal_emit_by_name is used by all generated
     * signal-emission methods.
     */
    public static final MethodHandle g_signal_emit_by_name =
            Interop.downcallHandle(
                "g_signal_emit_by_name",
                FunctionDescriptor.ofVoid(
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                true);

    private record SignalDeclaration(String signalName,
                                     Set<SignalFlags> signalFlags,
                                     Type returnType,
                                     int nParams,
                                     Type[] paramTypes) {}

    /*
     * Convert "CamelCase" to "kebab-case"
     */
    private static String getSignalName(String className) {
        return className.replaceAll("([a-z0-9])([A-Z])", "$1-$2")
                .toLowerCase().replaceAll("\\.", "");
    }
    
    /*
     * Convert the annotation parameters to SignalFlags
     */
    private static Set<SignalFlags> getFlags(Signal signal) {
        Set<SignalFlags> flags = EnumSet.noneOf(SignalFlags.class);
        if (signal.action())      flags.add(SignalFlags.ACTION);
        if (signal.deprecated())  flags.add(SignalFlags.DEPRECATED);
        if (signal.detailed())    flags.add(SignalFlags.DETAILED);
        if (signal.mustCollect()) flags.add(SignalFlags.MUST_COLLECT);
        if (signal.noHooks())     flags.add(SignalFlags.NO_HOOKS);
        if (signal.noRecurse())   flags.add(SignalFlags.NO_RECURSE);
        if (signal.runCleanup())  flags.add(SignalFlags.RUN_CLEANUP);
        if (signal.runFirst())    flags.add(SignalFlags.RUN_FIRST);
        if (signal.runLast())     flags.add(SignalFlags.RUN_LAST);
        return flags;
    }
    
    /*
     * Infer the GType from the Java class.
     */
    private static Type inferType(Class<?> cls)
            throws IllegalArgumentException {

        if (cls.equals(void.class) || cls.equals(Void.class))
            return Types.NONE;

        else if (cls.equals(boolean.class) || cls.equals(Boolean.class))
            return Types.BOOLEAN;

        else if (cls.equals(byte.class) || cls.equals(Byte.class))
            return Types.CHAR;

        else if (cls.equals(char.class) || cls.equals(Character.class))
            return Types.CHAR;

        else if (cls.equals(double.class) || cls.equals(Double.class))
            return Types.DOUBLE;

        else if (cls.equals(float.class) || cls.equals(Float.class))
            return Types.FLOAT;

        else if (cls.equals(int.class) || cls.equals(Integer.class))
            return Types.INT;

        else if (cls.equals(long.class) || cls.equals(Long.class))
            return Types.LONG;

        else if (cls.equals(String.class))
            return Types.STRING;

        else if (GObject.class.isAssignableFrom(cls))
            // GObject class
            return TypeCache.getType(cls);

        else if (ProxyInstance.class.isAssignableFrom(cls))
            // Struct
            return Types.BOXED;

        else if (Proxy.class.isAssignableFrom(cls))
            // GObject interface
            return TypeCache.getType(cls);

        throw new IllegalArgumentException("Cannot infer gtype for class "
                + cls.getName()
                + " used as a parameter or return-type of a signal declaration\n");
    }

    /**
     * If the provided class contains inner interface declarations with a
     * {@code @Signal}-annotation, this function will return a class
     * initializer that registers these declarations as GObject signals (using
     * {@code g_signal_newv}) in the class initializer.
     *
     * @param cls  the class that possibly contains @Signal annotations
     * @return a class initializer that registers the signals
     */
    public static Consumer<TypeClass> installSignals(Class<?> cls) {

        List<SignalDeclaration> signalDeclarations = new ArrayList<>();
        
        for (var iface : cls.getDeclaredClasses()) {

            // Look for functional interface declarations...
            if (! iface.isInterface())
                continue;

            // ... that are annotated with @Signal
            if (! iface.isAnnotationPresent(Signal.class))
                continue;

            Signal signalAnnotation = iface.getDeclaredAnnotation(Signal.class);
            
            // get the Single Abstract Method of the functional interface
            Method sam = getSingleAbstractMethod(iface);
            
            // signal name
            String signalName = signalAnnotation.name().isBlank()
                    ? getSignalName(iface.getSimpleName())
                    : signalAnnotation.name();
            
            // flags
            Set<SignalFlags> signalFlags = getFlags(signalAnnotation);
            
            // return type
            Type returnType = inferType(sam.getReturnType());
            
            // parameter count
            int nParams = sam.getParameterCount();
            
            // parameter types
            Type[] paramTypes = new Type[nParams];
            Class<?>[] paramClasses = sam.getParameterTypes();
            for (int p = 0; p < nParams; p++) {
                paramTypes[p] = inferType(paramClasses[p]);
            }

            // Add the signal to the list
            signalDeclarations.add(new SignalDeclaration(
                    signalName, signalFlags, returnType, nParams, paramTypes));
        }

        // Don't generate unnecessary class initializer methods.
        if (signalDeclarations.isEmpty())
            return null;

        // Return class initializer method that installs the signals.
        return (gclass) -> {
            for (var sig : signalDeclarations) {
                GObjects.signalNewv(
                        sig.signalName,
                        gclass.readGType(),
                        sig.signalFlags,
                        null,
                        null,
                        null,
                        sig.returnType,
                        sig.paramTypes);
            }
        };
    }
    
    /**
     * Emits a signal from a GObject.
     *
     * @param  gobject        the object that emits the signal
     * @param  detailedSignal a string of the form "signal-name::detail"
     * @param  params         the parameters to emit for this signal
     * @return the return value of the signal, or {@code null} if the signal
     *         has no return value
     * @throws IllegalArgumentException if a signal with this name is not found
     *                                  for the object
     */
    public static Object emit(GObject gobject,
                              String detailedSignal,
                              Object... params) {
        Type gtype = TypeCache.getType(gobject.getClass());

        // Parse the detailed signal name into a signal id and detail quark
        Out<Integer> signalId = new Out<>();
        Quark detailQ = new Quark(0);
        boolean success = GObjects.signalParseName(
                detailedSignal, gtype, signalId, detailQ, false);

        if (! success)
            throw new IllegalArgumentException("Invalid signal \"%s\" for class %s"
                    .formatted(detailedSignal, gobject));

        try (var arena = Arena.ofConfined()) {
            // Query the parameter details of the signal
            SignalQuery query = new SignalQuery(arena);
            GObjects.signalQuery(signalId.get(), query);

            // Create an array of Types for the parameters
            int nParams = query.readNParams();
            Type[] paramTypes = query.readParamTypes();

            // Allocate Values array for the instance parameter and other
            // parameters
            var values = new Value[nParams+1];

            // Allocation return value
            var returnValue = new Value(arena);
            Type returnType = query.readReturnType();
            if (! Types.NONE.equals(returnType))
                returnValue.init(returnType);

            // Set instance parameter
            values[0] = new Value(arena).init(gtype);
            values[0].setObject(gobject);

            // Set other parameters
            for (int i = 0; i < nParams; i++) {
                values[i+1] = new Value(arena).init(paramTypes[i]);
                ValueUtil.objectToValue(params[i], values[i+1]);
            }

            // Emit the signal
            GObjects.signalEmitv(values, signalId.get(), detailQ, returnValue);

            // Return the result (if any)
            Object result = Types.NONE.equals(returnType)
                    ? null
                    : ValueUtil.valueToObject(returnValue);

            // Cleanup the allocated values
            for (Value value : values) {
                if (value != null)
                    value.unset();
            }
            returnValue.unset();

            return result;
        }
    }

    /**
     * Get the single abstract method (SAM) implementation of a class that
     * implements a functional interface. A functional interface is an
     * interface with exactly one abstract method.
     *
     * @param  cls a functional interface
     * @return the Method reference to the method that implements the SAM
     * @throws IllegalArgumentException if {@code cls} is not a functional
     *                                  interface
     */
    public static Method getSingleAbstractMethod(Class<?> cls)
            throws IllegalArgumentException {

        // Check if the class is not an enum or array
        if ((! cls.isInterface()) || cls.isEnum() || cls.isArray()) {
            throw new IllegalArgumentException("%s is not a functional interface"
                    .formatted(cls));
        }

        // Loop through all declared methods
        Method samMethod = null;
        for (Method method : cls.getMethods()) {

            // Check if the method is not static
            if (Modifier.isStatic(method.getModifiers()))
                continue;

            // Check if the method is abstract
            if (! Modifier.isAbstract(method.getModifiers()))
                continue;

            // If there is more than one SAM, return null (ambiguous)
            if (samMethod != null)
                throw new IllegalArgumentException("%s is not a functional interface: more than one abstract method found."
                        .formatted(cls));

            samMethod = method;
        }

        // Check that a SAM exists
        if (samMethod == null)
            throw new IllegalArgumentException("%s is not a functional interface: abstract method not found."
                    .formatted(cls));

        return samMethod;
    }
}
