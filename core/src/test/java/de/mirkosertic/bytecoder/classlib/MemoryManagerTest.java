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
package de.mirkosertic.bytecoder.classlib;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;

@RunWith(BytecoderUnitTestRunner.class)
public class MemoryManagerTest {

    @Test
    public void testAllFreeNothingUsed() {
        MemoryManager.initWithSize(10000);
        Assert.assertEquals(10000, MemoryManager.freeMem(), 0);
        Assert.assertEquals(0, MemoryManager.usedMem(), 0);
    }

    @Test
    public void testMallocAndFree() {
        MemoryManager.initWithSize(10000);
        Assert.assertEquals(10000, MemoryManager.freeMem(), 0);
        Assert.assertEquals(0, MemoryManager.usedMem(), 0);

        Address theAllocated = MemoryManager.malloc(100);
        Assert.assertEquals(9888, MemoryManager.freeMem(), 0);
        Assert.assertEquals(112, MemoryManager.usedMem(), 0);

        Assert.assertEquals(4, Address.getStart(theAllocated), 0);

        Address theAllocated2 = MemoryManager.malloc(25);
        Assert.assertEquals(9851, MemoryManager.freeMem(), 0);
        Assert.assertEquals(149, MemoryManager.usedMem(), 0);

        Assert.assertEquals(116, Address.getStart(theAllocated2), 0);

        MemoryManager.free(theAllocated);
        Assert.assertEquals(9963, MemoryManager.freeMem(), 0);
        Assert.assertEquals(37, MemoryManager.usedMem(), 0);

        MemoryManager.free(theAllocated2);
        Assert.assertEquals(10000, MemoryManager.freeMem(), 0);
        Assert.assertEquals(0, MemoryManager.usedMem(), 0);
    }

    @Test
    public void testMallocAndFreeSmall() {
        MemoryManager.initWithSize(10000);
        Assert.assertEquals(10000, MemoryManager.freeMem(), 0);
        Assert.assertEquals(0, MemoryManager.usedMem(), 0);

        Address theAllocated = MemoryManager.malloc(8);
        Assert.assertEquals(9980, MemoryManager.freeMem(), 0);
        Assert.assertEquals(20, MemoryManager.usedMem(), 0);
    }
}