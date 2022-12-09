package io.github.jwharm.javagi;

import java.lang.foreign.MemoryAddress;

@FunctionalInterface
public interface Marshal {

    Object marshal(Object input, Ownership ownership);

    Marshal passthrough             = (input, ownership) -> input;

    Marshal integerToBoolean        = (input, ownership) -> ((Integer) input) == 1;
    Marshal booleanToInteger        = (input, ownership) -> ((Boolean) input) ? 1 : 0;

    Marshal addressToString         = (input, ownership) -> Interop.getStringFrom((MemoryAddress) input);
    Marshal stringToAddress         = (input, ownership) -> Interop.allocateNativeString((String) input);

    Marshal enumerationToInteger    = (input, ownership) -> ((Enumeration) input).getValue();
    Marshal bitfieldToInteger       = (input, ownership) -> ((Bitfield) input).getValue();
    Marshal aliasToPrimitive        = (input, ownership) -> ((Alias<?>) input).getValue();

    Marshal callbackToAddress       = (input, ownership) -> MemoryAddress.NULL; // TODO: Marshaller for callback parameters
}
