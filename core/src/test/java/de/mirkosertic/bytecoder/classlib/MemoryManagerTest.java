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

    @Test
    public void testUsedByStack() {
        Assert.assertFalse(MemoryManager.isUsedByStackUserSpace(1000));
        final Object o = new Object();
        final int ptr = Address.ptrOf(o);
        Assert.assertTrue(MemoryManager.isUsedByStackUserSpace(ptr));
    }

    @Test
    public void testGC() {
        MemoryManager.GC();

        final long x = MemoryManager.freeMem();

        final long l = MemoryManager.malloc(100);
        Assert.assertEquals(x - 108, MemoryManager.freeMem(), 0);

        MemoryManager.GC();

        Assert.assertEquals(x, MemoryManager.freeMem(), 0);
    }

}
