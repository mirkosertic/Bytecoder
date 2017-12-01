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

        Assert.assertEquals(16, Address.getStart(theAllocated), 0);

        Address theAllocated2 = MemoryManager.malloc(25);
        Assert.assertEquals(9851, MemoryManager.freeMem(), 0);
        Assert.assertEquals(149, MemoryManager.usedMem(), 0);

        Assert.assertEquals(128, Address.getStart(theAllocated2), 0);

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

    @Test
    public void testUsed() {
        MemoryManager.initWithSize(10000);
        Address theAllocated1 = MemoryManager.malloc(100);
        Address theAllocated2 = MemoryManager.malloc(100);
        Address.setIntValue(theAllocated2, 0, Address.getStart(theAllocated1));

        Assert.assertEquals(9776, MemoryManager.freeMem(), 0);
        Assert.assertEquals(224, MemoryManager.usedMem(), 0);

        MemoryManager.GC();

        Assert.assertEquals(9888, MemoryManager.freeMem(), 0);
        Assert.assertEquals(112, MemoryManager.usedMem(), 0);

        MemoryManager.GC();

        Assert.assertEquals(10000, MemoryManager.freeMem(), 0);
        Assert.assertEquals(0, MemoryManager.usedMem(), 0);
    }

    @Test
    public void testAllocateFlow() {
        MemoryManager.initWithSize(80000);

        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(32);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        Assert.assertEquals(79636, MemoryManager.freeMem(), 0);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(36);
        MemoryManager.malloc(28);
        MemoryManager.malloc(32);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(32);
        MemoryManager.malloc(28);
        MemoryManager.malloc(32);
        MemoryManager.malloc(32);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(32);
        Assert.assertEquals(78888, MemoryManager.freeMem(), 0);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(32);
        MemoryManager.malloc(28);
        MemoryManager.malloc(48);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(32);
        MemoryManager.malloc(32);
        MemoryManager.malloc(32);
        MemoryManager.malloc(28);
        MemoryManager.malloc(32);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(32);
        MemoryManager.malloc(32);
        MemoryManager.malloc(36);
        MemoryManager.malloc(28);
        MemoryManager.malloc(32);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(32);
        MemoryManager.malloc(44);
        MemoryManager.malloc(32);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(32);
        MemoryManager.malloc(68);
        MemoryManager.malloc(32);
        MemoryManager.malloc(32);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(32);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(32);
        MemoryManager.malloc(32);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        Assert.assertEquals(76740, MemoryManager.freeMem(), 0);
        MemoryManager.malloc(32);
        MemoryManager.malloc(36);
        MemoryManager.malloc(28);
        MemoryManager.malloc(32);
        MemoryManager.malloc(32);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(52);
        MemoryManager.malloc(28);
        MemoryManager.malloc(24);
        MemoryManager.malloc(20);
        MemoryManager.malloc(24);
        MemoryManager.malloc(20);
        MemoryManager.malloc(24);
        MemoryManager.malloc(20);
        MemoryManager.malloc(24);
        MemoryManager.malloc(20);
        MemoryManager.malloc(24);
        MemoryManager.malloc(20);
        MemoryManager.malloc(24);
        MemoryManager.malloc(40);
        MemoryManager.malloc(24);
        MemoryManager.malloc(32);
        MemoryManager.malloc(24);
        MemoryManager.malloc(44);
        MemoryManager.malloc(24);
        MemoryManager.malloc(36);
        MemoryManager.malloc(20);
        MemoryManager.malloc(20);
        MemoryManager.malloc(24);
        MemoryManager.malloc(24);
        MemoryManager.malloc(24);
        MemoryManager.malloc(16);
        MemoryManager.malloc(32);
        MemoryManager.malloc(32);
        MemoryManager.malloc(32);
        MemoryManager.malloc(32);
        MemoryManager.malloc(32);
        MemoryManager.malloc(32);
        MemoryManager.malloc(32);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(32);
        MemoryManager.malloc(28);
        MemoryManager.malloc(28);
        MemoryManager.malloc(32);
        MemoryManager.malloc(20);
        MemoryManager.malloc(20);
        MemoryManager.malloc(20);
        MemoryManager.malloc(24);
        MemoryManager.malloc(16);
        MemoryManager.malloc(100);
        Assert.assertEquals(74504, MemoryManager.freeMem(), 0);
        MemoryManager.malloc(24);
        MemoryManager.malloc(28);
        MemoryManager.malloc(56);
        MemoryManager.malloc(24);
        MemoryManager.malloc(28);
        MemoryManager.malloc(56);
        MemoryManager.malloc(24);
        MemoryManager.malloc(28);
        MemoryManager.malloc(56);
        MemoryManager.malloc(24);
        MemoryManager.malloc(10);
        MemoryManager.malloc(20);
        Assert.assertEquals(73982, MemoryManager.freeMem(), 0);


        MemoryManager.malloc(10);
        Assert.assertEquals(73960, MemoryManager.freeMem(), 0);

        MemoryManager.malloc(64);
    }

    @Test
    public void testMallocLarge() {
        MemoryManager.initWithSize(80000);

        MemoryManager.malloc(14484);
        Assert.assertEquals(65504, MemoryManager.freeMem(), 0);

        MemoryManager.malloc(80);
        Assert.assertEquals(65412, MemoryManager.freeMem(), 0);

    }
}