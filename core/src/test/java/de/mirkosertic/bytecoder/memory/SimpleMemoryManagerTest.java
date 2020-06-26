/*
 * Copyright 2020 Mirko Sertic
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
package de.mirkosertic.bytecoder.memory;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

public class SimpleMemoryManagerTest {

    @Test
    public void testInitialFree() {
        final SimpleMemoryManager mm = new SimpleMemoryManager(1000);
        assertEquals(1000, mm.freeMemory());
        assertEquals(0, mm.allocated());
    }

    @Test
    public void testMalloc() {
        final SimpleMemoryManager mm = new SimpleMemoryManager(1000);
        final int ptr = mm.malloc(100);
        assertEquals(16, ptr);
        assertEquals(884, mm.freeMemory());
        assertEquals(116, mm.allocated());

        assertEquals(132, mm.malloc(200));
        assertEquals(348, mm.malloc(300));

        mm.printDebug(System.out);
    }

    @Test
    public void testMallocFree() {
        final SimpleMemoryManager mm = new SimpleMemoryManager(1000);
        final int a = mm.malloc(100);
        final int b = mm.malloc(200);
        final int c = mm.malloc(300);
        mm.free(a);
        mm.free(b);

        assertEquals(684, mm.freeMemory());
        assertEquals(316, mm.allocated());

        mm.printDebug(System.out);
    }

    @Test
    public void testMallocFreeMallocHeadOfFreeList() {
        final SimpleMemoryManager mm = new SimpleMemoryManager(1000);
        final int a = mm.malloc(100);
        final int b = mm.malloc(200);
        final int c = mm.malloc(300);
        mm.free(a);
        mm.free(b);

        assertEquals(684, mm.freeMemory());
        assertEquals(316, mm.allocated());

        final int d = mm.malloc(200);

        mm.printDebug(System.out);

        assertEquals(132, d);
    }

    @Test
    public void testMallocFreeMallocMiddleOfFreeList() {
        final SimpleMemoryManager mm = new SimpleMemoryManager(1000);
        final int a = mm.malloc(100);
        final int b = mm.malloc(200);
        final int c = mm.malloc(300);
        mm.free(b);
        mm.free(a);

        assertEquals(684, mm.freeMemory());
        assertEquals(316, mm.allocated());

        final int d = mm.malloc(200);

        mm.printDebug(System.out);

        assertEquals(132, d);
    }

    @Test
    public void testMallocFreeMallocEndOfFreeList() {
        final SimpleMemoryManager mm = new SimpleMemoryManager(1000);
        final int a = mm.malloc(100);
        final int b = mm.malloc(200);
        final int c = mm.malloc(300);
        mm.free(a);
        mm.free(b);

        assertEquals(684, mm.freeMemory());
        assertEquals(316, mm.allocated());

        final int d = mm.malloc(352 - 16);

        mm.printDebug(System.out);

        assertEquals(664, d);
    }

    @Test
    public void testSplitBeginningOfFreeList() {
        final SimpleMemoryManager mm = new SimpleMemoryManager(1000);
        final int a = mm.malloc(100);
        final int b = mm.malloc(200);
        final int c = mm.malloc(300);
        mm.free(b);
        mm.free(a);

        assertEquals(684, mm.freeMemory());
        assertEquals(316, mm.allocated());

        final int d = mm.malloc(50);

        mm.printDebug(System.out);

        assertEquals(16, d);
    }

    @Test
    public void testSplitMiddleOfFreeList() {
        final SimpleMemoryManager mm = new SimpleMemoryManager(1000);
        final int a = mm.malloc(100);
        final int b = mm.malloc(200);
        final int c = mm.malloc(300);
        mm.free(b);
        mm.free(a);

        assertEquals(684, mm.freeMemory());
        assertEquals(316, mm.allocated());

        final int d = mm.malloc(150);

        mm.printDebug(System.out);

        assertEquals(132, d);
    }

    @Test
    public void testSplitEndOfFreeList() {
        final SimpleMemoryManager mm = new SimpleMemoryManager(1000);
        final int a = mm.malloc(100);
        final int b = mm.malloc(200);
        final int c = mm.malloc(300);
        mm.free(b);
        mm.free(a);

        assertEquals(684, mm.freeMemory());
        assertEquals(316, mm.allocated());

        final int d = mm.malloc(300);

        mm.printDebug(System.out);

        assertEquals(664, d);
    }

    @Test
    public void testGC() {

        final SimpleMemoryManager mm = new SimpleMemoryManager(1000);
        final int a = mm.malloc(100);
        final int b = mm.malloc(200);
        final int c = mm.malloc(300);

        mm.GC();

        mm.printDebug(System.out);

        assertEquals(1000, mm.freeMemory());
        assertEquals(0, mm.allocated());
    }

    @Test
    public void testGCMalloc() {

        final SimpleMemoryManager mm = new SimpleMemoryManager(1000);
        final int a = mm.malloc(100);
        final int b = mm.malloc(200);
        final int c = mm.malloc(300);

        mm.GC();

        int x = mm.malloc(80);

        Assert.assertEquals(16, x);

        assertEquals(904, mm.freeMemory());
        assertEquals(96, mm.allocated());

        int y = mm.malloc(4);

        mm.printDebug(System.out);

        Assert.assertEquals(112, y);

        assertEquals(884, mm.freeMemory());
        assertEquals(116, mm.allocated());

    }
}