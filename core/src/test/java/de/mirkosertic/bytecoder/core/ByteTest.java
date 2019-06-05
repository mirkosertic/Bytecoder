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

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;

@RunWith(BytecoderUnitTestRunner.class)
public class ByteTest {

    @Test
    public void testAssignment() {
        final int b = -200;
        final byte c = (byte) b;
        Assert.assertEquals(c, 56);
    }

    @Test
    public void testNegative() {
        final byte b = -30;
        Assert.assertEquals(b, -30, 0);
    }

    @Test
    public void testAssignmentByteArray() {
        final int b = -200;
        final byte[] c = new byte[1];
        c[0] = (byte) b;
        Assert.assertEquals(c[0], 56);
    }
}
