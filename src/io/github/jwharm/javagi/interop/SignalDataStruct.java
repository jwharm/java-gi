package io.github.jwharm.javagi.interop;

import jdk.incubator.foreign.*;
import java.lang.invoke.VarHandle;

import static jdk.incubator.foreign.ValueLayout.*;

public class SignalDataStruct {

    static final GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        JAVA_INT.withBitAlignment(32).withName("func"),
        ADDRESS.withBitAlignment(64).withName("data")
    ).withName("interop.SignalDataStruct");

    public static MemoryLayout $LAYOUT() {
        return SignalDataStruct.$struct$LAYOUT;
    }

    static final VarHandle func$VH = $struct$LAYOUT.varHandle(PathElement.groupElement("func"));

    public static VarHandle func$VH() {
        return SignalDataStruct.func$VH;
    }

    public static MemoryAddress func$get(MemorySegment seg) {
        return (MemoryAddress) SignalDataStruct.func$VH.get(seg);
    }

    public static void func$set( MemorySegment seg, MemoryAddress x) {
        SignalDataStruct.func$VH.set(seg, x);
    }

    static final VarHandle data$VH = $struct$LAYOUT.varHandle(PathElement.groupElement("data"));

    public static VarHandle data$VH() {
        return SignalDataStruct.data$VH;
    }

    public static MemoryAddress data$get(MemorySegment seg) {
        return (MemoryAddress) SignalDataStruct.data$VH.get(seg);
    }

    public static void data$set( MemorySegment seg, MemoryAddress x) {
        SignalDataStruct.data$VH.set(seg, x);
    }

    public static long sizeof() {
        return $LAYOUT().byteSize();
    }

    public static MemorySegment allocate(SegmentAllocator allocator) {
        return allocator.allocate($LAYOUT());
    }

    public static MemorySegment allocate(ResourceScope scope) {
        return allocate(SegmentAllocator.nativeAllocator(scope));
    }
}


