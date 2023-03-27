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
package de.mirkosertic.bytecoder.classlib.java.lang.invoke;

import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(UnitTestRunner.class)
public class StringConcatFactoryTest {

    public static class Inst {

        private String append(final int aValue) {
            return "Hello" + aValue;
        }

        private String append(final String prefix, final int aValue) {
            return prefix + aValue;
        }

    }

    @Test
    public void testWithConstant() {
        final Inst i = new Inst();
        Assert.assertEquals("Hello42", i.append(42));
    }

    @Test
    public void testAppend() {
        final Inst i = new Inst();
        Assert.assertEquals("Hello42", i.append("Hello", 42));
    }

    @Test
    public void testAppendInLoopInteger() {
        String a = "";
        for (int i=0;i<5;i++) {
            a += i;
        }
        Assert.assertEquals("01234", a);
    }

    @Test
    public void testAppendInLoopLong() {
        String a = "";
        for (long i=0;i<5;i++) {
            a += i;
        }
        Assert.assertEquals("01234", a);
    }

    private static String getPrefix() {
        return "";
    }

    @Test
    public void testAppendPrimitives() {
        final byte b = (byte) 0;
        final char c = 'a';
        final short d = 1;
        final int e = 2;
        final float f = 3;
        final long g = 4;
        final double h = 5;
        final boolean i = true;
        final String theResult = getPrefix() + b + c + d + e + f + g + h + i;
        System.out.println(theResult);
        Assert.assertEquals("0a123.045.0true", theResult);

    }
}
