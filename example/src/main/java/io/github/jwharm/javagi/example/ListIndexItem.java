package io.github.jwharm.javagi.example;

import io.github.jwharm.javagi.Interop;
import io.github.jwharm.javagi.Marshal;
import io.github.jwharm.javagi.Ownership;
import org.gtk.glib.Type;
import org.gtk.gobject.*;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

public class ListIndexItem extends GObject {

    protected ListIndexItem(Addressable address, Ownership ownership) {
        super(address, ownership);
    }

    public static final Marshal<Addressable, ListIndexItem> fromAddress =
            (input, ownership) -> input.equals(MemoryAddress.NULL) ? null : new ListIndexItem(input, ownership);

    public static MemoryLayout getMemoryLayout() {
        return MemoryLayout.structLayout(
                GObject.getMemoryLayout().withName("parent_instance"),
                Interop.valueLayout.C_INT.withName("int_value")
        ).withName("ListIndexModel");
    }

    private static Type type;
    public static Type getType() {
        if (type == null) {
            // Register the new gtype
            type = GObjects.typeRegisterStaticSimple(
                    GObject.getType(),
                    "ListIndexItem",
                    (short) ObjectClass.getMemoryLayout().byteSize(),
                    gclass -> {},
                    (short) getMemoryLayout().byteSize(),
                    (inst, gclass) -> {},
                    TypeFlags.NONE
            );
        }
        Interop.register(type, fromAddress);
        return type;
    }

    public void setIntValue(int value) {
        getMemoryLayout()
                .varHandle(MemoryLayout.PathElement.groupElement("int_value"))
                .set(MemorySegment.ofAddress((MemoryAddress) handle(), getMemoryLayout().byteSize(), Interop.getScope()), value);
    }

    public int getIntValue() {
        return (int) getMemoryLayout()
                .varHandle(MemoryLayout.PathElement.groupElement("int_value"))
                .get(MemorySegment.ofAddress((MemoryAddress) handle(), getMemoryLayout().byteSize(), Interop.getScope()));
    }

    public ListIndexItem(int value) {
        super(getType(), null);
        setIntValue(value);
    }
}
