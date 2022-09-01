package io.github.jwharm.javagi.interop;

import jdk.incubator.foreign.MemoryAddress;

import java.lang.ref.Cleaner;

public class ResourceProxy implements NativeAddress {

    private final static Cleaner cleaner = Cleaner.create();
    private final State state;
    private final Cleaner.Cleanable cleanable;

    private static class State implements Runnable {
        MemoryAddress __HANDLE__;

        State(MemoryAddress handle) {
            __HANDLE__ = handle;
        }

        public void run() {
            io.github.jwharm.javagi.interop.jextract.gtk_h.g_object_unref(__HANDLE__);
        }
    }

    public ResourceProxy(MemoryAddress handle) {
        state = new State(handle);
        cleanable = cleaner.register(this, state);
    }

    public void setHANDLE(MemoryAddress handle) {
        state.__HANDLE__ = handle;
    }

    public MemoryAddress HANDLE() {
        return state.__HANDLE__;
    }
}
