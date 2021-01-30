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
import java.util.Formattable;
import java.util.FormattableFlags;
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
            new PatternWriter(aPattern, aValues).write((Formatter) (Object) this);
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

        void write(final Formatter f) throws IOException {
            while (parsePosition < pattern.length()) {
                final char c = pattern.charAt(parsePosition);
                if (c == '%') {
                    parsePosition++;
                    write(f, parseFormatSpecifier());
                    valueIndex ++;
                } else {
                    out.append(c);
                    parsePosition++;
                }
            }
        }

        private void write(final Formatter f, final FormatSpecifier aSpecifier) throws IOException {
            final Object valueToWrite = values[aSpecifier.valueIndex];
            aSpecifier.conversion.writeTo(f, valueToWrite, out);
        }

        FormatSpecifier parseFormatSpecifier() {
            final FormatSpecifier spec = new FormatSpecifier();
            char c = pattern.charAt(parsePosition);
            int width = -1;
            int precision = -1;
            int flags = 0;
            boolean hasWidth = false;
            handler: while (true) {
                if (Character.isDigit(c)) {
                    final int startPosition = parsePosition;
                    while (Character.isDigit(c)) {
                        parsePosition++;
                        c = pattern.charAt(parsePosition);
                    }
                    if (c == '$') {
                        spec.valueIndex = Integer.parseInt(pattern.substring(startPosition, parsePosition)) - 1;
                    } else if (!hasWidth) {
                        width = Integer.parseInt(pattern.substring(startPosition, parsePosition));
                        hasWidth = true;
                    } else if (precision == -1) {
                        precision = Integer.parseInt(pattern.substring(startPosition, parsePosition));
                    } else {
                        throw new IllegalStateException(pattern);
                    }
                }
                switch (c) {
                    case '.':
                        hasWidth = true;
                        break;
                    case 'b':
                        spec.conversion = new BooleanConversion(width, precision, flags);
                        parsePosition++;
                        break handler;
                    case 'B':
                        spec.conversion = new BooleanConversion(width, precision, flags | FormattableFlags.UPPERCASE);
                        parsePosition++;
                        break handler;
                    case 'h':
                        throw new IllegalArgumentException(pattern);
                    case 'H':
                        throw new IllegalArgumentException(pattern);
                    case 's':
                        spec.conversion = new StringConversion(width, precision, flags);
                        parsePosition++;
                        break handler;
                    case 'S':
                        spec.conversion = new StringConversion(width, precision, flags | FormattableFlags.UPPERCASE);
                        parsePosition++;
                        break handler;
                    case '%':
                        spec.conversion = new PercentConversion(width, precision, flags);
                        parsePosition++;
                        break handler;
                    case 'n':
                        spec.conversion = new LinefeedConversion(width, precision, flags);
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
                        throw new IllegalArgumentException(pattern);
                    case 't':
                    case 'T':
                        throw new IllegalArgumentException(pattern);
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
            Conversion conversion;
        }
    }

    abstract static class Conversion {
        int width;
        int precision;
        int flags;

        public Conversion(final int width, final int precision, final int flags) {
            this.width = width;
            this.precision = precision;
            this.flags = flags;
        }

        abstract void writeTo(Formatter f, Object aValueToWrite, Appendable aOut) throws IOException;
    }

    public static class BooleanConversion extends Conversion {

        public BooleanConversion(final int width, final int precision, final int flags) {
            super(width, precision, flags);
        }

        @Override
        public void writeTo(final Formatter f, final Object aValueToWrite, final Appendable aOut) throws IOException {
            if (aValueToWrite == null) {
                if ((flags & FormattableFlags.UPPERCASE) > 0) {
                    aOut.append("NULL");
                } else {
                    aOut.append("null");
                }
            } else if (aValueToWrite instanceof Boolean) {
                if ((flags & FormattableFlags.UPPERCASE) > 0) {
                    aOut.append(String.valueOf(((Boolean) aValueToWrite).booleanValue()).toUpperCase());
                } else {
                    aOut.append(String.valueOf(((Boolean) aValueToWrite).booleanValue()));
                }
            } else {
                if ((flags & FormattableFlags.UPPERCASE) > 0) {
                    aOut.append("true");
                } else {
                    aOut.append("true");
                }
            }
        }
    }

    public static class StringConversion extends Conversion {

        public StringConversion(final int width, final int precision, final int flags) {
            super(width, precision, flags);
        }

        @Override
        public void writeTo(final Formatter f, final Object aValueToWrite, final Appendable aOut) throws IOException {
            if (aValueToWrite == null) {
                if ((flags & FormattableFlags.UPPERCASE) > 0) {
                    aOut.append("NULL");
                } else {
                    aOut.append("null");
                }
            } else if (aValueToWrite instanceof Formattable) {
                final Formattable formattable = (Formattable) aValueToWrite;

                formattable.formatTo(f, flags, width, precision);
            } else {
                if ((flags & FormattableFlags.UPPERCASE) > 0) {
                    aOut.append(aValueToWrite.toString().toUpperCase());
                } else {
                    aOut.append(aValueToWrite.toString());
                }
            }
        }
    }

    public static class PercentConversion extends Conversion {

        public PercentConversion(final int width, final int precision, final int flags) {
            super(width, precision, flags);
        }

        @Override
        public void writeTo(final Formatter f, final Object aValueToWrite, final Appendable aOut) throws IOException {
            aOut.append("%");
        }
    }

    public static class LinefeedConversion extends Conversion {

        public LinefeedConversion(final int width, final int precision, final int flags) {
            super(width, precision, flags);
        }

        @Override
        public void writeTo(final Formatter f, final Object aValueToWrite, final Appendable aOut) throws IOException {
            aOut.append(System.lineSeparator());
        }
    }
}
