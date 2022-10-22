package io.github.jwharm.javagi;

public abstract class Enumeration {

    private int value;

    public Enumeration(int value) {
        this.value = value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public boolean equals(int enumeration) {
        return this.value == enumeration;
    }
    
    public boolean equals(Enumeration enumeration) {
        return this.value == enumeration.value;
    }

    public static int[] getValues(Enumeration[] array) {
        int[] values = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }
}
