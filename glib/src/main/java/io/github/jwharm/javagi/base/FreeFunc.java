package io.github.jwharm.javagi.base;

/**
 * Classes that implement the FreeFunc interface, have a free() method
 */
public interface FreeFunc {

    /**
     * Free native memory
     */
    void free();

}
