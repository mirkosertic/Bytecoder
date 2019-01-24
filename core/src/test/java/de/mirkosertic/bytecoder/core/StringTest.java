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
package de.mirkosertic.bytecoder.core;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
public class StringTest {

    public static String getString() {
        return "123";
    }

    @Test
    public void testLength() {
        final String theTest = getString();
        Assert.assertEquals(3, theTest.length(), 0);
    }

    @Test
    public void testStringArrayClone() {
        final String theNewString = new String(getString().getBytes());
        Assert.assertEquals(3, theNewString.length(), 0);
    }

    @Test
    public void testConcatenation() {
        final String theTest = getString();
        final String theTest2 = theTest + "456";
        final int theLength = theTest2.length();
        Assert.assertEquals(6, theTest2.length(), 0);
    }

    @Test
    public void testEquals() {
        final String theString1  = getString() + "456";
        final String theString2  = getString() + "456";
        Assert.assertEquals(theString1, theString2);
    }

    @Test
    public void testStringBuilder() {
        final StringBuilder theBuilder = new StringBuilder();
        final String theResult = theBuilder.toString();
        Assert.assertEquals(theResult.length(), 0, 0);
    }

    @Test
    public void testStringBuilderAppend() {
        final StringBuilder theBuilder = new StringBuilder();
        theBuilder.append(new String("123".getBytes()));
        final String theResult = theBuilder.toString();
        Assert.assertEquals(theResult.length(), 3, 0);
    }

    @Test
    public void testTrim() {
        Assert.assertEquals("test", "test".trim());
        Assert.assertEquals("test", " test".trim());
        Assert.assertEquals("test", " test ".trim());
        Assert.assertEquals("test", "test ".trim());
        Assert.assertEquals("1", "1 ".trim());
        Assert.assertEquals("1", " 1".trim());
        Assert.assertEquals("1", "  1 ".trim());
        Assert.assertEquals(1, "  1 ".trim().length(), 0);
    }

    @Test
    public void testHashCode() {
        final int hashCode = "TOP_LEFT".hashCode();
        Assert.assertEquals(-1.54073903E8, hashCode, 0);
    }

    @Test
    public void testStringSwitch() {
        int x;
        final String test = new String("TOP_LEFT".getBytes());
        switch (test) {
            case "LALA": {
                x = 20;
                break;
            }
            case "TOP_LEFT": {
                 x = 20;
                 break;
            }
            default:
                x = 0;
                break;
        }
        Assert.assertEquals(x, 20);
    }
}
