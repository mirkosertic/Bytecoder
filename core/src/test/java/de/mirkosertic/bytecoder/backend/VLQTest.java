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
package de.mirkosertic.bytecoder.backend;

import org.junit.Assert;
import org.junit.Test;

public class VLQTest {

    @Test
    public void testDecode() {
        final int[] theResult = VLQ.decode("IAAM");
        Assert.assertEquals(4, theResult.length);
        Assert.assertEquals(4, theResult[0]);
        Assert.assertEquals(0, theResult[1]);
        Assert.assertEquals(0, theResult[2]);
        Assert.assertEquals(6, theResult[3]);
    }

    @Test
    public void testEncode() {
        Assert.assertEquals("IAAM", VLQ.encode(new int[] {4,0,0,6}));
    }
}