/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 the Java-GI developers
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

package org.javagi.interop;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Keeps a list of open Arenas that will be closed in a DestroyNotify callback.
 * The DestroyNotify callback will know which Arena to close, based on the
 * hashcode that is passed in the user_data parameter.
 */
public class Arenas {
    /*
     * We don't actually store arenas in the hashmap, but CompletableFutures.
     *
     * The purpose of the CF is to ensure the arena is only closed after the
     * native function has returned. (Sometimes the DestroyNotify is called
     * before that, for example when g_idle_add() runs a very short-lived
     * callback function.) Because the native function and the DestroyNotify
     * run in separate threads, we use a CF to communicate from the function’s
     * thread that the DestroyNotify's thread is allowed to close the arena.
     *
     * This is why the hashmap doesn’t contain the arena, but a
     * CompletableFuture. close_cb() retrieves the CF from the hashmap and
     * queries its result. (This will block until the CF is completed.) Then it
     * removes the hashmap entry and closes the arena.
     *
     * After the native method call has returned, readyToClose() is called.
     * This will complete the CF, by passing it the arena, and close_cb() will
     * not block on it anymore.
     */

    // Contains all open callback arenas that are closed using DestroyNotify
    private static final Map<Integer, CompletableFuture<Arena>> ARENAS = new HashMap<>();

    /**
     * The upcall stub for the DestroyNotify callback method
     */
    public static final MemorySegment CLOSE_CB_SYM;

    // Allocate the upcall stub for the DestroyNotify callback method
    static {
        try {
            FunctionDescriptor _fdesc = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS);
            MethodHandle _handle = MethodHandles.lookup().findStatic(
                    Arenas.class, "close_cb", _fdesc.toMethodType());
            CLOSE_CB_SYM = Linker.nativeLinker().upcallStub(_handle, _fdesc, Arena.global());
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This is called by native code when it runs the DestroyNotify callback.
     * It will close the accompanying Arena.
     *
     * @param data pointer to the hashcode of the Arena to close
     */
    public static void close_cb(MemorySegment data) {
        if (MemorySegment.NULL.equals(data))
            return;

        int hashCode = data.reinterpret(ValueLayout.JAVA_INT.byteSize())
                           .get(ValueLayout.JAVA_INT, 0);
        ARENAS.get(hashCode).thenAccept(arena -> {
            ARENAS.remove(hashCode);
            if (arena != null)
                arena.close();
        });
    }

    /**
     * This will add the Arena to the global static list of open arenas, and
     * return a pointer to the hashcode of the Arena.
     *
     * @param  arena the Arena to cache
     * @return a pointer to the hashcode of the Arena
     */
    public static MemorySegment cacheArena(Arena arena) {
        int hashCode = arena.hashCode();
        var future = new CompletableFuture<Arena>();
        ARENAS.put(hashCode, future);
        return arena.allocateFrom(ValueLayout.JAVA_INT, hashCode);
    }

    /**
     * Allow the upcall stub allocation of the callback to be released.
     *
     * @param arena the cached Arena
     */
    public static void readyToClose(Arena arena) {
        CompletableFuture<Arena> future = ARENAS.get(arena.hashCode());
        if (future != null)
            future.complete(arena);
    }
}
