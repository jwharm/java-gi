package io.github.jwharm.javagi.util;

import io.github.jwharm.javagi.base.GErrorException;
import org.gtk.gio.Cancellable;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * An {@link java.lang.AutoCloseable} interface for GIO streams.
 * <p>
 * This interface extends {@link java.lang.AutoCloseable} and implements the {@link #close()}
 * with a {@code default} version that calls the {@link #close(Cancellable)} method. This method
 * is implemented by GIO streams ({@link org.gtk.gio.IOStream}, {@link org.gtk.gio.InputStream}
 * and {@link org.gtk.gio.OutputStream}) so they become {@code AutoCloseable} and can be used in
 * a try-with-resources block.
 */
public interface AutoCloseable extends java.lang.AutoCloseable {

    /**
     * A default implementation of {@link java.lang.AutoCloseable} that calls {@link #close(Cancellable)}.
     * The return value of {@link #close(Cancellable)} is ignored.
     * @throws java.io.IOException An {@code IOException} that is wrapped around the {@link GErrorException}
     *                             that is thrown by {@link #close(Cancellable)}.
     */
    default void close() throws java.io.IOException {
        try {
            close(null);
        } catch (GErrorException gerror) {
            throw new IOException(gerror);
        }
    }

    /**
     * The {@code close} method that is implemented by GLib streams ({@link org.gtk.gio.IOStream},
     * {@link org.gtk.gio.InputStream} and {@link org.gtk.gio.OutputStream}).
     * @param cancellable optional {@link Cancellable} object, {@code null} to ignore
     * @return {@code true} on success, {@code false} on failure
     * @throws GErrorException See {@link org.gtk.glib.Error}
     */
    boolean close(@Nullable org.gtk.gio.Cancellable cancellable) throws GErrorException;
}