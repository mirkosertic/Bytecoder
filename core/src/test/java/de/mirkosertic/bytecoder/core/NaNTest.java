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
package de.mirkosertic.bytecoder.core;

import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(UnitTestRunner.class)
public class NaNTest {

    @Test
    public void testIsNaNDouble() {
        Assert.assertTrue(Double.isNaN(Double.NaN));
        Assert.assertFalse(Double.isNaN(10d));
    }

    @Test
    public void testIsNaNFloat() {
        Assert.assertTrue(Float.isNaN(Float.NaN));
        Assert.assertFalse(Float.isNaN(10f));
    }

    @Test
    public void testCastDouble() {
        final double d = Double.NaN;
        final int x = (int) d;
        Assert.assertEquals(0, x, 0);
    }

    @Test
    public void testCastFloat() {
        final float f = Float.NaN;
        final int x = (int) f;
        Assert.assertEquals(0, x, 0);
    }
}
