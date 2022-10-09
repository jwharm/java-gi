package io.github.jwharm.javagi;

public abstract class Alias<T> {

    private T value;

    public Alias(T initialValue) {
        this.value = value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return this.value;
    }

    public static boolean[] getBooleanValues(Alias<Boolean>[] array) {
        boolean[] values = new boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }

    public static byte[] getByteValues(Alias<Byte>[] array) {
        byte[] values = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }

    public static char[] getCharacterValues(Alias<Character>[] array) {
        char[] values = new char[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }

    public static double[] getDoubleValues(Alias<Double>[] array) {
        double[] values = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }

    public static float[] getFloatValues(Alias<Float>[] array) {
        float[] values = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }

    public static int[] getIntegerValues(Alias<Integer>[] array) {
        int[] values = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }

    public static long[] getLongValues(Alias<Long>[] array) {
        long[] values = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }

    public static short[] getShortValues(Alias<Short>[] array) {
        short[] values = new short[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }
}
