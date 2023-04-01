package io.github.jwharm.javagi.util;

import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.gobject.Closure;
import org.gnome.gobject.ClosureMarshal;
import org.gnome.gobject.Value;

/**
 * An implementation of {@link Closure} that can be used with Java methods.
 */
public class JavaClosure extends Closure {
    
    private static final String LOG_DOMAIN = "java-gi";

    /**
     * Construct a {@link Closure} for a method or lambda that takes no parameters and returns void.
     * @param callback a callback with signature {@code void run()}
     */
    public JavaClosure(Runnable callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> callback.run());
    }
    
    /**
     * Construct a {@link Closure} for a method or lambda that takes no parameters and returns boolean.
     * @param callback a callback with signature {@code boolean run()}
     */
    public JavaClosure(BooleanSupplier callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> returnValue.setBoolean(callback.getAsBoolean()));
    }

    @FunctionalInterface
    public interface UnmarshalledClosureCallback {
        Value run(Value[] args);
    }

    /**
     * Construct a {@link Closure} with un-marshalled {@link Value} parameters.
     * The function arguments are in a {@code Value[]} array where element index 0 contains
     * an instance reference, and index 1 and up contain the actual function arguments.
     * The return value is wrapped in a {@link Value} object too.
     * @param callback a callback function with signature {@code Value run(Value[] args)}
     */
    public JavaClosure(UnmarshalledClosureCallback callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> callback.run(args).copy(returnValue));
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
            public void run(Closure closure, Value returnValue, Value[] paramValues, MemorySegment invocationHint) {
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
                    Object result = method.invoke(instance, parameterObjects);

                    // Convert the returned Object to a GValue
                    ValueUtil.objectToValue(result, returnValue);

                } catch (Exception e) {
                    GLib.log(
                            LOG_DOMAIN,
                            LogLevelFlags.LEVEL_CRITICAL,
                            "JavaClosure: Cannot invoke method %s in class %s: %s\n",
                            method.getName(),
                            instance.getClass().getName(),
                            e.toString()
                    );
                }
            }
        });
    }
}
