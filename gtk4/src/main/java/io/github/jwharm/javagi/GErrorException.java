package io.github.jwharm.javagi;

import java.lang.foreign.MemorySegment;

public class GErrorException extends Exception {

    private final int code, domain;

    private static MemorySegment dereference(MemorySegment pointer) {
        return null; // GError.ofAddress(pointer.get(C_POINTER, 0), Interop.getScope());
    }

    private static String getMessage(MemorySegment pointer) {
        return ""; // GError.message$get(dereference(pointer)).getUtf8String(0);
    }

    private int getCode(MemorySegment pointer) {
        return 0; // GError.code$get(dereference(pointer));
    }

    private int getDomain(MemorySegment pointer) {
        return 0; // GError.domain$get(dereference(pointer));
    }

    private void freeMemory(MemorySegment pointer) {
        // io.github.jwharm.javagi.interop.jextract.gtk_h.g_error_free(dereference(pointer));
    }

    /**
     * Create a GErrorException from a GError memory segment that was
     * returned by a native function.
     */
    public GErrorException(MemorySegment gerrorPtr) {
        super(getMessage(gerrorPtr));
        this.code = getCode(gerrorPtr);
        this.domain = getDomain(gerrorPtr);
        freeMemory(gerrorPtr);
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
        return false; // MemoryAddress gerror = gerrorPtr.get(C_POINTER, 0);
        // return (! gerror.equals(MemoryAddress.NULL));
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
}
