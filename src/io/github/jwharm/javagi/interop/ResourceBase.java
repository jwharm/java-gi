package io.github.jwharm.javagi.interop;

import jdk.incubator.foreign.MemoryAddress;

import java.lang.ref.Cleaner;

public class ResourceBase implements NativeAddress {

    private final static Cleaner cleaner = Cleaner.create();
    private final State state;
    private final Cleaner.Cleanable cleanable;

    private static class State implements Runnable {
        Proxy proxy;

        State(Proxy proxy) {
            this.proxy = proxy;
        }

        public void run() {
            io.github.jwharm.javagi.interop.jextract.gtk_h.g_object_unref(proxy.HANDLE());
        }
    }

    public ResourceBase(Proxy proxy) {
        state = new State(proxy);
        cleanable = cleaner.register(this, state);
    }

    public MemoryAddress HANDLE() {
        return state.proxy.HANDLE();
    }
}
