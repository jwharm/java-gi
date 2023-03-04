package io.github.jwharm.javagi.util;

import java.lang.foreign.*;
import java.util.ArrayList;

import io.github.jwharm.javagi.annotations.ClassInit;
import org.gnome.gio.ListModel;
import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.glib.Type;
import org.gnome.gobject.*;

/**
 * An implementation of the {@link ListModel} that returns the index of
 * a list item instead of an actual item. The index can be used with a
 * {@link java.util.List}, to work with Java objects in combination with
 * a {@link ListModel}.
 */
public class ListIndexModel extends GObject implements ListModel {

    private static final String LOG_DOMAIN = "java-gi";

    private static final int PROP_ITEM_TYPE = 1;
    private static final int PROP_N_ITEMS = 2;

    /**
     * Construct a ListIndexModel for the provided memory address.
     * @param address the memory address of the instance in native memory
     */
    public ListIndexModel(Addressable address) {
        super(address);
    }

    private static Type type;
    private ArrayList<ListIndex> items = new ArrayList<>();

    /**
     * Get the gtype of {@link ListIndexModel}, or register it as a new gtype
     * if it was not registered yet.
     * @return the {@link Type} that has been registered for {@link ListIndexModel}
     */
    public static Type getType() {
        if (type == null) {
            type = Types.register(ListIndexModel.class);

            // Implement the ListModel interface
            InterfaceInfo interfaceInfo = InterfaceInfo.allocate();
            interfaceInfo.writeInterfaceInit((iface, data) -> {
                ListModelInterface lmi = new ListModelInterface(iface.handle());
                lmi.overrideGetItemType(ListModel::getItemType);
                lmi.overrideGetNItems(ListModel::getNItems);
                lmi.overrideGetItem(ListModel::getItem);
            });
            GObjects.typeAddInterfaceStatic(type, ListModel.getType(), interfaceInfo);
        }
        return type;
    }

    @ClassInit
    public static void classInit(GObject.ObjectClass objectClass) {
        // Install properties "item-type" and "n-items"
        ParamSpec[] pspecs = new ParamSpec[3];
        pspecs[PROP_ITEM_TYPE] = GObjects.paramSpecGtype("item-type", "", "", Type.G_TYPE_OBJECT,
                ParamFlags.CONSTRUCT_ONLY.or(ParamFlags.READWRITE, ParamFlags.STATIC_NAME, ParamFlags.STATIC_BLURB, ParamFlags.STATIC_NICK));
        pspecs[PROP_N_ITEMS] = GObjects.paramSpecUint("n-items", "", "", 0, GLib.MAXUINT32, 0,
                ParamFlags.READABLE.or(ParamFlags.STATIC_NAME, ParamFlags.STATIC_BLURB, ParamFlags.STATIC_NICK));
        objectClass.overrideGetProperty(GObject::getProperty);
        objectClass.overrideSetProperty(GObject::setProperty);
        objectClass.installProperties(pspecs);
    }

    @Override
    public void getProperty(int propertyId, org.gnome.gobject.Value value, org.gnome.gobject.ParamSpec pspec) {
        switch(propertyId) {
            case PROP_ITEM_TYPE -> value.setGtype(ListIndex.getType());
            case PROP_N_ITEMS -> value.setUint(getNItems());
            default -> GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_WARNING, "Invalid property id %d\n", propertyId);
        }
    }

    @Override
    public void setProperty(int propertyId, org.gnome.gobject.Value value, org.gnome.gobject.ParamSpec pspec) {
        // Ignore
    }

    /**
     * Instantiate a new ListIndexModel with the provided size.
     * @param size the initial size of the list model
     */
    public static ListIndexModel newInstance(int size) {
        ListIndexModel model = GObject.newInstance(getType());
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
     * @return always returns the value of {@link ListIndex#getType()}
     */
    @Override
    public Type getItemType() {
        return ListIndex.getType();
    }

    /**
     * Returns the size of the list model
     * @return the value of the size field
     */
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

        private static Type type;
        private int index;

        public ListIndex(Addressable address) {
            super(address);
        }

        public static Type getType() {
            if (type == null)
                type = Types.register(ListIndex.class);
            return type;
        }

        public static ListIndex newInstance(int value) {
            ListIndex instance = GObject.newInstance(getType());
            instance.index = value;
            return instance;
        }

        public int getIndex() {
            return index;
        }
    }
}
