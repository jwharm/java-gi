package io.github.jwharm.javagi;

import org.gtk.gobject.GObject;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryAddress;

/**
 * The Marshal interface defines a marshal operation from a Java value from or to a native value.
 * @param <In> The type of the input value
 * @param <Out> The type of the output value
 */
@FunctionalInterface
public interface Marshal<In, Out> {

    /**
     * Marshal the provided input value. If {@code ownership} is not null, and the {@code Out}
     * type is a {@link Proxy}, it can be used to set the ownership of the proxy object.
     * @param input Value to marshal
     * @param ownership Ownership to set on a {@link Proxy} object
     * @return the marshalled object
     */
    Out marshal(In input, Ownership ownership);

    /**
     * A no-op marshal function that returns the input
     */
    Marshal<GObject, GObject> passthrough              = (input, ownership) -> input;

    /**
     * Booleans in GLib are typedefs for integers with value 1 or 0. This marshal function
     * converts such a value to a Java boolean.
     */
    Marshal<Integer, Boolean> integerToBoolean         = (input, ownership) -> input == 1;

    /**
     * Booleans in GLib are typedefs for integers with value 1 or 0. This marshal function
     * converts a Java boolean to such a value.
     */
    Marshal<Boolean, Integer> booleanToInteger         = (input, ownership) -> input ? 1 : 0;

    /**
     * Marshal function that reads a String from the provided address
     */
    Marshal<MemoryAddress, String> addressToString     = (input, ownership) -> Interop.getStringFrom(input);

    /**
     * Marshal function that writes a String to the provided address
     */
    Marshal<String, Addressable> stringToAddress       = (input, ownership) -> Interop.allocateNativeString(input);

    /**
     * Marshal function to write an Enumeration to native memory
     */
    Marshal<Enumeration, Integer> enumerationToInteger = (input, ownership) -> input.getValue();

    /**
     * Marshal function to write a Bitfield to native memory
     */
    Marshal<Bitfield, Integer> bitfieldToInteger       = (input, ownership) -> input.getValue();

    /**
     * Marshal function to write the value of a primitive alias to native memory
     */
    Marshal aliasToPrimitive                           = (input, ownership) -> ((Alias<?>) input).getValue();

    /**
     * Marshal function to write a callback to native memory.
     * Currently not implemented, always returns {@link MemoryAddress#NULL}.
     */
    Marshal callbackToAddress                          = (input, ownership) -> MemoryAddress.NULL; // TODO: Marshaller for callback parameters
}
