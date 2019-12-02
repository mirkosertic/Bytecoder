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
        @BytecoderTestOption(backend = CompileTarget.BackendType.wasm, preferStackifier = true)
}, includeJVM = false)
public class MemoryManagerTest {

    static class A {
        Object b;
    }

    private static Object staticRef;

    private static void nothing() {
        final Object o = new Object();
    }

    private static void createStaticReference() {
        staticRef = new Object();
    }

    @Test
    public void testUsedByHeap() {
        MemoryManager.GC();
        final A a = new A();
        final Object b = new Object();
        Assert.assertFalse(MemoryManager.isUsedByHeapUserSpace(Address.ptrOf(a)));
        Assert.assertFalse(MemoryManager.isUsedByHeapUserSpace(Address.ptrOf(b)));

        a.b = b;

        MemoryManager.printObjectDebug(b);
        MemoryManager.printObjectDebug(a);

        Assert.assertFalse(MemoryManager.isUsedByHeapUserSpace(Address.ptrOf(a)));
        Assert.assertTrue(MemoryManager.isUsedByHeapUserSpace(Address.ptrOf(b)));
    }

    @Test
    public void testUsedByStack() {
        Assert.assertFalse(MemoryManager.isUsedByStackUserSpace(1000));
        final Object o = new Object();
        final int ptr = Address.ptrOf(o);
        Assert.assertTrue(MemoryManager.isUsedByStackUserSpace(ptr));
    }

    @Test
    public void testGCStackAllocation() {
        MemoryManager.GC();
        final long x = MemoryManager.freeMem();

        nothing();

        Assert.assertTrue(MemoryManager.freeMem() < x);

        MemoryManager.GC();

        Assert.assertEquals(x, MemoryManager.freeMem(), 0);
    }

    @Test
    public void testGCStaticRef() {
        MemoryManager.GC();
        final long x = MemoryManager.freeMem();

        createStaticReference();

        final long freeMem = MemoryManager.freeMem();
        Assert.assertTrue(freeMem < x);

        MemoryManager.GC();

        // Nothing is freed, as the reference is still statically referenced
        Assert.assertEquals(freeMem, MemoryManager.freeMem(), 0);
    }

    @Test
    public void testGC() {
        MemoryManager.GC();

        final long x = MemoryManager.freeMem();

        final long l = MemoryManager.malloc(100);
        Assert.assertEquals(x - 112, MemoryManager.freeMem(), 0);

        MemoryManager.GC();

        Assert.assertEquals(x, MemoryManager.freeMem(), 0);
    }

    public static int createAndReturnObjectPt() {
        // Increment the pointer by one to make the reference invisible on
        // the stack for the GC, and the object is garbage-collectible.
        return Address.ptrOf(new Object()) + 1;
    }

    @Test
    public void testNotUsedByStackOrHeap() {
        MemoryManager.GC();
        final long theFreeMem = MemoryManager.freeMem();
        final int ptr = createAndReturnObjectPt();
        MemoryManager.GC();
        Assert.assertEquals(theFreeMem, MemoryManager.freeMem(), 0);
        Assert.assertFalse(MemoryManager.isUsedByHeapUserSpace(ptr - 1));
        Assert.assertFalse(MemoryManager.isUsedByStackUserSpace( ptr - 1));
        Assert.assertEquals(-1, MemoryManager.indexInAllocationList(ptr - 1), 0);
        Assert.assertTrue(MemoryManager.indexInFreeList(ptr -1) >= 0);
    }
}
