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
public class GCTest {

    @Test
    public void testMalloc() {
        GC.initWithSize(10000);
        Assert.assertEquals(0, GC.usedMem(), 0);
        Assert.assertEquals(10000, GC.freeMem(), 0);
        Address theAlloc1 = GC.malloc(100);
        Assert.assertEquals(100, GC.usedMem(), 0);
        Assert.assertEquals(9900, GC.freeMem(), 0);
        Assert.assertEquals(0, theAlloc1.getStart(), 0);
        Address theAlloc2 = GC.malloc(50);
        Assert.assertEquals(100, theAlloc2.getStart(), 0);
        Assert.assertEquals(150, GC.usedMem(), 0);
        Assert.assertEquals(9850, GC.freeMem(), 0);
    }

    @Test
    public void testMallocFree() {
        GC.initWithSize(10000);
        Address thePointer = GC.malloc(100);
        GC.free(thePointer);
        Assert.assertEquals(10000, GC.freeMem(), 0);
        Assert.assertEquals(0, GC.usedMem(), 0);
        Address theNew = GC.malloc(50);
        Assert.assertEquals(0, theNew.getStart(), 0);
    }
}