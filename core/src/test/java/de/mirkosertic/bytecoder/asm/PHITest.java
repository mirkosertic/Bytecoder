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
public class PHITest {

    private static float floatValue(Float aValue) {
        return aValue.floatValue();
    }

    private static Float floatValueObject(Float aValue) {
        return aValue;
    }

    @Test
    public void testFloatFromComputationBoxing() {
        int x = 10;
        float theValue;
        if (x > 10) {
            theValue = floatValueObject(-10f);
        } else {
            theValue = floatValueObject(-20f);
        }
        Assert.assertEquals(-20, theValue, 0);
    }

    @Test
    public void testFloatFromComputation() {
        int x = 10;
        float theValue;
        if (x > 10) {
            theValue = floatValue(-10f);
        } else {
            theValue = floatValue(-20f);
        }
        Assert.assertEquals(-20, theValue, 0);
    }

    @Test
    public void testFloat() {
        int x = 10;
        float theValue;
        if (x > 10) {
            theValue = -10;
        } else {
            theValue = -20;
        }
        Assert.assertEquals(-20, theValue, 0);
    }

    @Test
    public void testDouble() {
        int x = 10;
        double theValue;
        if (x > 10) {
            theValue = -10;
        } else {
            theValue = -20;
        }
        Assert.assertEquals(-20, theValue, 0);
    }

}
