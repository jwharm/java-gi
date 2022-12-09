package io.github.jwharm.javagi;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;

import org.jetbrains.annotations.ApiStatus;

import org.gtk.glib.Quark;

/**
 * A GErrorException is thrown when a GError is returned by native code.
 * See <a href="https://docs.gtk.org/glib/error-reporting.html">the Gtk documentation on 
 * error reporting</a> for details about GError.
 */
public class GErrorException extends Exception {

    // Auto-generated
    private static final long serialVersionUID = -5219056090883059292L;
    
    // The GError proxy object
    private final org.gtk.glib.Error gerror;

    // Dereference the GError instance from the pointer
    private static org.gtk.glib.Error dereference(MemorySegment pointer) {
        return (org.gtk.glib.Error) org.gtk.glib.Error.fromAddress.marshal(pointer.get(Interop.valueLayout.ADDRESS, 0), Ownership.NONE);
    }
    
    // Get the message from the GError instance (used by the GErrorException constructor)
    private static String getMessage(MemorySegment pointer) {
        return dereference(pointer).message$get();
    }

    /**
     * Create a GErrorException from a GError memory segment that was
     * returned by a native function.
     */
    @ApiStatus.Internal
    public GErrorException(MemorySegment gerrorPtr) {
        super(getMessage(gerrorPtr));
        this.gerror = dereference(gerrorPtr);
    }

    /**
     * Create a GErrorException that can be used to return a GError from a Java callback function to
     * native code. See <a href="https://docs.gtk.org/glib/error-reporting.html">the Gtk documentation
     * on error reporting</a> for details.
     * @param domain The GError error domain
     * @param code The GError error code
     * @param message The error message, printf-style formatted
     * @param args varargs parameters for message format
     */
    public GErrorException(Quark domain, int code, String message, java.lang.Object... args) {
        super(message);
        this.gerror = new org.gtk.glib.Error(domain, code, message, args);
    }

    /**
     * Check if an error is set.
     * @return true when an error was set on this pointer
     */
    public static boolean isErrorSet(MemorySegment gerrorPtr) {
        MemoryAddress gerror = gerrorPtr.get(Interop.valueLayout.ADDRESS, 0);
        return (! gerror.equals(MemoryAddress.NULL));
    }

    /**
     * Get the error code
     * @return the code of the GError
     */
    public int getCode() {
        return gerror.code$get();
    }

    /**
     * Get the error domain
     * @return The domain of the GError
     */
    public Quark getDomain() {
        return gerror.domain$get();
    }
    
    /**
     * Get the GError instance
     * @return the GError instance
     */
    public org.gtk.glib.Error getGError() {
        return gerror;
    }
}
