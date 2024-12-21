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
import org.gnome.glib.Type;
import org.gnome.gobject.*;

import io.github.jwharm.javagi.base.Enumeration;

import static io.github.jwharm.javagi.gobject.types.Types.*;
import static org.gnome.gobject.GObjects.gtypeGetType;
import static org.gnome.gobject.GObjects.typeIsA;

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
        if (src == null)
            return null;

        Type type = src.readGType();
        if (type == null || type.equals(NONE))
            return null;

        // Fundamental types
        if (type.equals(BOOLEAN))        return src.getBoolean();
        if (type.equals(CHAR))           return src.getSchar();
        if (type.equals(DOUBLE))         return src.getDouble();
        if (type.equals(FLOAT))          return src.getFloat();
        if (type.equals(INT))            return src.getInt();
        if (type.equals(LONG))           return src.getLong();
        if (type.equals(STRING))         return src.getString();
        if (type.equals(ENUM))           return src.getEnum();
        if (type.equals(FLAGS))          return src.getFlags();
        if (type.equals(OBJECT))         return src.getObject();
        if (type.equals(POINTER))        return src.getPointer();
        if (type.equals(PARAM))          return src.getParam();

        // GType
        if (type.equals(gtypeGetType())) return src.getGtype();

        // Derived types
        if (typeIsA(type, OBJECT))       return src.getObject();
        if (typeIsA(type, ENUM))         return src.getEnum();
        if (typeIsA(type, FLAGS))        return src.getFlags();

        // Boxed types
        return src.getBoxed();
    }

    /**
     * Read the GType of the {@code dest} GValue and set the {@code src} object
     * (or boxed primitive value) as its value using the corresponding setter in
     * the {@link Value} proxy class.
     *
     * @param  src  the Java Object (or boxed primitive value) to put in the
     *              GValue. Should not be {@code null}
     * @param  dest the GValue to write to. Should not be {@code null}
     * @return {@code true} if the value was set, and {@code false} otherwise.
     */
    public static boolean objectToValue(Object src, Value dest) {
        if (src == null || dest == null)
            return false;

        Type type = dest.readGType();
        if (type == null)
            return false;

        if      (type.equals(BOOLEAN))        dest.setBoolean((Boolean) src);
        else if (type.equals(CHAR))           dest.setSchar((Byte) src);
        else if (type.equals(DOUBLE))         dest.setDouble((Double) src);
        else if (type.equals(FLOAT))          dest.setFloat((Float) src);
        else if (type.equals(INT))            dest.setInt((Integer) src);
        else if (type.equals(LONG))           dest.setLong(toInt(src));
        else if (type.equals(STRING))         dest.setString((String) src);
        else if (type.equals(ENUM))           dest.setEnum(((Enumeration) src).getValue());
        else if (type.equals(FLAGS))          dest.setFlags(((Enumeration) src).getValue());
        else if (type.equals(OBJECT))         dest.setObject((GObject) src);
        else if (type.equals(gtypeGetType())) dest.setGtype((Type) src);
        else if (type.equals(POINTER))        dest.setPointer((MemorySegment) src);
        else if (type.equals(PARAM))          dest.setParam((ParamSpec) src);
        else                                  dest.setBoxed((MemorySegment) src);

        return true;
    }

    /*
     * On Linux and macOS, Long values are 64 bit in native code. On Windows,
     * Long values are 32 bit. To preserve cross-platform compatibility, Java-GI
     * converts all Java Long values to Integers.
     */
    private static int toInt(Object src) {
        return src instanceof Long l ? l.intValue() : (Integer) src;
    }
}
