package io.github.jwharm.javagi.base;

import java.io.Serial;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;

import io.github.jwharm.javagi.interop.Interop;
import org.jetbrains.annotations.ApiStatus;

import org.gtk.glib.Quark;

/**
 * A GErrorException is thrown when a GError is returned by native code.
 * See <a href="https://docs.gtk.org/glib/error-reporting.html">the Gtk documentation on 
 * error reporting</a> for details about GError.
 */
public class GErrorException extends Exception {

    // Auto-generated
    @Serial
    private static final long serialVersionUID = -5219056090883059292L;

    private final Quark domain;
    private final int code;
    private final String message;

    // Dereference the GError instance from the pointer
    private static org.gtk.glib.Error dereference(MemorySegment pointer) {
        return (org.gtk.glib.Error) org.gtk.glib.Error.fromAddress.marshal(pointer.get(Interop.valueLayout.ADDRESS, 0), null);
    }
    
    // Get the message from the GError instance (used by the GErrorException constructor)
    private static String readMessage(MemorySegment pointer) {
        return dereference(pointer).readMessage();
    }

    /**
     * Create a GErrorException from a GError memory segment that was
     * returned by a native function.
     * @param gerrorPtr Pointer to a GError in native memory
     */
    @ApiStatus.Internal
    public GErrorException(MemorySegment gerrorPtr) {
        super(readMessage(gerrorPtr));

        org.gtk.glib.Error gerror = dereference(gerrorPtr);
        this.domain = gerror.readDomain();
        this.code = gerror.readCode();
        this.message = gerror.readMessage();
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
        this.domain = domain;
        this.code = code;
        this.message = message;
    }

    /**
     * Check if an error is set.
     * @param gerrorPtr pointer to a GError in native memory
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
        return code;
    }

    /**
     * Get the error domain
     * @return The domain of the GError
     */
    public Quark getDomain() {
        return domain;
    }
    
    /**
     * Get a new GError instance with the domain, code and message of this GErrorException
     * @return a new GError instance
     */
    public org.gtk.glib.Error getGError() {
        return new org.gtk.glib.Error(domain, code, message);
    }
}
