package io.github.jwharm.javagi;

/**
 * Base class for bitfield objects
 */
public abstract class Bitfield {

    private final int value;

    /**
     * Create a bitfield with the provided integer value
     * @param value the initial value of the bitfield
     */
    protected Bitfield(int value) {
        this.value = value;
    }

    /**
     * Get the value of the bitfield as an integer value
     * @return the value of the bitfield
     */
    public int getValue() {
        return this.value;
    }

    /**
     * Compares the value of this bitfield with the provided int value
     * @param bitfield an int value to compare with
     * @return returns true when {@code this.value == bitfield}
     */
    public boolean equals(int bitfield) {
        return this.value == bitfield;
    }

    /**
     * Compares the value of this bitfield with the value of the provided bitfield
     * @param mask another bitfield
     * @return returns true when {@code this.value == mask.value}
     */
    public boolean equals(Bitfield mask) {
        return this.value == mask.value;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Bitfield mask) return equals(mask);
        if (other instanceof Integer bitfield) return equals(bitfield.intValue());
        return false;
    }

    @Override
    public int hashCode() {
        return value;
    }

    /**
     * Convenience function to turn an array of Bitfield objects into 
     * an array of int values
     * @param array Array of Bitfield objects
     * @return array of int values
     */
    public static int[] getValues(Bitfield[] array) {
        int[] values = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }
}
