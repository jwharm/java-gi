/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 Jan-Willem Harmannij
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

import java.lang.foreign.Arena;
import java.lang.ref.Cleaner;

/**
 * Helper class to separate the cleanup logic from the object being cleaned
 *
 * @param arena the Arena that will be closed
 */
public record ArenaCloseAction(Arena arena) implements Runnable {

    // Cleaner used to close the arena
    public static final Cleaner CLEANER = Cleaner.create();

    @Override
    public void run() {
        arena.close();
    }
}
