/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2026 Jan-Willem Harmannij
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package org.javagi.base;

import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.jspecify.annotations.Nullable;

///
/// Handles exceptions thrown by Java callback methods.
///
/// When a Java callback method that is invoked from native code throws an
/// exception, the JVM will immediately crash. Java-GI therefore catches these
/// exceptions.
///
/// A global handler that will be invoked for these exceptions can be set with
/// [#setUncaughtExceptionHandler]. The handler is not bound to a specific
/// thread; it will be run for all exceptions from Java callbacks invoked from
/// native code.
///
/// When no handler was set, the exception is instead stored in a `ThreadLocal`
/// field and will later be thrown (wrapped in a
/// [CallbackInvocationException]), immediately after a native method call in
/// the same thread has completed.
///
/// Exception handling can optionally be disabled by setting the environment
/// variable `java-gi.discard-callback-exceptions` to `"true"` (ignoring case).
///
/// When the environment variable `java-gi.log-callback-exceptions` is set to
/// `"true"` (ignoring case), Java-GI will log the exception on `stderr` (using
/// `g_log()` with level `WARNING`).
///
/// For performance reasons, the values of the two environment variables are
/// cached, so the log/rethrow behavior cannot be changed during runtime.
///
public class ExceptionHandler {
    private static final boolean DISCARD_EXCEPTIONS =
            Boolean.getBoolean("java-gi.discard-callback-exceptions");

    private static final boolean LOG_EXCEPTIONS =
            Boolean.getBoolean("java-gi.log-callback-exceptions");

    private static final ThreadLocal<@Nullable Throwable> PENDING_EXCEPTION = new ThreadLocal<>();
    private static @Nullable UncaughtExceptionHandler HANDLER = null;

    ///
    /// Interface for handlers invoked when a runtime exception is thrown from
    /// a Java callback called from native code.
    ///
    @FunctionalInterface
    public interface UncaughtExceptionHandler {
        ///
        /// Method invoked for runtime exceptions thrown from Java callbacks.
        /// Any exceptions thrown by this handler will be ignored.
        ///
        /// @param throwable the exception
        /// @param source the name of the callback where the exception occurred
        ///
        void uncaughtException(Throwable throwable, String source);
    }

    ///
    /// Get the handler that was previously set with
    /// [#setUncaughtExceptionHandler]. When no handler was set, this will
    /// return `null`.
    ///
    /// @return the handler, or `null`
    ///
    public static ExceptionHandler.@Nullable UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return HANDLER;
    }

    ///
    /// Set a handler to be invoked when a runtime exception occurs from Java
    /// callbacks called from native code. This will replace the normal
    /// handling by Java-GI, where the exception is temporarily stored and
    /// later rethrown after a native function call has completed. Pass a
    /// `null` to remove a previously set handler.
    ///
    /// @param handler the exception handler. If `null`, any previously set
    ///                handler will be unset.
    ///
    public static void setUncaughtExceptionHandler(ExceptionHandler.@Nullable UncaughtExceptionHandler handler) {
        HANDLER = handler;
    }

    ///
    /// Handles exceptions that were thrown in a Java callback method that was
    /// invoked from native code.
    ///
    /// When the environment variable `java-gi.log-callback-exceptions` is
    /// `"true"` (ignoring casse), the exception will be logged to `stderr`.
    ///
    /// When the environment variable `java-gi.discard-callback-exceptions`
    /// is `"true"` (ignoring case), the exception is discarded.
    ///
    /// If a handler has been set with [#setUncaughtExceptionHandler] the
    /// handler is invoked to handle the exception. Otherwise, the exception
    /// is stored in a ThreadLocal field to be handled later.
    ///
    /// @param throwable the exception to handle
    /// @param source    the location where the exception occured
    ///
    public static void handleException(Throwable throwable, String source) {
        if (LOG_EXCEPTIONS)
            GLib.log(Constants.LOG_DOMAIN, LogLevelFlags.LEVEL_WARNING, throwable + " in " + source);

        if (DISCARD_EXCEPTIONS)
            return;

        Throwable suppressed = PENDING_EXCEPTION.get();
        if (suppressed != null) {
            throwable.addSuppressed(suppressed);
            PENDING_EXCEPTION.remove();
        }

        if (HANDLER != null) {
            // Run user-installed exception handler.
            try {
                HANDLER.uncaughtException(throwable, source);
            } catch (Throwable _) {
                // Any exception thrown by the handler will be ignored.
            }
        } else {
            // Store the exception so it can be rethrown later.
            PENDING_EXCEPTION.set(throwable);
        }
    }

    ///
    /// When propagation of exceptions is not disabled with the envrionment
    /// variable `java-gi.discard-callback-exceptions`, and an exception was
    /// stored with [#handleException], the exception is wrapped in a
    /// [CallbackInvocationException] and thrown.
    ///
    /// **Note:** The **cause** of the exception points to the actual location
    /// where the exception occured, while the outer
    /// CallbackInvocationException is thrown by the first method that
    /// completed in the same thread where the original exception occured. For
    /// example, when a GLib [idle callback][GLib#idleAdd] throws an exception,
    /// it will be rethrown from a completely unrelated callsite that just
    /// happened to be scheduled after the idle callback on the GLib main loop.
    ///
    /// @throws CallbackInvocationException wraps the exception that occured in
    ///         a Java callback method.
    ///
    public static void propagateExceptions() throws CallbackInvocationException {
        if (DISCARD_EXCEPTIONS)
            return;

        Throwable throwable = PENDING_EXCEPTION.get();
        if (throwable == null)
            return;

        PENDING_EXCEPTION.remove();

        throw new CallbackInvocationException(throwable);
    }
}
