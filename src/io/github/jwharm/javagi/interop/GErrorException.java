package io.github.jwharm.javagi.interop;

import jdk.incubator.foreign.MemorySegment;
import io.github.jwharm.javagi.interop.jextract.GError;

public class GErrorException extends Exception {

    public GErrorException(MemorySegment gerror) {
        super(GError.message$get(gerror).getUtf8String(0));
    }
}
