package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.api.NativeReferenceHolder;
import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class TStringBuilder implements CharSequence, NativeReferenceHolder {

    public TStringBuilder() {
        this(10);
    }

    public TStringBuilder(final int capacity) {
        initializeWith(capacity);
    }

    public TStringBuilder(final String str) {
        this();
        append(str);
    }

    native void initializeWith(int capacity);

    public native StringBuilder append(final String value);

    public StringBuilder append(final byte value) {
        return append(Byte.toString(value));
    }

    public StringBuilder append(final char value) {
        return append(Character.toString(value));
    }

    public StringBuilder append(final short value) {
        return append(Short.toString(value));
    }

    public StringBuilder append(final int value) {
        return append(Integer.toString(value));
    }

    public StringBuilder append(final long value) {
        return append(Long.toString(value));
    }

    public StringBuilder append(final float value) {
        return append(Float.toString(value));
    }

    public StringBuilder append(final double value) {
        return append(Double.toString(value));
    }

    public StringBuilder append(final Object value) {
        if (value != null) {
            return append(value.toString());
        }
        return append("null");
    }

    public native StringBuilder append(final CharSequence value, int a, int b);

    public native StringBuilder append(final char[] data, int offset, int len);

    public native StringBuilder reverse();

    native void setLength(int size);

    public native String toString();

    @Override
    public native int length();

    @Override
    public native char charAt(int index);

    @Override
    public native CharSequence subSequence(int start, int end);
}
