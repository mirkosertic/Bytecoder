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
package de.mirkosertic.bytecoder.asm;

import de.mirkosertic.bytecoder.asm.test.UnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(UnitTestRunner.class)
public class RoundingTest {

    @Test
    public void testRoundDown() {
        float f = 1.6f;
        int k = (int) f;
        Assert.assertEquals(1f, k, 0);
    }

    @Test
    public void testFloatRemainder() {
        float f = 100f;
        float k = 26f;
        Assert.assertEquals(22f, f % k, 0);
    }

    @Test
    public void testIntRemainder() {
        int f = 100;
        int k = 44;
        Assert.assertEquals(12f, f % k, 0);
    }

    @Test
    public void testDiv() {
        int theResult = 100 / 66;
        Assert.assertEquals(1, theResult, 0);
    }

    @Test
    public void testNegDiv() {
        int theResult = -100 / 66;
        Assert.assertEquals(-1, theResult, 0);
    }
}
