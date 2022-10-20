package io.github.jwharm.javagi;

import org.jetbrains.annotations.ApiStatus;

public class Out<T> {
    
    private T value;
    
    public Out() {
    }
    
    public T get() {
        return value;
    }
    
    @ApiStatus.Internal
    public void set(T value) {
        this.value = value;
    }
}
