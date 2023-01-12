package io.github.jwharm.javagi.example;

import io.github.jwharm.javagi.Interop;
import io.github.jwharm.javagi.Marshal;
import org.gtk.gio.ListModel;
import org.gtk.gio.ListModelInterface;
import org.gtk.glib.Type;
import org.gtk.gobject.*;

import java.lang.foreign.*;

public class ListIndexModel extends GObject implements ListModel {

    protected ListIndexModel(Addressable address) {
        super(address);
    }

    public static final Marshal<Addressable, ListIndexModel> fromAddress =
            (input, scope) -> input.equals(MemoryAddress.NULL) ? null : new ListIndexModel(input);

    public static MemoryLayout getMemoryLayout() {
        return MemoryLayout.structLayout(
                GObject.getMemoryLayout().withName("parent_instance"),
                Interop.valueLayout.C_INT.withName("size")
        ).withName("ListIndexModel");
    }

    private static Type type;
    public static Type getType() {
        if (type == null) {
            // Register the new gtype
            type = GObjects.typeRegisterStaticSimple(
                    GObject.getType(),
                    "ListIndexModel",
                    (short) ObjectClass.getMemoryLayout().byteSize(),
                    gclass -> {},
                    (short) getMemoryLayout().byteSize(),
                    (inst, gclass) -> {},
                    TypeFlags.NONE
            );
            GObjects.typeAddInterfaceStatic(type, ListModel.getType(), InterfaceInfo.builder()
                    .setInterfaceInit(iface -> {
                        ListModelInterface lmi = ListModelInterface.fromAddress.marshal(iface.handle(), null);
                        lmi.setGetItemType(ListModel::getItemType);
                        lmi.setGetNItems(ListModel::getNItems);
                        lmi.setGetItem(ListModel::getItem);
                    })
                    .setInterfaceData(null)
                    .setInterfaceFinalize(null)
                    .build());
        }
        Interop.register(type, fromAddress);
        return type;
    }

    private ListIndexModel() {
        super(getType(), null);
    }

    public ListIndexModel(int size) {
        this();
        setSize(size);
    }

    public void setSize(int size) {
        int oldSize = getNItems();
        getMemoryLayout()
                .varHandle(MemoryLayout.PathElement.groupElement("size"))
                .set(MemorySegment.ofAddress((MemoryAddress) handle(), getMemoryLayout().byteSize(), MemorySession.openImplicit()), size);
        itemsChanged(0, oldSize, size);
    }

    @Override
    public Type getItemType() {
        return ListIndexItem.getType();
    }

    @Override
    public int getNItems() {
        return (int) getMemoryLayout()
                .varHandle(MemoryLayout.PathElement.groupElement("size"))
                .get(MemorySegment.ofAddress((MemoryAddress) handle(), getMemoryLayout().byteSize(), MemorySession.openImplicit()));
    }

    @Override
    public GObject getItem(int position) {
        if (position < 0 || position >= getNItems()) return null;
        return new ListIndexItem(position);
    }
}
