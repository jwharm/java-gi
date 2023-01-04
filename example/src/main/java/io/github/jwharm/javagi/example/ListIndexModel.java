package io.github.jwharm.javagi.example;

import io.github.jwharm.javagi.Interop;
import io.github.jwharm.javagi.Marshal;
import io.github.jwharm.javagi.Ownership;
import org.gtk.gio.ListModel;
import org.gtk.gio.ListModelInterface;
import org.gtk.glib.Type;
import org.gtk.gobject.*;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

public class ListIndexModel extends GObject implements ListModel {

    protected ListIndexModel(Addressable address, Ownership ownership) {
        super(address, ownership);
    }

    public static final Marshal<Addressable, ListIndexModel> fromAddress =
            (input, ownership) -> input.equals(MemoryAddress.NULL) ? null : new ListIndexModel(input, ownership);

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
                        ListModelInterface lmi = ListModelInterface.fromAddress.marshal(iface.handle(), Ownership.NONE);
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
        super(newWithProperties(getType(), 0, new String[0], new Value[0]).handle(), Ownership.FULL);
    }

    public ListIndexModel(int size) {
        this();
        setSize(size);
    }

    public void setSize(int size) {
        getMemoryLayout()
                .varHandle(MemoryLayout.PathElement.groupElement("size"))
                .set(MemorySegment.ofAddress((MemoryAddress) handle(), getMemoryLayout().byteSize(), Interop.getScope()), size);
    }

    @Override
    public Type getItemType() {
        return ListIndexItem.getType();
    }

    @Override
    public int getNItems() {
        return (int) getMemoryLayout()
                .varHandle(MemoryLayout.PathElement.groupElement("size"))
                .get(MemorySegment.ofAddress((MemoryAddress) handle(), getMemoryLayout().byteSize(), Interop.getScope()));
    }

    @Override
    public GObject getItem(int position) {
        return new ListIndexItem(position);
    }
}
