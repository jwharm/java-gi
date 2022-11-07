package io.github.jwharm.javagi;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.invoke.VarHandle;

import org.jetbrains.annotations.ApiStatus;

public class GErrorException extends Exception {

    private static final long serialVersionUID = -5219056090883059292L;
    
    private final int code, domain;

    private static MemorySegment dereference(MemorySegment pointer) {
        return _GError.ofAddress(pointer.get(Interop.valueLayout.ADDRESS, 0), Interop.getScope());
    }

    private static String getMessage(MemorySegment pointer) {
        return _GError.message$get(dereference(pointer)).getUtf8String(0);
    }

    private int getCode(MemorySegment pointer) {
        return _GError.code$get(dereference(pointer));
    }

    private int getDomain(MemorySegment pointer) {
        return _GError.domain$get(dereference(pointer));
    }

    /**
     * Create a GErrorException from a GError memory segment that was
     * returned by a native function.
     */
    @ApiStatus.Internal
    public GErrorException(MemorySegment gerrorPtr) {
        super(getMessage(gerrorPtr));
        this.code = getCode(gerrorPtr);
        this.domain = getDomain(gerrorPtr);
    }

    /**
     * Create a GErrorException that can be used to return a GError from a Java callback function to
     * native code. See <a href="https://docs.gtk.org/glib/error-reporting.html">the Gtk documentation
     * on error reporting</a> for details.
     * @param domain The GError error domain
     * @param code The GError error code
     * @param message The error message
     */
    public GErrorException(int domain, int code, String message) {
        super(message);
        this.code = code;
        this.domain = domain;
    }

    /**
     * @return true when an error was set on this pointer
     */
    public static boolean isErrorSet(MemorySegment gerrorPtr) {
        MemoryAddress gerror = gerrorPtr.get(Interop.valueLayout.ADDRESS, 0);
        return (! gerror.equals(MemoryAddress.NULL));
    }

    /**
     * @return the code of the GError
     */
    public int getCode() {
        return code;
    }

    /**
     * @return The domain of the GError
     */
    public int getDomain() {
        return domain;
    }
    
    /**
     * Based on jextract-generated source file, generated from glib.h
     */
    private class _GError {

        static final GroupLayout $struct$LAYOUT = 
                MemoryLayout.structLayout(
                        Interop.valueLayout.C_INT.withName("domain"),
                        Interop.valueLayout.C_INT.withName("code"), 
                        Interop.valueLayout.ADDRESS.withName("message")
                ).withName("_GError");

        private static final VarHandle domain$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("domain"));

        public static int domain$get(MemorySegment seg) {
            return (int) _GError.domain$VH.get(seg);
        }

        private static final VarHandle code$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("code"));

        public static int code$get(MemorySegment seg) {
            return (int) _GError.code$VH.get(seg);
        }

        private static final VarHandle message$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("message"));

        public static MemoryAddress message$get(MemorySegment seg) {
            return (MemoryAddress) _GError.message$VH.get(seg);
        }

        public static MemorySegment ofAddress(MemoryAddress addr, MemorySession scope) {
            return MemorySegment.ofAddress(addr, _GError.$struct$LAYOUT.byteSize(), scope);
        }
    }
}
