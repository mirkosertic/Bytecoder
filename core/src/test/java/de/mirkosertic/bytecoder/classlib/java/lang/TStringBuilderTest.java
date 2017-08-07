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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;

@RunWith(BytecoderUnitTestRunner.class)
public class TStringBuilderTest {

    @Test
    public void testEmpty() {
        StringBuilder theBuilder = new StringBuilder();
        assertEquals(0, theBuilder.length(), 0);
    }

    @Test
    public void testAppendString() {
        StringBuilder theBuilder = new StringBuilder();
        theBuilder.append("Hello");
        assertEquals(5, theBuilder.length(), 0);
    }

    @Test
    public void testAppendObject() {
        StringBuilder theBuilder = new StringBuilder();
        Object theValue = new Integer(10);
        theBuilder.append(theValue);
        assertEquals(2, theBuilder.length(), 0);
    }

    @Test
    public void testAppendInteger() {
        TStringBuilder theBuilder = new TStringBuilder();
        theBuilder.append(123);
        assertEquals(3, theBuilder.length(), 0);
    }

    @Test
    public void testAppendNegativeInteger() {
        TStringBuilder theBuilder = new TStringBuilder();
        theBuilder.append(-123);
        assertEquals(4, theBuilder.length(), 0);
    }

    @Test
    public void testAppendFloat() {
        TStringBuilder theBuilder = new TStringBuilder();
        theBuilder.append(123.123f);
        assertEquals("123.123", theBuilder.toString());
    }

    @Test
    public void testAppendNegativeFloat() {
        TStringBuilder theBuilder = new TStringBuilder();
        theBuilder.append(-123.123f);
        assertEquals("-123.123", theBuilder.toString());
    }

    @Test
    public void testAppendDouble() {
        TStringBuilder theBuilder = new TStringBuilder();
        theBuilder.append(123.123d);
        assertEquals("123.123", theBuilder.toString());
    }

    @Test
    public void testAppendNegativeDouble() {
        TStringBuilder theBuilder = new TStringBuilder();
        theBuilder.append(-123.123d);
        assertEquals("-123.123", theBuilder.toString());
    }
}