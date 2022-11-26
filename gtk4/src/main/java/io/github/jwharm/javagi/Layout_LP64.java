package io.github.jwharm.javagi;

import java.lang.foreign.ValueLayout;

/**
 * This class contains the definitions of data type mappings from Java primitive values to native data types,
 * where long and pointer are 64-bit (LP64).
 */
public class Layout_LP64 {

    public final ValueLayout.OfBoolean C_BOOLEAN   = ValueLayout.JAVA_BOOLEAN;
    public final ValueLayout.OfByte    C_BYTE      = ValueLayout.JAVA_BYTE;
    public final ValueLayout.OfChar    C_CHAR      = ValueLayout.JAVA_CHAR;
    public final ValueLayout.OfShort   C_SHORT     = ValueLayout.JAVA_SHORT.withBitAlignment(16);
    public final ValueLayout.OfInt     C_INT       = ValueLayout.JAVA_INT.withBitAlignment(32);
    public final ValueLayout.OfLong    C_LONG      = ValueLayout.JAVA_LONG.withBitAlignment(64);
    public final ValueLayout.OfLong    C_LONG_LONG = ValueLayout.JAVA_LONG.withBitAlignment(64);
    public final ValueLayout.OfFloat   C_FLOAT     = ValueLayout.JAVA_FLOAT.withBitAlignment(32);
    public final ValueLayout.OfDouble  C_DOUBLE    = ValueLayout.JAVA_DOUBLE.withBitAlignment(64);
    public final ValueLayout.OfAddress ADDRESS     = ValueLayout.ADDRESS.withBitAlignment(64);
    
}
