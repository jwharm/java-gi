package io.github.jwharm.javagi.util;

import io.github.jwharm.javagi.base.Bitfield;
import io.github.jwharm.javagi.base.Enumeration;
import org.gtk.glib.Variant;
import org.gtk.gobject.*;

import java.lang.foreign.MemoryAddress;
import java.util.function.Function;

/**
 * An implementation of {@link Closure} that configures a marshaller for most common Closure function signatures.
 */
public class JavaClosure extends Closure {

    @FunctionalInterface
    public interface Bool__Boxed_Boxed {
        boolean run(MemoryAddress boxed1, MemoryAddress boxed2);
    }

    /**
     * Construct a Closure that takes two MemoryAddress parameters and returns boolean.
     * @param callback a callback with signature {@code boolean run(MemoryAddress boxed1, MemoryAddress boxed2)}
     */
    public JavaClosure(Bool__Boxed_Boxed callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> {
            var result = callback.run(args[1].getBoxed(), args[2].getBoxed());
            if (returnValue != null) returnValue.setBoolean(result);
        });
    }

    @FunctionalInterface
    public interface Bool__Flags {
        boolean run(Bitfield flags);
    }

    /**
     * Construct a Closure that takes a Bitfield parameter and returns boolean.
     * @param callback         a callback with signature {@code boolean run(Bitfield flags)}
     * @param flagsConstructor the constructor of the Bitfield subclass
     */
    public JavaClosure(Bool__Flags callback, Function<Integer, ? extends Bitfield> flagsConstructor) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> {
            var result = callback.run(flagsConstructor.apply(args[1].getFlags()));
            if (returnValue != null) returnValue.setBoolean(result);
        });
    }

    @FunctionalInterface
    public interface Bool__Void {
        boolean run();
    }

    /**
     * Construct a Closure that takes no parameters and returns boolean.
     * @param callback a callback with signature {@code boolean run()}
     */
    public JavaClosure(Bool__Void callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> returnValue.setBoolean(callback.run()));
    }

    @FunctionalInterface
    public interface String__GObject_Pointer {
        String run(GObject gobject, MemoryAddress pointer);
    }

    /**
     * Construct a Closure that takes a GObject and a MemoryAddress parameter and returns a String.
     * @param callback a callback with signature {@code String run(GObject gobject, MemoryAddress pointer)}
     */
    public JavaClosure(String__GObject_Pointer callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> {
            var result = callback.run(args[1].getObject(), args[2].getPointer());
            if (returnValue != null) returnValue.setString(result);
        });
    }

    @FunctionalInterface
    public interface Void__Bool {
        void run(boolean arg);
    }

    /**
     * Construct a Closure that takes a boolean parameter and returns void.
     * @param callback a callback with signature {@code void run(boolean arg)}
     */
    public JavaClosure(Void__Bool callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> callback.run(args[1].getBoolean()));
    }

    @FunctionalInterface
    public interface Void__Boxed {
        void run(MemoryAddress arg);
    }

    /**
     * Construct a Closure that takes a MemoryAddress parameter and returns void.
     * @param callback a callback with signature {@code void run(MemoryAddress arg)}
     */
    public JavaClosure(Void__Boxed callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> callback.run(args[1].getBoxed()));
    }

    @FunctionalInterface
    public interface Void__Char {
        void run(byte arg);
    }

    /**
     * Construct a Closure that takes a byte parameter and returns void.
     * The byte parameter value is passed from native code in a Value with type G_TYPE_CHAR, and
     * is read from the Value argument with {@link Value#getSchar()}.
     * @param callback a callback with signature {@code void run(byte arg)}
     */
    public JavaClosure(Void__Char callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> callback.run(args[1].getSchar()));
    }

    @FunctionalInterface
    public interface Void__Double {
        void run(double arg);
    }

    /**
     * Construct a Closure that takes a double parameter and returns void.
     * @param callback a callback with signature {@code void run(double arg)}
     */
    public JavaClosure(Void__Double callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> callback.run(args[1].getDouble()));
    }

    @FunctionalInterface
    public interface Void__Enum {
        void run(Enumeration arg);
    }

    /**
     * Construct a Closure that takes an Enumeration parameter and returns void.
     * @param callback         a callback with signature {@code void run(Enumeration flags)}
     * @param enumConstructor the constructor of the Enumeration subclass
     */
    public JavaClosure(Void__Enum callback, Function<Integer, ? extends Enumeration> enumConstructor) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> callback.run(enumConstructor.apply(args[1].getEnum())));
    }

    @FunctionalInterface
    public interface Void__Flags {
        void run(Bitfield flags);
    }

    /**
     * Construct a Closure that takes a Bitfield parameter and returns void.
     * @param callback         a callback with signature {@code void run(Bitfield flags)}
     * @param flagsConstructor the constructor of the Bitfield subclass
     */
    public JavaClosure(Void__Flags callback, Function<Integer, ? extends Bitfield> flagsConstructor) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> callback.run(flagsConstructor.apply(args[1].getFlags())));
    }

    @FunctionalInterface
    public interface Void__Float {
        void run(float arg);
    }

    /**
     * Construct a Closure that takes a float parameter and returns void.
     * @param callback a callback with signature {@code void run(float arg)}
     */
    public JavaClosure(Void__Float callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> callback.run(args[1].getFloat()));
    }

    @FunctionalInterface
    public interface Void__Int {
        void run(int arg);
    }

    /**
     * Construct a Closure that takes an int parameter and returns void.
     * @param callback a callback with signature {@code void run(int arg)}
     */
    public JavaClosure(Void__Int callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> callback.run(args[1].getInt()));
    }

    @FunctionalInterface
    public interface Void__Long {
        void run(long arg);
    }

    /**
     * Construct a Closure that takes a long parameter and returns void.
     * @param callback a callback with signature {@code void run(long arg)}
     */
    public JavaClosure(Void__Long callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> callback.run(args[1].getLong()));
    }

    @FunctionalInterface
    public interface Void__GObject {
        void run(GObject arg);
    }

    /**
     * Construct a Closure that takes a {@link GObject} parameter and returns void.
     * @param callback a callback with signature {@code void run(GObject arg)}
     */
    public JavaClosure(Void__GObject callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> callback.run(args[1].getObject()));
    }

    @FunctionalInterface
    public interface Void__Param {
        void run(ParamSpec arg);
    }

    /**
     * Construct a Closure that takes a {@link ParamSpec} parameter and returns void.
     * @param callback a callback with signature {@code void run(ParamSpec arg)}
     */
    public JavaClosure(Void__Param callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> callback.run(args[1].getParam()));
    }

    @FunctionalInterface
    public interface Void__String {
        void run(String arg);
    }

    /**
     * Construct a Closure that takes a String parameter and returns void.
     * @param callback a callback with signature {@code void run(String arg)}
     */
    public JavaClosure(Void__String callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> callback.run(args[1].getString()));
    }

    @FunctionalInterface
    public interface Void__Variant {
        void run(Variant arg);
    }

    /**
     * Construct a Closure that takes a {@link Variant} parameter and returns void.
     * @param callback a callback with signature {@code void run(Variant arg)}
     */
    public JavaClosure(Void__Variant callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> callback.run(args[1].getVariant()));
    }

    @FunctionalInterface
    public interface Void__Void {
        void run();
    }

    /**
     * Construct a Closure that takes no parameters and returns void.
     * @param callback a callback with signature {@code void run()}
     */
    public JavaClosure(Void__Void callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> callback.run());
    }

    @FunctionalInterface
    public interface Generic {
        Value run(Value[] args);
    }

    /**
     * Construct a Closure with un-marshalled {@link Value} parameters.
     * The function arguments are in a {@code Value[]} array where element index 0 contains
     * an instance reference, and index 1 and up contain the actual function arguments.
     * The return value is wrapped in a {@link Value} object too.
     * @param callback a callback function with signature {@code Value run(Value[] args)}
     */
    public JavaClosure(Generic callback) {
        super(Closure.newSimple((int) Closure.getMemoryLayout().byteSize(), null).handle());
        setMarshal((closure, returnValue, args, hint) -> callback.run(args).copy(returnValue));
    }

}
