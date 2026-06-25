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
import org.javagi.interop.InteropException;
import org.jspecify.annotations.Nullable;

///
/// Handles exceptions thrown by Java callback methods.
///
/// When a Java callback method that is invoked from native code throws an
/// exception, the JVM will immediately crash. Java-GI therefore catches these
/// exceptions.
///
/// By default, the exception is stored in a `ThreadLocal` field and will
/// later be thrown (wrapped in an [InteropException]), immediately after a
/// native method call in the same thread has completed. This can optionally
/// be disabled by setting the environment variable
/// `java-gi.discard-callback-exceptions` to `"true"` (ignoring case).
///
/// When the environment variable `java-gi.log-callback-exceptions` is set to
/// `"true"` (ignoring case), Java-GI will log the exception on `stderr` (using
/// `g_log()` with level `WARNING`).
///
/// For performance reasons, the values of the two environment variables is
/// cached, so the log/rethrow behavior cannot be changed during runtime.
///
public class ExceptionHandler {
    private static final boolean DISCARD_EXCEPTIONS =
            Boolean.getBoolean("java-gi.discard-callback-exceptions");

    private static final boolean LOG_EXCEPTIONS =
            Boolean.getBoolean("java-gi.log-callback-exceptions");

    private static final ThreadLocal<@Nullable Throwable> PENDING_EXCEPTION = new ThreadLocal<>();

    ///
    /// Handles exceptions that were thrown in a Java callback method that was
    /// invoked from native code.
    ///
    /// When the environment variable `java-gi.log-callback-exceptions` is
    /// `"true"` (ignoring casse), the exception will be logged to `stderr`.
    ///
    /// Unless the environment variable `java-gi.discard-callback-exceptions`
    /// is `"true"` (ignoring case), the exception is stored to be thrown
    /// later.
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
        if (suppressed != null)
            throwable.addSuppressed(suppressed);

        PENDING_EXCEPTION.set(throwable);
    }

    ///
    /// When propagation of exceptions is not disabled with the envrionment
    /// variable `java-gi.discard-callback-exceptions`, and an exception was
    /// stored with [#handleException], the exception is wrapped in an
    /// [InteropException] and thrown.
    ///
    /// **Note:** The **cause** of the exception points to the actual location
    /// where the exception occured, while the outer InteropException is thrown
    /// by the first method that completed in the same thread where the original
    /// exception occured. For example, when a GLib
    /// [idle callback][GLib#idleAdd] throws an exception, it will be rethrown
    /// from a completely unrelated callsite that just happened to be scheduled
    /// after the idle callback on the GLib main loop.
    ///
    /// @throws InteropException wraps the exception that occured in
    ///         a Java callback method.
    ///
    public static void propagateExceptions() throws InteropException {
        if (DISCARD_EXCEPTIONS)
            return;

        Throwable throwable = PENDING_EXCEPTION.get();
        if (throwable == null)
            return;

        PENDING_EXCEPTION.remove();

        throw new InteropException(throwable);
    }
}
