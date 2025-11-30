/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2025 Jan-Willem Harmannij
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

import org.gnome.glib.Variant;
import org.gnome.glib.VariantType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;

@NullMarked
public class Variants {

    /**
     * Unpack a GVariant into a Java Object.
     * <ul>
     * <li>A basic primitive type is returned as a Java boxed primitive.
     * <li>A string, object path or type signature is returned as a Java String.
     * <li>A nested GVariant is returned as a Java Variant (when {@code recursive}
     *     is {@code false}) or recursively unpacked (when {@code recursive} is
     *     {@code true}).
     * <li>A Maybe type is returned as either {@code null} or the unpacked value.
     * <li>An array is returned as an {@code ArrayList<?>} with unpacked values.
     * <li>A dictionary is returned as a {@code HashMap<?, ?>} with unpacked entries.
     * <li>A tuple is returned as a {@code ArrayList<Object>} with unpacked entries.
     * </ul>
     *
     * @param v a Variant to unpack
     * @param recursive whether to recursively unpack nested GVariants
     * @return the unpacked Java Object
     */
    public static @Nullable Object unpack(@Nullable Variant v, boolean recursive) {
        if (v == null)
            return null;

        VariantType t = v.getVariantType();
        if (t.isBasic()) {
            return switch (t.dupString()) {
                case "b" -> v.getBoolean();
                case "y" -> v.getByte();
                case "n" -> v.getInt16();
                case "q" -> v.getUint16();
                case "i" -> v.getInt32();
                case "u" -> v.getUint32();
                case "x" -> v.getInt64();
                case "t" -> v.getUint64();
                case "h" -> v.getHandle();
                case "d" -> v.getDouble();
                case "s", "o", "g" -> v.getString(null);
                default -> throw new IllegalArgumentException("Unsupported basic GVariantType " + t.dupString());
            };
        } else if (t.isMaybe()) {
            Variant value = v.getMaybe();
            return value == null ? null : unpack(value, recursive);
        } else if (t.isArray()) {
            if (t.element().isDictEntry()) {
                Map<@Nullable Object, @Nullable Object> map = new HashMap<>();
                for (int i = 0; i < v.nChildren(); i++) {
                    Variant entry = v.getChildValue(i);
                    Object key = unpack(entry.getChildValue(0), recursive);
                    Object val = unpack(entry.getChildValue(1), recursive);
                    map.put(key, val);
                }
                return map;
            } else {
                List<@Nullable Object> list = new ArrayList<>();
                for (int i = 0; i < v.nChildren(); i++) {
                    list.add(unpack(v.getChildValue(i), recursive));
                }
                return list;
            }
        } else if (t.isTuple()) {
            List<@Nullable Object> tuple = new ArrayList<>();
            for (int i = 0; i < v.nChildren(); i++) {
                tuple.add(unpack(v.getChildValue(i), recursive));
            }
            return tuple;
        } else if (t.isVariant()) {
            return recursive ? unpack(v.getVariant(), true) : v.getVariant();
        }
        throw new IllegalArgumentException("Unsupported type: " + t.dupString());
    }

    /**
     * Create a GVariant from a Java Object.
     * <ul>
     * <li>a {@code null} value is returned as a maybe ({@code "mv"})
     *     GVariant with value {@code null}
     * <li>a {@code boolean} is returned as a {@code boolean} GVariant
     * <li>a {@code byte} is returned as a {@code byte} GVariant
     * <li>a {@code char} is returned as a (single-character) string GVariant
     * <li>an {@code short} is returned as an {@code int16} GVariant
     * <li>an {@code int} is returned as an {@code int32} GVariant
     * <li>an {@code long} is returned as an {@code int64} GVariant
     * <li>a {@code float} or {@code double} is returned as a {@code double}
     *     GVariant
     * <li>a Java String is returned as a string GVariant
     * <li>a Java List or Set is returned as an array GVariant with recursively
     *     packed elements
     * <li>a Java Map is returned as a dictionary GVariant with recursively
     *     packed entries
     * <li>a Java Optional is returned as a maybe ({@code "m"}) GVariant with
     *     either the packed value or {@code null} (with type {@code "mv"})
     * </ul>
     * Note that arrays are not supported, only Lists.
     *
     * @param o the Java Object to pack into a GVariant
     * @return the GVariant with the packed Object
     */
    public static Variant pack(@Nullable Object o) {
        return switch (o) {
            case Character c -> Variant.string(c.toString());
            case Collection<?> c -> {
                var elemType = new VariantType(c.isEmpty() ? "mv" : formatString(c.iterator().next()));
                var elems = c.stream().map(Variants::pack).toArray(Variant[]::new);
                yield Variant.array(elemType, elems);
            }
            case Map<?, ?> map -> {
                var elemType = new VariantType("mv");
                if (!map.isEmpty()) {
                    var key = map.keySet().iterator().next();
                    var val = map.get(key);
                    elemType = new VariantType("{" + formatString(key) + formatString(val) + "}");
                }
                var entries = map.entrySet().stream()
                        .map(entry -> Variant.dictEntry(pack(entry.getKey()), pack(entry.getValue())))
                        .toArray(Variant[]::new);
                yield Variant.array(elemType, entries);
            }
            case Optional<?> opt -> {
                if (opt.isEmpty()) yield new Variant("mv", (Object) null);
                var elemType = new VariantType(formatString(opt.get()));
                yield Variant.maybe(elemType, pack(opt.get()));
            }
            case null, default -> new Variant(formatString(o), o);
        };
    }

    /**
     * Create a GVariant formatString that can be used for the provided Object.
     * For a List, Map or Optional, the formatString for the element(s) is returned.
     *
     * @param o a Java Object to create a GVariant formatString for
     * @return the generated formatString
     */
    private static String formatString(@Nullable Object o) {
        return switch (o) {
            case null -> "mv";
            case Boolean _ -> "b";
            case Byte _ -> "y";
            case Character _ -> "s";
            case Short _ -> "n";
            case Integer _ -> "i";
            case Long _ -> "x";
            case Double _, Float _ -> "d";
            case String _ -> "s";
            case Variant _ -> "v";
            case Collection<?> c -> "a" + (c.isEmpty() ? "mv" : formatString(c.iterator().next()));
            case Map<?, ?> map -> {
                if (map.isEmpty()) yield "a{mv}";
                var key = map.keySet().iterator().next();
                var val = map.get(key);
                yield "a{" + formatString(key) + formatString(val) + "}";
            }
            case Optional<?> opt -> "m" + opt.map(Variants::formatString).orElse("v");
            default -> throw new IllegalArgumentException("Unsupported object type: " + o.getClass());
        };
    }
}
