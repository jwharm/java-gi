/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 the Java-GI developers
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

package io.github.jwharm.javagi.util;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Small utility functions for working with files
 */
public class FileUtils {

    public static File findFile(File folder, String fileNamePrefix) throws FileNotFoundException {
        File[] files = folder.listFiles();
        if (files != null)
            for (File file : files)
                if (file.isFile() && file.getName().startsWith(fileNamePrefix))
                    return file;
        throw new FileNotFoundException(fileNamePrefix + " not found in " + folder);
    }
}
