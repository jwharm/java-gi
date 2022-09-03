package io.github.jwharm.javagi.interop;

import jdk.incubator.foreign.MemoryAddress;

import java.lang.ref.Cleaner;

public class Proxy implements NativeAddress {

    private final static Cleaner cleaner = Cleaner.create();
    private final Proxy.State state;
    private final Cleaner.Cleanable cleanable;

    private static class State implements Runnable {
        MemoryAddress address;

        State(MemoryAddress address) {
            System.out.println("register " + address);
            this.address = address;
        }

        public void run() {
            System.out.println("unref " + address);
            io.github.jwharm.javagi.interop.jextract.gtk_h.g_object_unref(address);
        }
    }

    public Proxy(MemoryAddress handle) {
        state = new Proxy.State(handle);
        cleanable = cleaner.register(this, state);
    }

    @Override
    public final MemoryAddress HANDLE() {
        return state.address;
    }
}
