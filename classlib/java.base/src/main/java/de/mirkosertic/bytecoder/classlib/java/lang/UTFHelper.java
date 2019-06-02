package de.mirkosertic.bytecoder.classlib.java.lang;

public class UTFHelper {

    static final int HI_BYTE_SHIFT;
    static final int LO_BYTE_SHIFT;
    static {
        HI_BYTE_SHIFT = 8;
        LO_BYTE_SHIFT = 0;
    }

    public static char[] toChars(final byte[] value) {
        final char[] dst = new char[value.length >> 1];
        getChars(value, 0, dst.length, dst, 0);
        return dst;
    }

    public static void getChars(final byte[] value, final int srcBegin, final int srcEnd, final char[] dst, int dstBegin) {
        // We need a range check here because 'getChar' has no checks
        for (int i = srcBegin; i < srcEnd; i++) {
            dst[dstBegin++] = getChar(value, i);
        }
    }

    static char getChar(final byte[] val, int index) {
        index <<= 1;
        return (char)(((val[index++] & 0xff) << HI_BYTE_SHIFT) |
                ((val[index]   & 0xff) << LO_BYTE_SHIFT));
    }

    public static byte[] toBytes(final char[] value, int off, final int len) {
        final byte[] val = newBytesFor(len);
        for (int i = 0; i < len; i++) {
            putChar(val, i, value[off]);
            off++;
        }
        return val;
    }

    public static byte[] newBytesFor(final int len) {
        return new byte[len << 1];
    }

    static void putChar(final byte[] val, int index, final int c) {
        index <<= 1;
        val[index++] = (byte)(c >> HI_BYTE_SHIFT);
        val[index]   = (byte)(c >> LO_BYTE_SHIFT);
    }
}
