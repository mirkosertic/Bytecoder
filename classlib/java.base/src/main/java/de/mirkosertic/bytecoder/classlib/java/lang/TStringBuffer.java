/*
 * Copyright 2023 Mirko Sertic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class TStringBuffer {

    public TStringBuffer() {
        this(10);
    }

    public TStringBuffer(final int capacity) {
        initializeWith(capacity);
    }

    public TStringBuffer(final String str) {
        this();
        append(str);
    }

    native void initializeWith(int capacity);

    public native StringBuffer append(final String str);

    public StringBuffer append(final byte value) {
        return append(Byte.toString(value));
    }

    public StringBuffer append(final char value) {
        return append(Character.toString(value));
    }

    public StringBuffer append(final short value) {
        return append(Short.toString(value));
    }

    public StringBuffer append(final int value) {
        return append(Integer.toString(value));
    }

    public StringBuffer append(final long value) {
        return append(Long.toString(value));
    }

    public StringBuffer append(final float value) {
        return append(Float.toString(value));
    }

    public StringBuffer append(final double value) {
        return append(Double.toString(value));
    }

    public StringBuffer append(final Object value) {
        if (value == null) {
            return append("null");
        }
        return append(value.toString());
    }

    public native String toString();
}
