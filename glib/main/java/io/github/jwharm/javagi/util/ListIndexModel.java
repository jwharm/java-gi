package io.github.jwharm.javagi.util;

import java.lang.foreign.*;
import java.util.ArrayList;

import io.github.jwharm.javagi.annotations.Property;
import io.github.jwharm.javagi.types.Types;
import org.gnome.gio.ListModel;
import org.gnome.glib.Type;
import org.gnome.gobject.*;

/**
 * An implementation of the {@link ListModel} interface that returns the
 * index of a list item instead of an actual item. The index can be used
 * to retrieve Java objects from a regular {@link java.util.List}.
 */
public class ListIndexModel extends GObject implements ListModel {

    private static final Type gtype = Types.register(ListIndexModel.class);
    private ArrayList<ListIndex> items = new ArrayList<>();

    /**
     * Construct a ListIndexModel for the provided memory address.
     * @param address the memory address of the instance in native memory
     */
    public ListIndexModel(MemorySegment address) {
        super(address);
    }

    /**
     * Instantiate a new ListIndexModel with the provided size.
     * @param size the initial size of the list model
     */
    public static ListIndexModel newInstance(int size) {
        ListIndexModel model = GObject.newInstance(gtype);
        model.setSize(size);
        return model;
    }

    /**
     * Set the size field to the provided value, and emit the "items-changed" signal.
     * @param size the new listmodel size
     */
    public void setSize(int size) {
        int oldSize = items.size();
        items = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
            items.add(ListIndex.newInstance(i));
        itemsChanged(0, oldSize, size);
    }

    /**
     * Returns the gtype of {@link ListIndex}
     * @return always returns the value of {@link ListIndex#gtype}
     */
    @Property(name="item-type", type=ParamSpecGType.class, constructOnly=true)
    @Override
    public Type getItemType() {
        return ListIndex.gtype;
    }

    /**
     * Returns the size of the list model
     * @return the value of the size field
     */
    @Property(name="n-items", type=ParamSpecUInt.class, writable=false)
    @Override
    public int getNItems() {
        return items.size();
    }

    /**
     * Returns a {@link ListIndex} with the requested position as its value
     * @param position the position of the item to fetch
     * @return a {@link ListIndex} with the requested position as its value
     */
    @Override
    public GObject getItem(int position) {
        if (position < 0 || position >= getNItems()) return null;
        return items.get(position);
    }

    /**
     * Small GObject-derived class with a numeric "index" field.
     */
    public static class ListIndex extends GObject {

        private static final Type gtype = Types.register(ListIndex.class);
        private int index;

        /**
         * Construct a new ListIndex Proxy instance
         * @param address the memory address of the native object instance
         */
        public ListIndex(MemorySegment address) {
            super(address);
        }

        /**
         * Construct a new ListIndex with the provided value
         * @param value the value of the ListIndex instance
         * @return a new ListIndex instance
         */
        public static ListIndex newInstance(int value) {
            ListIndex instance = GObject.newInstance(gtype);
            instance.index = value;
            return instance;
        }

        /**
         * Get the index of this ListIndex object
         * @return the index of this ListIndex object
         */
        public int getIndex() {
            return index;
        }
    }
}
