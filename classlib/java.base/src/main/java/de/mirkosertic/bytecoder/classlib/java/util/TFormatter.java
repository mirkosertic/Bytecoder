/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.classlib.java.util;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.VM;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Formatter;
import java.util.Locale;

@SubstitutesInClass(completeReplace = true)
public class TFormatter {

    private Locale locale;
    private final Appendable out;
    private IOException exception;

    public TFormatter() {
        this(VM.defaultLocale());
    }

    public TFormatter(final Locale aLocale) {
        locale = aLocale;
        out = new StringBuilder();
    }

    public TFormatter(final PrintStream aOut) {
        this((Appendable) aOut);
    }

    public TFormatter(final Appendable aOut) {
        locale = VM.defaultLocale();
        out = aOut;
    }

    public Formatter format(final String aPattern, final Object... aValues) {
        formatValues(aPattern, aValues);
        return (Formatter) (Object) this;
    }

    public Formatter format(final Locale aLocale, final String aPattern, final Object... aValues) {
        locale = aLocale;
        formatValues(aPattern, aValues);
        return (Formatter) (Object) this;
    }

    private void formatValues(final String aPattern, final Object... aValues) {
        exception = null;
        try {
            new PatternWriter(aPattern, aValues).write();
        } catch (final IOException e) {
            exception = e;
        }
    }

    public void flush() {
    }

    public void close() {
    }

    public Appendable out() {
        return out;
    }

    public IOException ioException() {
        return exception;
    }

    @Override
    public String toString() {
        return out.toString();
    }

    public Locale locale() {
        return locale;
    }

    private class PatternWriter {
        private int parsePosition;
        private int valueIndex;
        private final String pattern;
        private final Object[] values;

        PatternWriter(final String aPattern, final Object[] aValues) {
            parsePosition = 0;
            valueIndex = 0;
            pattern = aPattern;
            values = aValues;
        }

        void write() throws IOException {
            while (parsePosition < pattern.length()) {
                final char c = pattern.charAt(parsePosition);
                if (c == '%') {
                    parsePosition++;
                    write(parseFormatSpecifier());
                    valueIndex ++;
                } else {
                    out.append(c);
                    parsePosition++;
                }
            }
        }

        private void write(final FormatSpecifier aSpecifier) throws IOException {
            final Object valueToWrite = values[aSpecifier.valueIndex];
            aSpecifier.conversion.writeTo(valueToWrite, aSpecifier, out);
        }

        FormatSpecifier parseFormatSpecifier() {
            final FormatSpecifier spec = new FormatSpecifier();
            char c = pattern.charAt(parsePosition);
            handler: while (true) {
                switch (c) {
                    case 'b':
                        spec.conversion = new BooleanConversion();
                        spec.uppercase = false;
                        parsePosition++;
                        break handler;
                    case 'B':
                        spec.conversion = new BooleanConversion();
                        spec.uppercase = true;
                        parsePosition++;
                        break handler;
                    case 'h':
                        throw new IllegalArgumentException();
                    case 'H':
                        throw new IllegalArgumentException();
                    case 's':
                        spec.conversion = new StringConversion();
                        spec.uppercase = false;
                        parsePosition++;
                        break handler;
                    case 'S':
                        spec.conversion = new StringConversion();
                        spec.uppercase = true;
                        parsePosition++;
                        break handler;
                    case '%':
                        spec.conversion = new PercentConversion();
                        parsePosition++;
                        break handler;
                    case 'n':
                        spec.conversion = new LinefeedConversion();
                        parsePosition++;
                        break handler;
                    case 'c':
                    case 'C':
                    case 'd':
                    case 'o':
                    case 'x':
                    case 'X':
                    case 'e':
                    case 'E':
                    case 'f':
                    case 'g':
                    case 'G':
                    case 'a':
                    case 'A':
                        throw new IllegalArgumentException();
                    case 't':
                    case 'T':
                        throw new IllegalArgumentException();
                }
                parsePosition++;
                c = pattern.charAt(parsePosition);
            }

            if (spec.valueIndex == -1) {
                spec.valueIndex = valueIndex;
            }
            return spec;
        }

        class FormatSpecifier {
            int valueIndex = -1;
            boolean uppercase = false;
            Conversion conversion;
        }
    }

    interface Conversion {
        void writeTo(Object aValueToWrite, PatternWriter.FormatSpecifier aSpecifier, Appendable aOut) throws IOException;
    }

    public static class BooleanConversion implements Conversion {

        @Override
        public void writeTo(Object aValueToWrite, PatternWriter.FormatSpecifier aSpecifier, Appendable aOut) throws IOException {
            if (aValueToWrite == null) {
                if (aSpecifier.uppercase) {
                    aOut.append("NULL");
                } else {
                    aOut.append("null");
                }
            } else if (aValueToWrite instanceof Boolean) {
                if (aSpecifier.uppercase) {
                    aOut.append(String.valueOf(((Boolean) aValueToWrite).booleanValue()).toUpperCase());
                } else {
                    aOut.append(String.valueOf(((Boolean) aValueToWrite).booleanValue()));
                }
            } else {
                if (aSpecifier.uppercase) {
                    aOut.append("true");
                } else {
                    aOut.append("true");
                }
            }
        }
    }

    public static class StringConversion implements Conversion {
        @Override
        public void writeTo(Object aValueToWrite, PatternWriter.FormatSpecifier aSpecifier, Appendable aOut) throws IOException {
            if (aValueToWrite == null) {
                if (aSpecifier.uppercase) {
                    aOut.append("NULL");
                } else {
                    aOut.append("null");
                }
            } else {
                if (aSpecifier.uppercase) {
                    aOut.append(aValueToWrite.toString().toUpperCase());
                } else {
                    aOut.append(aValueToWrite.toString());
                }
            }
        }
    }

    public static class PercentConversion implements Conversion {
        @Override
        public void writeTo(Object aValueToWrite, PatternWriter.FormatSpecifier aSpecifier, Appendable aOut) throws IOException {
            aOut.append("%");
        }
    }

    public static class LinefeedConversion implements Conversion {
        @Override
        public void writeTo(Object aValueToWrite, PatternWriter.FormatSpecifier aSpecifier, Appendable aOut) throws IOException {
            aOut.append(System.lineSeparator());
        }
    }
}
