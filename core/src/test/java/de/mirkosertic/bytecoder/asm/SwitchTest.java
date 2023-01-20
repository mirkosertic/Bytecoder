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
public class SwitchTest {

    public static int compute(int aValue) {
        int theValue = 0;
        switch (aValue) {
        case 1:
            theValue = 1;
            break;
        case 2:
            theValue = 2;
            break;
        case 3:
            theValue = 3;
            break;
        case 4:
            theValue = 4;
            break;
        case 5:
            theValue = 5;
            break;
        default:
            return 99;
        }
        return theValue;
    }

    @Test
    public void testSwitch() {
        Assert.assertEquals(99, compute(9999), 0);
        Assert.assertEquals(1, compute(1), 0);
        Assert.assertEquals(2, compute(2), 0);
        Assert.assertEquals(3, compute(3), 0);
        Assert.assertEquals(4, compute(4), 0);
        Assert.assertEquals(5, compute(5), 0);
    }
}
