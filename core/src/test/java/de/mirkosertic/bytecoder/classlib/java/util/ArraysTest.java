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
package de.mirkosertic.bytecoder.classlib.java.util;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(BytecoderUnitTestRunner.class)
public class ArraysTest {

    @Test
    public void testObjectSort() {
        final Integer[] array = new Integer[] {10, 2, 8, 9, 1};
        Arrays.sort(array);

        Assert.assertEquals(1, array[0].intValue(), 0);
        Assert.assertEquals(2, array[1].intValue(), 0);
        Assert.assertEquals(8, array[2].intValue(), 0);
        Assert.assertEquals(9, array[3].intValue(), 0);
        Assert.assertEquals(10, array[4].intValue(), 0);
    }
}
