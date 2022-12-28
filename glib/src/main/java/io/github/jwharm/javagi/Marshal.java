package io.github.jwharm.javagi;

import org.gtk.gobject.GObject;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryAddress;

@FunctionalInterface
public interface Marshal<In, Out> {

    Out marshal(In input, Ownership ownership);

    Marshal<GObject, GObject> passthrough             = (input, ownership) -> input;

    Marshal<Integer, Boolean> integerToBoolean        = (input, ownership) -> input == 1;
    Marshal<Boolean, Integer> booleanToInteger        = (input, ownership) -> input ? 1 : 0;

    Marshal<MemoryAddress, String> addressToString         = (input, ownership) -> Interop.getStringFrom(input);
    Marshal<String, Addressable> stringToAddress         = (input, ownership) -> Interop.allocateNativeString(input);

    Marshal<Enumeration, Integer> enumerationToInteger    = (input, ownership) -> input.getValue();
    Marshal<Bitfield, Integer> bitfieldToInteger       = (input, ownership) -> input.getValue();
    Marshal aliasToPrimitive        = (input, ownership) -> ((Alias<?>) input).getValue();

    Marshal callbackToAddress       = (input, ownership) -> MemoryAddress.NULL; // TODO: Marshaller for callback parameters
}
