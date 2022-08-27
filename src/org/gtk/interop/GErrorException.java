package org.gtk.interop;

import jdk.incubator.foreign.MemorySegment;
import org.gtk.interop.jextract.GError;

public class GErrorException extends Exception {

    public GErrorException(MemorySegment gerror) {
        super(GError.message$get(gerror).getUtf8String(0));
    }
}
