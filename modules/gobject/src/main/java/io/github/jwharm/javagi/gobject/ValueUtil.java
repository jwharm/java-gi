/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 Jan-Willem Harmannij
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

package io.github.jwharm.javagi.gobject;

import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;

import io.github.jwharm.javagi.gobject.types.Types;
import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.glib.Type;
import org.gnome.gobject.*;

import io.github.jwharm.javagi.base.Enumeration;

import static io.github.jwharm.javagi.Constants.LOG_DOMAIN;

/**
 * Utility functions to convert a {@link Value} to and from a Java Object.
 */
public class ValueUtil {
    
    /**
     * Read the GType from the GValue, call the corresponding getter (using the
     * methods defined in the {@link Value} proxy class), and return the result.
     *
     * @param  src a GValue instance.
     * @return     a Java object (or boxed primitive value) that has been
     *             marshaled from the GValue, or {@code null} if {@code src} is
     *             null.
     */
    public static Object valueToObject(Value src) {
        if (src == null) {
            return null;
        }
        
        Type type = src.readGType();
        
        if (type == null || type.equals(Types.NONE)) {
            return null;
        }

        if (type.equals(Types.BOOLEAN))           return src.getBoolean();
        if (type.equals(Types.CHAR))              return src.getSchar();
        if (type.equals(Types.DOUBLE))            return src.getDouble();
        if (type.equals(Types.FLOAT))             return src.getFloat();
        if (type.equals(Types.INT))               return src.getInt();
        if (type.equals(Types.LONG))              return src.getLong();
        if (type.equals(Types.STRING))            return src.getString();
        if (type.equals(Types.ENUM))              return src.getEnum();
        if (type.equals(Types.FLAGS))             return src.getFlags();
        if (type.equals(Types.OBJECT))            return src.getObject();
        if (type.equals(GObjects.gtypeGetType())) return src.getGtype();
        if (type.equals(Types.POINTER))           return src.getPointer();
        if (type.equals(Types.PARAM))             return src.getParam();

        return src.getBoxed();
    }

    /**
     * Read the GType of the {@code dest} GValue and set the {@code src} object
     * (or boxed primitive value) as its value using the corresponding setter in
     * the {@link Value} proxy class.
     *
     * @param src  the Java Object (or boxed primitive value) to put in the
     *             GValue. Should not be {@code null}
     * @param dest the GValue to write to. Should not be {@code null}
     */
    public static void objectToValue(Object src, Value dest) {
        if (src == null || dest == null)
            return;

        Type type = dest.readGType();
        if (type == null)
            return;

        try {
            if (type.equals(Types.BOOLEAN)) {
                dest.setBoolean((Boolean) src);
            } else if (type.equals(Types.CHAR)) {
                dest.setSchar((Byte) src);
            } else if (type.equals(Types.DOUBLE)) {
                dest.setDouble((Double) src);
            } else if (type.equals(Types.FLOAT)) {
                dest.setFloat((Float) src);
            } else if (type.equals(Types.INT)) {
                dest.setInt((Integer) src);
            } else if (type.equals(Types.LONG)) {
                // On Linux: Value.setLong(long), on Windows: Value.setLong(int)
                // Use reflection to bypass the type checker
                for (Method m : Value.class.getDeclaredMethods()) {
                    if ("setLong".equals(m.getName())) {
                        m.invoke(dest, src);
                        break;
                    }
                }
            } else if (type.equals(Types.STRING)) {
                dest.setString((String) src);
            } else if (type.equals(Types.ENUM)) {
                dest.setEnum(((Enumeration) src).getValue());
            } else if (type.equals(Types.FLAGS)) {
                dest.setFlags(((Enumeration) src).getValue());
            } else if (type.equals(Types.OBJECT)) {
                dest.setObject((GObject) src);
            } else if (type.equals(GObjects.gtypeGetType())) {
                dest.setGtype((Type) src);
            } else if (type.equals(Types.POINTER)) {
                dest.setPointer((MemorySegment) src);
            } else if (type.equals(Types.PARAM)) {
                dest.setParam((ParamSpec) src);
            } else {
                // Boxed value
                dest.setBoxed((MemorySegment) src);
            }
        } catch (Exception e) {
            GLib.log(
                    LOG_DOMAIN,
                    LogLevelFlags.LEVEL_CRITICAL,
                    "ValueUtil: Cannot set Object with Class %s to GValue with GType %s: %s\n",
                    src.getClass().getSimpleName(),
                    GObjects.typeName(type),
                    e.toString()
            );
        }
    }
}
