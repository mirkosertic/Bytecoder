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
public class TCharacter {

    public static final int MIN_RADIX = 2;

    public static final int MAX_RADIX = 36;

    private final char value;

    public TCharacter(final char value) {
        this.value = value;
    }

    public static native int digit(char ch, int radix);

    public static int compare(final char x, final char y) {
        return x - y;
    }

    public static char forDigit(final int digit, final int radix) {
        if ((digit >= radix) || (digit < 0)) {
            return '\0';
        }
        if ((radix < Character.MIN_RADIX) || (radix > Character.MAX_RADIX)) {
            return '\0';
        }
        if (digit < 10) {
            return (char)('0' + digit);
        }
        return (char)('a' - 10 + digit);
    }

    public static native boolean isLowerCase(char c);

    public static native boolean isUpperCase(char c);

    public static native boolean isDigit(char c);

    public static native char toLowerCase(char c);

    public static native char toUpperCase(char c);

    public static native int getNumericValue(char c);

    public static native String toString(char c);

    public static Character valueOf(final char c) {
        return new Character(c);
    }
}
