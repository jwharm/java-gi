package io.github.jwharm.javagi.example;

import io.github.jwharm.javagi.*;
import org.gtk.gio.ListModel;
import org.gtk.gio.ListModelInterface;
import org.gtk.glib.Type;
import org.gtk.gobject.*;
import org.gtk.gtk.*;

import java.lang.foreign.*;
import java.lang.invoke.VarHandle;

public class ListIndex extends GObject {
    private static final int PROP_ITEM_TYPE = 1;
    private static final String PROP_NAME = "item-type";
    private static final String TYPE_NAME = "ListIndex";
    private static final Type PARENT_TYPE = GObject.getType();

    private static final MemoryLayout memoryLayout = MemoryLayout.structLayout(
            GObject.getMemoryLayout().withName("parent_instance"),
            Interop.valueLayout.C_INT.withName("index"),
            Interop.valueLayout.C_INT.withName("size")
    ).withName(TYPE_NAME);

    private static Type type;
    public static Type getType() {
        if (type == null) {
            // Register the new gtype
            type = GObjects.typeRegisterStaticSimple(
                    PARENT_TYPE,
                    TYPE_NAME,
                    (short) ObjectClass.getMemoryLayout().byteSize(),
                    classInit,
                    (short) memoryLayout.byteSize(),
                    instanceInit,
                    TypeFlags.NONE
            );
            GObjects.typeAddInterfaceStatic(type, ListModel.getType(), InterfaceInfo.builder()
                    .setInterfaceInit(interfaceInit)
                    .setInterfaceData(null)
                    .setInterfaceFinalize(null)
                    .build());
        }
        return type;
    }

    private static final VarHandle index = memoryLayout.varHandle(MemoryLayout.PathElement.groupElement("index"));
    private static final VarHandle size = memoryLayout.varHandle(MemoryLayout.PathElement.groupElement("size"));

    public static ListIndex castFrom(GObject gobject) {
        if (GObjects.typeCheckInstanceIsA(TypeInstance.fromAddress.marshal(gobject.handle(), Ownership.NONE), getType())) {
            return new ListIndex(gobject.handle(), gobject.yieldOwnership());
        } else {
            throw new ClassCastException("Object type is not an instance of ListIndex");
        }
    }

    public static final Marshal<Addressable, ListIndex> fromAddress = (input, ownership) -> input.equals(MemoryAddress.NULL) ? null : new ListIndex(input, ownership);
    protected ListIndex(Addressable address, Ownership ownership) {
        super(address, ownership);
    }

    public ListIndex(int size) {
        this();
        setSizeIntenal(size);
    }

    public ListIndex() {
        super(new GObject(getType(), PROP_NAME, getType()).handle(),
                Ownership.FULL);
    }

    private static final InstanceInitFunc instanceInit = (instance, gClass) -> fromAddress.marshal(instance.handle(), Ownership.NONE).initInstance();

    private void initInstance() {
        setIndex(0);
        setSizeIntenal(0);
    }

    private static TypeClass parentClass = null;
    private static final ObjectClass.DisposeCallback instanceDispose = pointer -> {
        if (parentClass == null) System.out.println("ListIndex::instanceDispose (no parent)");
        else {
            InteropException ie = new InteropException("Could not dispose ListIndex");
            Gtk4Example.schedule(() -> {
                try {
                    var func = (MemoryAddress) ObjectClass.getMemoryLayout()
                            .varHandle(MemoryLayout.PathElement.groupElement("dispose"))
                            .get(MemorySegment.ofAddress(parentClass.handle().address(), ObjectClass.getMemoryLayout().byteSize(), Interop.getScope()));
                    var linked = Linker.nativeLinker().downcallHandle(func, ObjectClass.DisposeCallback.DESCRIPTOR);
                    linked.invoke(pointer);
                } catch (Throwable e) {
                    throw (InteropException) ie.initCause(e);
                }
            });
        }
    };

    private static final ObjectClass.SetPropertyCallback setProperty = (object, propertyId, value, paramSpec) -> {
        if (propertyId != PROP_ITEM_TYPE) System.out.println("ListIndex::setProperty (unknown property)");
    };

    private static final ObjectClass.GetPropertyCallback getProperty = (object, propertyId, value, paramSpec) -> {
        if (propertyId == PROP_ITEM_TYPE) value.setGtype(getType());
        else System.out.println("ListIndex::getProperty (unknown property)");
    };

    private static final ClassInitFunc classInit = klass -> {
        System.out.println("ListIndex::classInit");
        parentClass = klass.peekParent();
        ObjectClass objectClass = ObjectClass.fromAddress.marshal(GObjects.typeCheckClassCast(klass, PARENT_TYPE).handle(), Ownership.NONE);
        objectClass.setDispose(instanceDispose);
        objectClass.setGetProperty(getProperty);
        objectClass.setSetProperty(setProperty);

        ParamSpec paramType = GObjects.paramSpecGtype(PROP_NAME, "", "", PARENT_TYPE,
                ParamFlags.CONSTRUCT
                        .or(ParamFlags.READWRITE,
                                ParamFlags.STATIC_NAME,
                                ParamFlags.STATIC_NICK,
                                ParamFlags.STATIC_BLURB)
        );

        objectClass.installProperty(PROP_ITEM_TYPE, paramType);
    };

    private static final ListModelInterface.GetItemTypeCallback getItemType = address -> {
        System.out.println("ListIndex::getItemType");
        return getType();
    };
    private static final ListModelInterface.GetNItemsCallback getNItems = model -> fromAddress.marshal(model.handle(), Ownership.NONE).getSize();
    private static final ListModelInterface.GetItemCallback getItem = (model, position) -> {
        ListIndex item = fromAddress.marshal(model.handle(), Ownership.NONE).getItem(position);
        if (item == null) return null;
        return item;
    };

    public ListIndex getItem(int position) {
        if (position >= getSize() || position <= -1) return null;
        ListIndex result = new ListIndex(getSize());
        result.setIndex(position);
        return result;
    }

    private static final InterfaceInitFunc interfaceInit = iface -> {
        System.out.println("ListIndex::interfaceInit");
        ListModelInterface lmi = ListModelInterface.fromAddress.marshal(iface.handle(), Ownership.NONE);
        lmi.setGetItem(getItem);
        lmi.setGetNItems(getNItems);
        lmi.setGetItemType(getItemType);
    };

    public int getIndex() {
        return (int) ListIndex.index.get(MemorySegment.ofAddress((MemoryAddress) handle(), memoryLayout.byteSize(), Interop.getScope()));
    }

    public void setIndex(int index) {
        ListIndex.index.set(MemorySegment.ofAddress((MemoryAddress) handle(), memoryLayout.byteSize(), Interop.getScope()), index);
    }

    public int getSize() {
        return (int) ListIndex.size.get(MemorySegment.ofAddress((MemoryAddress) handle(), memoryLayout.byteSize(), Interop.getScope()));
    }

    public void setSize(int size) {
        int oldSize = getSize();
        setSizeIntenal(size);
        asListModel().itemsChanged(0, oldSize, size);
    }

    private void setSizeIntenal(int size) {
        ListIndex.size.set(MemorySegment.ofAddress((MemoryAddress) handle(), memoryLayout.byteSize(), Interop.getScope()), size);
    }

    public ListModel asListModel() {
        return ListModel.castFrom(this);
    }

    public SingleSelection inSingleSelection() {
        return new SingleSelection(asListModel());
    }

    public SelectionModel inSelectionModel() {
        return SelectionModel.castFrom(inSingleSelection());
    }

    public static int toIndex(ListItem item) {
        return castFrom(item.getItem()).getIndex();
    }
}
