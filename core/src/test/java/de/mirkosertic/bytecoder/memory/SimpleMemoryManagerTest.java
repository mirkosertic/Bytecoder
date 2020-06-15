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
        assertEquals(12, ptr);
        assertEquals(888, mm.freeMemory());
        assertEquals(112, mm.allocated());

        assertEquals(124, mm.malloc(200));
        assertEquals(336, mm.malloc(300));

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

        assertEquals(688, mm.freeMemory());
        assertEquals(312, mm.allocated());

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

        assertEquals(688, mm.freeMemory());
        assertEquals(312, mm.allocated());

        final int d = mm.malloc(200);

        mm.printDebug(System.out);

        assertEquals(124, d);
    }

    @Test
    public void testMallocFreeMallocMiddleOfFreeList() {
        final SimpleMemoryManager mm = new SimpleMemoryManager(1000);
        final int a = mm.malloc(100);
        final int b = mm.malloc(200);
        final int c = mm.malloc(300);
        mm.free(b);
        mm.free(a);

        assertEquals(688, mm.freeMemory());
        assertEquals(312, mm.allocated());

        final int d = mm.malloc(200);

        mm.printDebug(System.out);

        assertEquals(124, d);
    }

    @Test
    public void testMallocFreeMallocEndOfFreeList() {
        final SimpleMemoryManager mm = new SimpleMemoryManager(1000);
        final int a = mm.malloc(100);
        final int b = mm.malloc(200);
        final int c = mm.malloc(300);
        mm.free(a);
        mm.free(b);

        assertEquals(688, mm.freeMemory());
        assertEquals(312, mm.allocated());

        final int d = mm.malloc(352);

        mm.printDebug(System.out);

        assertEquals(648, d);
    }

    @Test
    public void testSplitBeginningOfFreeList() {
        final SimpleMemoryManager mm = new SimpleMemoryManager(1000);
        final int a = mm.malloc(100);
        final int b = mm.malloc(200);
        final int c = mm.malloc(300);
        mm.free(b);
        mm.free(a);

        assertEquals(688, mm.freeMemory());
        assertEquals(312, mm.allocated());

        final int d = mm.malloc(50);

        mm.printDebug(System.out);

        assertEquals(12, d);
    }

    @Test
    public void testSplitMiddleOfFreeList() {
        final SimpleMemoryManager mm = new SimpleMemoryManager(1000);
        final int a = mm.malloc(100);
        final int b = mm.malloc(200);
        final int c = mm.malloc(300);
        mm.free(b);
        mm.free(a);

        assertEquals(688, mm.freeMemory());
        assertEquals(312, mm.allocated());

        final int d = mm.malloc(150);

        mm.printDebug(System.out);

        assertEquals(124, d);
    }

    @Test
    public void testSplitEndOfFreeList() {
        final SimpleMemoryManager mm = new SimpleMemoryManager(1000);
        final int a = mm.malloc(100);
        final int b = mm.malloc(200);
        final int c = mm.malloc(300);
        mm.free(b);
        mm.free(a);

        assertEquals(688, mm.freeMemory());
        assertEquals(312, mm.allocated());

        final int d = mm.malloc(300);

        mm.printDebug(System.out);

        assertEquals(648, d);
    }
}