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

import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(UnitTestRunner.class)
public class LongMathTest {

    public static long add(long a, long b) {
        return a + b;
    }

    public static long mul(long a, long b) {
        return a * b;
    }

    public static long div(long a, long b) {
        return a / b;
    }

    public static long sub(long a, long b) {
        return a - b;
    }

    public static long sleft(long aValue, int aOtherValue) {
        return aValue >>> aOtherValue;
    }

    public static long sright(long aValue, int aOtherValue) {
        return aValue << aOtherValue;
    }

    @Test
    public void testAdd() {
        float theResult = add(10L, 20L);
        Assert.assertEquals(30, theResult, 0);
    }

    @Test
    public void testMul() {
        float theResult = mul(10L, 20L);
        Assert.assertEquals(200, theResult, 0);
    }

    @Test
    public void testSub() {
        float theResult = sub(10L, 5);
        Assert.assertEquals(5, theResult, 0);
    }

    @Test
    public void testDiv() {
        float theResult = div(30, 10);
        Assert.assertEquals(3, theResult, 0);
    }

    @Test
    public void testShiftRight() {
        int theResult = (int) sleft(1000L, 1);
        Assert.assertEquals(500, theResult, 0);
    }

    @Test
    public void testShiftLeft() {
        int theResult = (int) sright(1000L, 1);
        Assert.assertEquals(2000, theResult, 0);
    }

}
