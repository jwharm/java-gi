package io.github.jwharm.javagi.base;

/**
 * This is a wrapper class for out-parameters of (usually primitive) 
 * values.
 * When a method expects an {@code Out<>} object, you have to instantiate 
 * one; you can optionally fill it with an initial value, and pass 
 * it to the method. After the method has returned, you can read 
 * the the value of the out-parameter with the {@link #get()} method.
 * @param <T> The parameter type.
 */
public class Out<T> {
    
    private T value;
    
    /**
     * Create an Out object with no initial value
     */
    public Out() {
    }
    
    /**
     * Create an Out object and set the initial value
     * @param value The initial value
     */
    public Out(T value) {
        this.value = value;
    }
    
    /**
     * Get the value from the out-parameter
     * @return the value of the out-parameter
     */
    public T get() {
        return value;
    }
    
    /**
     * Set the parameter to the provided value
     * @param value the value to set
     */
    public void set(T value) {
        this.value = value;
    }
}
