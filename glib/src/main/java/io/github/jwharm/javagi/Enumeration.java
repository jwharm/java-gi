package io.github.jwharm.javagi;

/**
 * Base class of enumeration objects
 */
public abstract class Enumeration {

    private int value;

    /**
     * Create a new enumeration instance with the provided value
     * @param value the initial enumeration value
     */
    public Enumeration(int value) {
        this.value = value;
    }

    /**
     * Set the value of this enumeration to the provided value
     * @param value the new value
     */
    public void setValue(int value) {
        this.value = value;
    }

    /**
     * Get the integer value of this enumeration
     * @return the integer value of this enumeration value
     */
    public int getValue() {
        return this.value;
    }

    /**
     * Compare this enumeration value with an integer
     * @param enumeration the integer to compare this enumeration value with
     * @return true when {@code this.value == enumeration}
     */
    public boolean equals(int enumeration) {
        return this.value == enumeration;
    }

    /**
     * Compare this enumeration value with another enumeration value
     * @param enumeration another enumeration value
     * @return true when {@code this.value == enumeration.value}
     */
    public boolean equals(Enumeration enumeration) {
        return this.value == enumeration.value;
    }

    /**
     * Convenience function to transfer an array of Enumeration objects 
     * into an array of integer values.
     * @param array An array of Enumeration objects
     * @return an array of the integer values of the provided Enumeration objects
     */
    public static int[] getValues(Enumeration[] array) {
        int[] values = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }
}
