package io.github.jwharm.javagi.util;

import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.BooleanSupplier;

import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.gobject.Closure;
import org.gnome.gobject.ClosureMarshal;
import org.gnome.gobject.Value;

import static io.github.jwharm.javagi.Constants.LOG_DOMAIN;

/**
 * An implementation of {@link Closure} that can be used with Java callbacks.
 * In most cases, the callback will be invoked using reflection. For two common 
 * cases (Runnable and BooleanSupplier), the callback will be invoked directly.
 */
public class JavaClosure extends Closure {
    
    /**
     * Construct a {@link Closure} for a method or lambda that takes no parameters and returns void.
     * @param callback a callback with signature {@code void run()}
     */
    public JavaClosure(Runnable callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint, marshalData) -> callback.run());
    }
    
    /**
     * Construct a {@link Closure} for a method or lambda that takes no parameters and returns boolean.
     * @param callback a callback with signature {@code boolean run()}
     */
    public JavaClosure(BooleanSupplier callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint, marshalData) -> returnValue.setBoolean(callback.getAsBoolean()));
    }

    /**
     * Construct a {@link Closure} that will invoke the provided Java lambda using reflection.
     * The Closure function arguments are read from the argument-{@link Value} containers and passed
     * to the lambda. The return value of the lambda is put in the Closure return-{@link Value}.
     * @param lambda                    a lambda (instance of a functional interface)
     * @throws IllegalArgumentException if the lambda is not an instance of a functional interface
     */
    public JavaClosure(Object lambda) throws IllegalArgumentException {
        this(lambda, getSingleMethod(lambda.getClass()));
    }

    /**
     * Get the single abstract method (SAM) implementation of a class that implements a functional interface.
     * A functional interface is an interface with exactly one abstract method.
     * @param functionalInterfaceClass a functional interface
     * @return the Method reference to the method that implements the SAM
     * @throws IllegalArgumentException if {@code functionalInterfaceClass} is not a functional interface
     */
    private static Method getSingleMethod(Class<?> functionalInterfaceClass) throws IllegalArgumentException {
        // Check if the class is not an enum or array
        if (functionalInterfaceClass.isEnum() || functionalInterfaceClass.isArray()) {
            throw new IllegalArgumentException(functionalInterfaceClass + " is not a functional interface");
        }

        // Loop through all declared methods
        Method samMethod = null;
        for (Method method : functionalInterfaceClass.getDeclaredMethods()) {
            // Check if the method is not static
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            // If there is more than one SAM, return null (ambiguous)
            if (samMethod != null) {
                throw new IllegalArgumentException(functionalInterfaceClass + " is not a functional interface: more than one method found.");
            }
            samMethod = method;
        }

        // Check that a SAM exists
        if (samMethod == null) {
            throw new IllegalArgumentException(functionalInterfaceClass + " is not a functional interface: method not found.");
        }
        return samMethod;
    }

    /**
     * Construct a {@link Closure} that will invoke the provided Java method using reflection.
     * The Closure function arguments are read from the argument-{@link Value} containers and passed
     * to the method. The return value of the method is put in the Closure return-{@link Value}.
     * @param instance a class instance on which the provided method will be invoked. When the
     *                 method is static, this parameter is ignored and and may be {@code null}.
     * @param method   the method to invoke. See {@link Method#invoke(Object, Object...)}
     */
    public JavaClosure(Object instance, Method method) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal(new ClosureMarshal() {
            public void run(Closure closure, Value returnValue, Value[] paramValues, MemorySegment invocationHint, MemorySegment marshalData) {
                try {
                    Object[] parameterObjects;
                    if (paramValues == null || paramValues.length == 0) {
                        parameterObjects = new Object[0];
                    } else {
                        // Convert the parameter Values into Java Objects
                        parameterObjects = new Object[paramValues.length - 1];
                        for (int v = 1; v < paramValues.length; v++) {
                            parameterObjects[v - 1] = ValueUtil.valueToObject(paramValues[v]);
                        }
                    }
                    // Invoke the method
                    method.setAccessible(true);
                    Object result = method.invoke(instance, parameterObjects);

                    // Convert the returned Object to a GValue
                    ValueUtil.objectToValue(result, returnValue);

                } catch (Exception e) {
                    GLib.log(
                            LOG_DOMAIN,
                            LogLevelFlags.LEVEL_CRITICAL,
                            "JavaClosure: Cannot invoke method %s in class %s: %s\n",
                            method == null ? "null" : method.getName(),
                            instance == null ? "null" : instance.getClass().getName(),
                            e.toString()
                    );
                }
            }
        });
    }
}
