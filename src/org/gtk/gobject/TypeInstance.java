package org.gtk.gobject;

import jdk.incubator.foreign.*;

/**
 * An opaque structure used as the base of all type instances.
 */
public class TypeInstance extends org.gtk.gobject.Object {

    public TypeInstance(MemoryAddress handle) {
        super(handle);
    }

    /** Cast object to TypeInstance */
    public static TypeInstance castFrom(org.gtk.gobject.Object gobject) {
        return new TypeInstance(gobject.HANDLE());
    }

    public jdk.incubator.foreign.MemoryAddress getPrivate(GType privateType) {
        var RESULT = org.gtk.interop.jextract.gtk_h.g_type_instance_get_private(HANDLE(), privateType.getValue());
        return RESULT;
    }

}
