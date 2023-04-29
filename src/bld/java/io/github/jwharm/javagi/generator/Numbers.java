package io.github.jwharm.javagi.generator;

/**
 * Utility class for parsing numbers where it is unknown whether they are signed
 */
public class Numbers {
    public static Byte parseByte(String s) throws NumberFormatException {
        return isNegative(s) ? Byte.parseByte(s) : (byte) Integer.parseInt(s);
    }

    public static Short parseShort(String s) throws NumberFormatException {
        return isNegative(s) ? Short.parseShort(s) : (short) Integer.parseInt(s);
    }

    public static Integer parseInt(String s) throws NumberFormatException {
        return isNegative(s) ? Integer.parseInt(s) : Integer.parseUnsignedInt(s);
    }

    public static Long parseLong(String s) throws NumberFormatException {
        return isNegative(s) ? Long.parseLong(s) : Long.parseUnsignedLong(s);
    }

    private static boolean isNegative(String s) {
        return s.length() > 0 && s.charAt(0) == '-';
    }
}
