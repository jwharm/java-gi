/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2026 Jan-Willem Harmannij
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
package org.gnome.gio;

import java.lang.FunctionalInterface;
import java.lang.Throwable;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import javax.annotation.processing.Generated;
import org.javagi.base.ExceptionHandler;
import org.javagi.base.FunctionPointer;
import org.javagi.gobject.InstanceCache;
import org.javagi.interop.Interop;

///
/// Functional interface declaration of the {@code AsyncReadyCallback} callback.
///
/// @see AsyncReadyCallback#run
///
@FunctionalInterface
@Generated("org.javagi.JavaGI")
public interface AsyncReadyCallback extends FunctionPointer {
    ///
    /// Type definition for a function that will be called back when an asynchronous
    /// operation within GIO has been completed. `GAsyncReadyCallback`
    /// callbacks from `GTask` are guaranteed to be invoked in a later
    /// iteration of the thread-default main context
    /// (see [MainContext#pushThreadDefault][org.gnome.glib.MainContext#pushThreadDefault])
    /// where the `GTask` was created. All other users of
    /// `GAsyncReadyCallback` must likewise call it asynchronously in a
    /// later iteration of the main context.
    ///
    /// The asynchronous operation is guaranteed to have held a reference to the
    /// source Object from the time when the `*_async()` function was called, until
    /// after this callback returns.
    ///
    /// @param res a `GAsyncResult`.
    ///
    void run(AsyncResult res);

    ///
    /// Creates a native function pointer to the {@link #upcall} method.
    ///
    /// @param arena the arena in which the function pointer is allocated
    /// @return the native function pointer
    ///
    default MemorySegment toCallback(Arena arena) {
        FunctionDescriptor _fdesc = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS,
                ValueLayout.ADDRESS, ValueLayout.ADDRESS);
        MethodHandle _handle = Interop.upcallHandle(MethodHandles.lookup(), AsyncReadyCallback.class,  _fdesc);
        return Linker.nativeLinker().upcallStub(_handle.bindTo(this), _fdesc, arena);
    }

    ///
    /// The {@code upcall} method is called from native code. The parameters
    /// are marshaled and {@link #run} is executed.
    ///
    default void upcall(MemorySegment sourceObject, MemorySegment res, MemorySegment data) {
        try {
            InstanceCache.refOnce(res);
            run((AsyncResult) InstanceCache.get(res, AsyncResult.AsyncResult$Impl::new));
        } catch (Throwable _t) {
            ExceptionHandler.handleException(_t, "AsyncReadyCallback");
        }
    }
}
