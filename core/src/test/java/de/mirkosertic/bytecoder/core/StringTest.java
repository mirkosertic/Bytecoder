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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mirkosertic.bytecoder.classlib.java.lang.TString;
import de.mirkosertic.bytecoder.classlib.java.lang.TStringBuilder;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;

@RunWith(BytecoderUnitTestRunner.class)
public class StringTest {

    public static String getString() {
        return "123";
    }

    @Test
    public void testLength() {
        String theTest = getString();
        Assert.assertEquals(3, theTest.length(), 0);
    }

    @Test
    public void testStringArrayClone() {
        String theNewString = new String(getString().getBytes());
        Assert.assertEquals(3, theNewString.length(), 0);
    }

    @Test
    public void testConcatenation() {
        String theTest = getString();
        String theTest2 = theTest + "456";
        Assert.assertEquals(6, theTest2.length(), 0);
    }

    @Test
    public void testEquals() {
        String theString1  = getString() + "456";
        String theString2  = getString() + "456";
        Assert.assertEquals(theString1, theString2);
    }

    @Test
    public void testStringBuilder() {
        TStringBuilder theBuilder = new TStringBuilder();
        String theResult = theBuilder.toString();
        Assert.assertEquals(theResult.length(), 0, 0);
    }

    public void testStringBuilderAppend() {
        TStringBuilder theBuilder = new TStringBuilder();
        theBuilder.append(new TString("123".getBytes()));
        String theResult = theBuilder.toString();
        Assert.assertEquals(theResult.length(), 3, 0);
    }
}
