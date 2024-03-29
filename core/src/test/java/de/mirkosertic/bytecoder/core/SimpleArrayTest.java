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
public class SimpleArrayTest {

    @Test
    public void testNewPrimitiveAndLength() {
        int[] theArray = new int[100];
        theArray[0] = 10;
        int theSize = theArray.length;
        int theValue = theArray[10];
    }

    @Test
    public void testNewPrimitiveAndLengthAssert() {
        int[] theArray = new int[100];
        Assert.assertEquals(100, theArray.length, 0);
    }

    @Test
    public void testNewObjectAndLength() {
        Object[] theArray = new Object[100];
        int theSize = theArray.length;
    }
}
