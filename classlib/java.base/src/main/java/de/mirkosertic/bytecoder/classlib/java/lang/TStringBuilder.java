package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.lang.annotation.Native;

@SubstitutesInClass(completeReplace = true)
public class TStringBuilder implements CharSequence {

    @Native
    private Object nativeObject;

    public TStringBuilder() {
        this(10);
    }

    public TStringBuilder(final int capacity) {
        nativeObject = null;
        initializeWith(capacity);
    }

    native void initializeWith(int capacity);

    public native StringBuilder append(final String value);

    public native StringBuilder append(final byte value);

    public native StringBuilder append(final char value);

    public native StringBuilder append(final short value);

    public native StringBuilder append(final int value);

    public native StringBuilder append(final long value);

    public native StringBuilder append(final float value);

    public native StringBuilder append(final double value);

    public native StringBuilder append(final Object value);

    public native StringBuilder append(final CharSequence value, int a, int b);

    public native StringBuilder append(final char[] data, int a, int b);

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
