/*
 * Copyright 2018 Mirko Sertic
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

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
public class MathTest {

    @Test
    public void testMin() {
        Assert.assertEquals(10, Math.min(20, 10));
    }

    @Test
    public void testMax() {
        Assert.assertEquals(20, Math.max(20, 10));
    }

    @Test
    public void testLog() {
        final double l = Math.log(1234.0);
    }

    @Test
    public void testFloorModInt() {
        Math.floorMod(10, 10);
    }

    @Test
    public void testFloorModLong() {
        Math.floorMod(10L, 10L);
    }

    @Test
    public void testAddExactLong() {
        Assert.assertEquals(42L, Math.addExact(21L, 21L));
    }

    @Test
    public void testAddExactInt() {
        Assert.assertEquals(42, Math.addExact(21, 21));
    }

    @Test
    public void testSqrt() {
        Assert.assertEquals(3d, Math.sqrt(9d), 0);
    }

    @Test
    public void testCbrt() {
        Assert.assertEquals(3d, Math.cbrt(27d), 0);
        Assert.assertEquals(-3d, Math.cbrt(-27d), 0);
    }
}