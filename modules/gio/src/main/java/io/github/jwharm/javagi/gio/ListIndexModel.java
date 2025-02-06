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

package io.github.jwharm.javagi.gio;

import java.util.ArrayList;

import io.github.jwharm.javagi.gobject.annotations.Property;
import io.github.jwharm.javagi.gobject.types.TypeCache;
import org.gnome.gio.ListModel;
import org.gnome.glib.Type;
import org.gnome.gobject.*;

/**
 * An implementation of the {@link ListModel} interface that returns the
 * index of a list item instead of an actual item. The index can be used
 * to retrieve Java objects from a regular {@link java.util.List}.
 */
public class ListIndexModel extends GObject
        implements ListModel<ListIndexModel.ListIndex> {

    private ArrayList<ListIndex> items = new ArrayList<>();

    /**
     * Return the GType for the ListIndexModel.
     *
     * @return the GType
     */
    public static Type getType() {
        return TypeCache.getType(ListIndexModel.class);
    }

    /**
     * Construct a ListIndexModel for the provided memory address.
     *
     * @param size the initial list size
     */
    public ListIndexModel(int size) {
        super();
        setSize(size);
    }

    /**
     * Construct a new ListIndexModel with the provided size.
     *
     * @param size the initial size of the list model
     * @deprecated Replaced with {@link #ListIndexModel(int)}
     */
    @Deprecated
    public static ListIndexModel newInstance(int size) {
        return new ListIndexModel(size);
    }

    /**
     * Set the size field to the provided value, and emit the "items-changed"
     * signal.
     *
     * @param size the new list model size
     */
    public void setSize(int size) {
        int oldSize = items.size();
        items = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
            items.add(new ListIndex(i));
        itemsChanged(0, oldSize, size);
    }

    /**
     * Get the gtype of {@link ListIndex}.
     *
     * @return always returns the value of {@link ListIndex#getType}
     */
    @Property(constructOnly = true)
    @Override
    public Type getItemType() {
        return ListIndex.getType();
    }

    /**
     * No-op. The item type is always {@link ListIndex#getType}.
     *
     * @param itemType ignored
     */
    @SuppressWarnings("unused")
    @Property(constructOnly = true)
    public void setItemType(Type itemType) {
    }

    /**
     * Get the size of the list model.
     *
     * @return the value of the size field
     */
    @Property(name="n-items", type=ParamSpecUInt.class, writable=false)
    @Override
    public int getNItems() {
        return items.size();
    }

    /**
     * Returns a {@link ListIndex} with the requested position as its value.
     *
     * @param  position the position of the item to fetch
     * @return a {@link ListIndex} with the requested position as its value
     */
    @Override
    public ListIndex getItem(int position) {
        if (position < 0 || position >= getNItems())
            return null;
        return items.get(position);
    }

    /**
     * Small GObject-derived class with a numeric "index" field.
     */
    public static class ListIndex extends GObject {
        private final int index;

        /**
         * Return the GType for the ListIndex.
         *
         * @return the GType
         */
        public static Type getType() {
            return TypeCache.getType(ListIndex.class);
        }

        /**
         * Construct a new ListIndex Proxy instance.
         *
         * @param value the index value
         */
        public ListIndex(int value) {
            super();
            this.index = value;
        }

        /**
         * Get the index of this ListIndex object.
         *
         * @return the index of this ListIndex object
         */
        public int getIndex() {
            return index;
        }
    }
}
