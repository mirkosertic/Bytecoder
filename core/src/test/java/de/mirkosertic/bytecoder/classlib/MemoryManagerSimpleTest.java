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

import de.mirkosertic.bytecoder.backend.CompileTarget;
import de.mirkosertic.bytecoder.unittest.BytecoderTestOption;
import de.mirkosertic.bytecoder.unittest.BytecoderTestOptions;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
@BytecoderTestOptions(value = {
        @BytecoderTestOption(backend = CompileTarget.BackendType.js)
}, includeJVM = false)
public class MemoryManagerSimpleTest {

    @Test
    public void testInit() {
        MemoryManager.initNative();
        Assert.assertEquals(1000, MemoryManager.freeMem(), 0);
        Assert.assertEquals(0, MemoryManager.usedMem(), 0);
    }

    @Test
    public void testMalloc() {
        MemoryManager.initNative();
        Assert.assertEquals(1000, MemoryManager.freeMem(), 0);
        Assert.assertEquals(0, MemoryManager.usedMem(), 0);

        final int theMalloc = MemoryManager.malloc(100);
        Assert.assertEquals(36, theMalloc , 0);

        Assert.assertEquals(892, MemoryManager.freeMem(), 0);
        Assert.assertEquals(108, MemoryManager.usedMem(), 0);

        final int theMalloc2 = MemoryManager.malloc(150);
        Assert.assertEquals(144, theMalloc2 , 0);

        Assert.assertEquals(734, MemoryManager.freeMem(), 0);
        Assert.assertEquals(266, MemoryManager.usedMem(), 0);
    }

    @Test
    public void testMallocFree() {
        MemoryManager.initNative();
        Assert.assertEquals(1000, MemoryManager.freeMem(), 0);
        Assert.assertEquals(0, MemoryManager.usedMem(), 0);

        int theMalloc = MemoryManager.malloc(100);
        Assert.assertEquals(36, theMalloc , 0);
        Assert.assertEquals(892, MemoryManager.freeMem(), 0);
        Assert.assertEquals(108, MemoryManager.usedMem(), 0);

        MemoryManager.free(theMalloc);
        Assert.assertEquals(1000, MemoryManager.freeMem(), 0);
        Assert.assertEquals(0, MemoryManager.usedMem(), 0);

        theMalloc = MemoryManager.malloc(100);
        Assert.assertEquals(36, theMalloc , 0);
        Assert.assertEquals(892, MemoryManager.freeMem(), 0);
        Assert.assertEquals(108, MemoryManager.usedMem(), 0);
    }

    @Test
    public void testMallocGC() {
        MemoryManager.initNative();
        Assert.assertEquals(1000, MemoryManager.freeMem(), 0);
        Assert.assertEquals(0, MemoryManager.usedMem(), 0);

        final int theMalloc1 = MemoryManager.malloc(100);
        final int theMalloc2 = MemoryManager.malloc(200);

        Assert.assertEquals(684, MemoryManager.freeMem(), 0);
        Assert.assertEquals(316, MemoryManager.usedMem(), 0);

        MemoryManager.GC();

        Assert.assertEquals(1000, MemoryManager.freeMem(), 0);
        Assert.assertEquals(0, MemoryManager.usedMem(), 0);
    }

    @Test
    public void testMallocGCPartial() {
        MemoryManager.initNative();
        Assert.assertEquals(1000, MemoryManager.freeMem(), 0);
        Assert.assertEquals(0, MemoryManager.usedMem(), 0);

        final int theMalloc1 = MemoryManager.malloc(100);
        final int theMalloc2 = MemoryManager.malloc(200);
        Address.setIntValue(theMalloc1, 0, theMalloc2);

        // Malloc1 references malloc2, but it is not references
        // Malloc2 will be GCed first, then Malloc2

        Assert.assertEquals(684, MemoryManager.freeMem(), 0);
        Assert.assertEquals(316, MemoryManager.usedMem(), 0);

        MemoryManager.GC();

        Assert.assertEquals(792, MemoryManager.freeMem(), 0);
        Assert.assertEquals(208, MemoryManager.usedMem(), 0);
   }
}
