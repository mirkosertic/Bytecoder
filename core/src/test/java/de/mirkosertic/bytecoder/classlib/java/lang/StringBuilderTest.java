/*
 * Copyright 2017 Mirko Sertic
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

import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(UnitTestRunner.class)
public class StringBuilderTest {

    @Test
    public void testEmpty() {
        final StringBuilder theBuilder = new StringBuilder();
        assertEquals(0, theBuilder.length(), 0);
    }

    @Test
    public void testAppendString() {
        final StringBuilder theBuilder = new StringBuilder();
        theBuilder.append("Hello");
        assertEquals(5, theBuilder.length(), 0);
    }

    @Test
    public void testAppendObject() {
        final StringBuilder theBuilder = new StringBuilder();
        final Object theValue = new Integer(10);
        theBuilder.append(theValue);
        assertEquals(2, theBuilder.length(), 0);
    }

    @Test
    public void testAppendInteger() {
        final StringBuilder theBuilder = new StringBuilder();
        theBuilder.append(123);
        assertEquals(3, theBuilder.length(), 0);
    }

    @Test
    public void testAppendNegativeInteger() {
        final StringBuilder theBuilder = new StringBuilder();
        theBuilder.append(-123);
        assertEquals(4, theBuilder.length(), 0);
    }

    @Test
    public void testAppendFloat() {
        final StringBuilder theBuilder = new StringBuilder();
        theBuilder.append(123.0f);
        assertEquals("123.0", theBuilder.toString());
    }

    @Test
    public void testAppendNegativeFloat() {
        final StringBuilder theBuilder = new StringBuilder();
        theBuilder.append(-123.0f);
        assertEquals("-123.0", theBuilder.toString());
    }

    @Test
    public void testAppendDouble() {
        final StringBuilder theBuilder = new StringBuilder();
        theBuilder.append(123.123d);
        assertEquals("123.123", theBuilder.toString());
    }

    @Test
    public void testAppendNegativeDouble() {
        final StringBuilder theBuilder = new StringBuilder();
        theBuilder.append(-123.123d);
        assertEquals("-123.123", theBuilder.toString());
    }

    @Test
    public void testAppendChar() {
        final StringBuilder theBuilder = new StringBuilder();
        theBuilder.append('1');
        assertEquals("1", theBuilder.toString());
        theBuilder.append("");
        assertEquals("1", theBuilder.toString());
    }

    @Test
    public void testUnicode() {
        final StringBuilder theBuilder = new StringBuilder("M");
        theBuilder.append('ü');
        theBuilder.append("nster");
        final String theResult = theBuilder.toString();
        Assert.assertEquals("Münster", theResult);
    }

    @Test
    public void testCapacity() {
        final StringBuilder theBuilder = new StringBuilder(10);
        Assert.assertEquals("", theBuilder.toString());
    }

    @Test
    public void testDeleteCharAt() {
        StringBuilder builder = new StringBuilder("ABC");
        builder = builder.deleteCharAt(0);
        Assert.assertEquals("BC", builder.toString());
    }

    @Test
    public void testSetCharAt() {
        final StringBuilder builder = new StringBuilder("ABC");
        builder.setCharAt(0, 'E');
        Assert.assertEquals("EBC", builder.toString());
    }

    @Test
    public void testInsert() {
        StringBuilder builder = new StringBuilder("ABC");
        builder = builder.insert(0, 'I');
        Assert.assertEquals("IABC", builder.toString());
    }

    @Test
    public void testCharAt() {
        final StringBuilder builder = new StringBuilder("ABC");
        Assert.assertEquals('A', builder.charAt(0));
    }
}
