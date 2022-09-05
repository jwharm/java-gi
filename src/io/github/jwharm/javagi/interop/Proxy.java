package io.github.jwharm.javagi.interop;

import jdk.incubator.foreign.MemoryAddress;

import java.lang.ref.Cleaner;

public class Proxy implements NativeAddress {

    private final static Cleaner cleaner = Cleaner.create();
    private Proxy.State state;
    private Cleaner.Cleanable cleanable;

    private static class State implements Runnable {
        MemoryAddress address;

        State(MemoryAddress address) {
            this.address = address;
        }

        public void run() {
            io.github.jwharm.javagi.interop.jextract.gtk_h.g_object_unref(address);
        }
    }

    public Proxy(MemoryAddress handle, boolean ownedByCaller) {
        state = new Proxy.State(handle);
        if (ownedByCaller) {
            cleanable = cleaner.register(this, state);
        }
    }

    @Override
    public final MemoryAddress HANDLE() {
        return state.address;
    }
}
