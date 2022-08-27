package org.gtk.gobject;

import jdk.incubator.foreign.MemoryAddress;
import org.gtk.interop.ResourceProxy;

public class Object extends ResourceProxy {

    public Object(MemoryAddress handle) {
        super(handle);
    }

    public void freezeNotify() {
        org.gtk.interop.jextract.gtk_h.g_object_freeze_notify(HANDLE());
    }

    public boolean isFloating() {
        return org.gtk.interop.jextract.gtk_h.g_object_is_floating(HANDLE()) != 0;
    }

    public Object ref() {
        org.gtk.interop.jextract.gtk_h.g_object_ref(HANDLE());
        return this;
    }

    public Object refSink() {
        org.gtk.interop.jextract.gtk_h.g_object_ref_sink(HANDLE());
        return this;
    }

    public void runDispose() {
        org.gtk.interop.jextract.gtk_h.g_object_run_dispose(HANDLE());
    }

    public Object takeRef() {
        org.gtk.interop.jextract.gtk_h.g_object_take_ref(HANDLE());
        return this;
    }

    public void thawNotify() {
        org.gtk.interop.jextract.gtk_h.g_object_thaw_notify(HANDLE());
    }

    public void unref() {
        org.gtk.interop.jextract.gtk_h.g_object_unref(HANDLE());
    }
}
