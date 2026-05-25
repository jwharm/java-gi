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

package org.javagi.gio;

import org.gnome.gio.*;
import org.gnome.glib.GLib;
import org.gnome.glib.MainContext;
import org.gnome.glib.MainLoop;
import org.javagi.base.GErrorException;
import org.javagi.base.Out;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.gnome.glib.GLib.PRIORITY_DEFAULT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test byte[] marshaling by reading and writing bytes to and from a file,
 * and running async copy and move operations
 */
public class FileTest {

    @TempDir
    public static Path tempDir;

    private File createFile(String name, String contents) throws GErrorException, IOException {
        // explicitly trigger initialization
        Gio.javagi$ensureInitialized();

        // create file
        File file = File.newForPath(tempDir.resolve(name).toString());

        // write to file
        try (var stream = file.create(FileCreateFlags.REPLACE_DESTINATION, null)) {
            stream.write(contents.getBytes(StandardCharsets.UTF_8), null);
        }

        return file;
    }

    @Test
    public void readFile() throws GErrorException, IOException {
        String output, input;

        input = "test string";
        File file = createFile("read.txt", input);

        try (var stream = file.openReadwrite(null)) {
            Out<byte[]> buffer = new Out<>(new byte[input.length()]);
            stream.getInputStream().read(buffer, null);
            byte[] result = buffer.get();
            assertNotNull(result);
            output = new String(result);
        }

        assertEquals(input, output);
    }

    @Test
    public void moveAsync() throws GErrorException, IOException {
        File orig = createFile("move.txt", "test string");
        File dest = File.newForPath(tempDir.resolve("moved.txt").toString());

        MainLoop loop = new MainLoop(MainContext.getThreadDefault(), true);
        GLib.idleAddOnce(() -> {
            orig.moveAsync(dest, FileCopyFlags.NONE, PRIORITY_DEFAULT, null, (_, _, _)-> {}, (_, res, _) -> {
                try {
                    assertTrue(orig.moveFinish(res));
                } catch (GErrorException e) {
                    fail(e);
                }
                GLib.idleAddOnce(loop::quit);
            });

        });
        loop.run();

        assertFalse(orig.queryExists(null));
        assertTrue(dest.queryExists(null));
    }

    @Test
    public void copyAsync() throws GErrorException, IOException {
        File orig = createFile("copy.txt", "test string");
        File dest = File.newForPath(tempDir.resolve("copied.txt").toString());

        MainLoop loop = new MainLoop(MainContext.getThreadDefault(), true);
        GLib.idleAddOnce(() -> {
            orig.copyAsync(dest, FileCopyFlags.NONE, PRIORITY_DEFAULT, null, (_, _, _)-> {}, (_, res, _) -> {
                try {
                    assertTrue(orig.copyFinish(res));
                } catch (GErrorException e) {
                    fail(e.getMessage());
                }
                GLib.idleAddOnce(loop::quit);
            });

        });
        loop.run();

        assertTrue(orig.queryExists(null));
        assertTrue(dest.queryExists(null));
    }
}
