package io.github.jwharm.javagi.util;

import io.github.jwharm.javagi.interop.Interop;
import io.github.jwharm.javagi.base.Marshal;
import org.gtk.glib.Type;
import org.gtk.gobject.GObject;
import org.gtk.gobject.GObjects;
import org.gtk.gobject.ObjectClass;
import org.gtk.gobject.TypeFlags;

import java.lang.foreign.*;

public class ListIndexItem extends GObject {

    protected ListIndexItem(Addressable address) {
        super(address);
    }

    public static final Marshal<Addressable, ListIndexItem> fromAddress =
            (input, scope) -> input.equals(MemoryAddress.NULL) ? null : new ListIndexItem(input);

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
                    (gclass, data) -> {},
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
                .set(MemorySegment.ofAddress((MemoryAddress) handle(), getMemoryLayout().byteSize(), MemorySession.openImplicit()), value);
    }

    public int getIntValue() {
        return (int) getMemoryLayout()
                .varHandle(MemoryLayout.PathElement.groupElement("int_value"))
                .get(MemorySegment.ofAddress((MemoryAddress) handle(), getMemoryLayout().byteSize(), MemorySession.openImplicit()));
    }

    public ListIndexItem(int value) {
        super(getType(), null);
        setIntValue(value);
    }
}
