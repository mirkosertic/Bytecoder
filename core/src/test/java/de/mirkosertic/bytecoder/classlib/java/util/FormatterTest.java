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

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Formattable;
import java.util.Formatter;

@RunWith(BytecoderUnitTestRunner.class)
public class FormatterTest {

    @Test
    public void testSimplePrint() {
        final Formatter f = new Formatter();
        final String result = f.format("%s, %s!", "Hello", "World").toString();
        System.out.println(result);
        Assert.assertEquals("Hello, World!", result);
    }

    @Test
    public void testStringFormat() {
        final String result = String.format("%s, %s!", "Hello", "World");
        System.out.println(result);
        Assert.assertEquals("Hello, World!", result);
    }

    @Test
    public void testStringFormatUppercase() {
        final String result = String.format("%S, %S!", "Hello", "World");
        System.out.println(result);
        Assert.assertEquals("HELLO, WORLD!", result);
    }

    @Test
    public void testStringFormatArgumentIndex() {
        final String result = String.format("%2$s, %1$s!", "World", "Hello");
        System.out.println(result);
        Assert.assertEquals("Hello, World!", result);
    }

    @Test
    public void testUselessWidthAndPrecision() {
        final Formattable f = new Formattable() {
            @Override
            public void formatTo(final Formatter formatter, final int flags, final int width, final int precision) {
                final StringBuilder sb = new StringBuilder();
                sb.append(flags);
                sb.append(":");
                sb.append(width);
                sb.append(":");
                sb.append(precision);
                formatter.format("%s", sb);
            }
        };
        final String result = String.format("%10.3s", f);
        System.out.println(result);
        Assert.assertEquals("0:10:3", result);
    }

    @Test
    public void testUselessPrecision() {
        final Formattable f = new Formattable() {
            @Override
            public void formatTo(final Formatter formatter, final int flags, final int width, final int precision) {
                final StringBuilder sb = new StringBuilder();
                sb.append(flags);
                sb.append(":");
                sb.append(width);
                sb.append(":");
                sb.append(precision);
                formatter.format("%s", sb);
            }
        };
        final String result = String.format("%.3s", f);
        System.out.println(result);
        Assert.assertEquals("0:-1:3", result);
    }

}
