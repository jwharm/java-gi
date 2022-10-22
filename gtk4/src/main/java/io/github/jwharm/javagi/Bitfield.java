package io.github.jwharm.javagi;

public abstract class Bitfield {

    private int value;

    public Bitfield(int value) {
        this.value = value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public boolean equals(int bitfield) {
        return this.value == bitfield;
    }
    
    public boolean equals(Bitfield mask) {
        return this.value == mask.value;
    }

    public Bitfield combined(Bitfield mask) {
        this.setValue(this.getValue() | mask.getValue());
        return this;
    }

    public static Bitfield combined(Bitfield mask, Bitfield... masks) {
        for (Bitfield arg : masks) {
            mask.setValue(mask.getValue() | arg.getValue());
        }
        return mask;
    }

    public static int[] getValues(Bitfield[] array) {
        int[] values = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }
}
