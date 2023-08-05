package io.github.jwharm.javagi.types;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.gnome.glib.Quark;
import org.gnome.glib.Type;
import org.gnome.gobject.GObject;
import org.gnome.gobject.GObjects;
import org.gnome.gobject.SignalFlags;
import org.gnome.gobject.Value;

import io.github.jwharm.javagi.annotations.Signal;
import io.github.jwharm.javagi.base.Out;
import io.github.jwharm.javagi.base.Proxy;
import io.github.jwharm.javagi.base.ProxyInstance;
import io.github.jwharm.javagi.util.JavaClosure;
import io.github.jwharm.javagi.util.ValueUtil;

/**
 * Helper class to register signals in a new GType
 */
public class Signals {

    private record SignalDeclaration(String signalName, Type itype, SignalFlags signalFlags, Type returnType, int nParams, Type[] paramTypes) {}

    // Convert "CamelCase" to "kebab-case"
    private static String getSignalName(String className) {
        return className.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase().replaceAll("\\.", "");
    }
    
    // Convert the annotation parameters to SignalFlags
    private static SignalFlags getFlags(Signal signal) {
        SignalFlags flags = new SignalFlags(0);
        if (signal.action()) flags = flags.or(SignalFlags.ACTION);
        if (signal.deprecated()) flags = flags.or(SignalFlags.DEPRECATED);
        if (signal.detailed()) flags = flags.or(SignalFlags.DETAILED);
        if (signal.mustCollect()) flags = flags.or(SignalFlags.MUST_COLLECT);
        if (signal.noHooks()) flags = flags.or(SignalFlags.NO_HOOKS);
        if (signal.noRecurse()) flags = flags.or(SignalFlags.NO_RECURSE);
        if (signal.runCleanup()) flags = flags.or(SignalFlags.RUN_CLEANUP);
        if (signal.runFirst()) flags = flags.or(SignalFlags.RUN_FIRST);
        if (signal.runLast()) flags = flags.or(SignalFlags.RUN_LAST);
        return flags;
    }
    
    // Infer the GType from the Java class.
    private static Type inferType(Class<?> cls) throws IllegalArgumentException {
        if (cls.equals(void.class) || cls.equals(Void.class)) {
            return Types.NONE;
        } else if (cls.equals(boolean.class) || cls.equals(Boolean.class)) {
            return Types.BOOLEAN;
        } else if (cls.equals(byte.class) || cls.equals(Byte.class)) {
            return Types.CHAR;
        } else if (cls.equals(char.class) || cls.equals(Character.class)) {
            return Types.CHAR;
        } else if (cls.equals(double.class) || cls.equals(Double.class)) {
            return Types.DOUBLE;
        } else if (cls.equals(float.class) || cls.equals(Float.class)) {
            return Types.FLOAT;
        } else if (cls.equals(int.class) || cls.equals(Integer.class)) {
            return Types.INT;
        } else if (cls.equals(long.class) || cls.equals(Long.class)) {
            return Types.LONG;
        } else if (cls.equals(String.class)) {
            return Types.STRING;
        } else if (GObject.class.isAssignableFrom(cls)) {
            // GObject class
            return Types.getGType(cls);
        } else if (ProxyInstance.class.isAssignableFrom(cls)) {
            // Struct
            return Types.BOXED;
        } else if (Proxy.class.isAssignableFrom(cls)) {
            // GObject interface
            return Types.getGType(cls);
        } else {
            throw new IllegalArgumentException("Cannot infer gtype for class " + cls.getName()
                    + " used as a parameter or return-type of a signal declaration\n");
        }
    }

    /**
     * If the provided class contains inner interface declarations with a @Signal-annotation, 
     * this function will return a class initializer that registers these declarations as 
     * GObject signals (using {@code g_signal_newv}) in the class initializer.
     * @param cls the class that possibly contains @Signal annotations
     * @return a class initalizer that registers the signals
     * @param <T> the class must extend {@link org.gnome.gobject.GObject}
     * @param <TC> the returned lambda expects a {@link GObject.ObjectClass} parameter
     */
    public static <T extends GObject, TC extends GObject.ObjectClass> Consumer<TC> installSignals(Class<T> cls) {
        List<SignalDeclaration> signalDeclarations = new ArrayList<>();
        
        for (var iface : cls.getDeclaredClasses()) {
            // Look for functional interface declarations...
            if (! iface.isInterface()) {
                continue;
            }
            // ... that are annotated with @SignalConnection
            if (! iface.isAnnotationPresent(Signal.class)) {
                continue;
            }
            Signal signalAnnotation = iface.getDeclaredAnnotation(Signal.class);
            
            // get the Single Abstract Method of the functional interface
            Method sam = JavaClosure.getSingleMethod(iface);
            
            // signal name
            String signalName = signalAnnotation.name().isBlank() ? getSignalName(iface.getSimpleName()) : signalAnnotation.name();
            
            // GType of the class that declares the signal
            Type itype = Types.getGType(cls);
            
            // flags
            SignalFlags signalFlags = getFlags(signalAnnotation);
            
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
            signalDeclarations.add(new SignalDeclaration(signalName, itype, signalFlags, returnType, nParams, paramTypes));
        }
        
        // Return class initializer method that installs the signals.
        return (gclass) -> {
            for (var sig : signalDeclarations) {
                GObjects.signalNewv(sig.signalName, sig.itype, sig.signalFlags, null, null, null, sig.returnType, sig.paramTypes);
            }
        };
    }
    
    /**
     * Emits a signal from a GObject.
     * @param gobject the object that emits the signal
     * @param detailedSignal a string of the form "signal-name::detail"
     * @param params the parameters to emit for this signal
     * @return the return value of the signal, or {@code null} if the signal has no return value
     */
    public static Object emit(GObject gobject, String detailedSignal, Object... params) {
        Value[] values = new Value[params.length + 1];
        
        Type gtype = Types.getGType(gobject.getClass());
        
        // Set instance parameter
        values[0] = Value.allocate().init(gtype);
        values[0].setObject(gobject);

        // Set other parameters
        for (int i = 0; i < params.length; i++) {
            ValueUtil.objectToValue(params[i], values[i+1]);
        }

        // Allocation return value
        Value returnValue = Value.allocate();

        // Parse the detailed signal name into a signal id and detail quark
        Out<Integer> signalId = new Out<>();
        Quark detailQ = new Quark(0);
        GObjects.signalParseName(detailedSignal, gtype, signalId, detailQ, false);

        // Emit the signal
        GObjects.signalEmitv(values, signalId.get(), detailQ, returnValue);

        // Return the result (if any)
        return ValueUtil.valueToObject(returnValue);
    }
}
