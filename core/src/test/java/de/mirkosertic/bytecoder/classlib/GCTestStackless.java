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
package de.mirkosertic.bytecoder.classlib;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mirkosertic.bytecoder.api.web.ClickEvent;
import de.mirkosertic.bytecoder.api.web.EventListener;
import de.mirkosertic.bytecoder.api.web.EventTarget;
import de.mirkosertic.bytecoder.api.web.Window;
import de.mirkosertic.bytecoder.backend.CompileTarget;
import de.mirkosertic.bytecoder.unittest.BytecoderTestOption;
import de.mirkosertic.bytecoder.unittest.BytecoderTestOptions;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;

@RunWith(BytecoderUnitTestRunner.class)
@BytecoderTestOptions(value = {
        @BytecoderTestOption(backend = CompileTarget.BackendType.wasm_llvm, preferStackifier = true),
}, includeJVM = false)
public class GCTestStackless {

    private static EventListener<ClickEvent> registerClicklistenerOn(final EventTarget target) {
        final EventListener<ClickEvent> listener = new EventListener<ClickEvent>() {
            @Override
            public void run(ClickEvent aEvent) {

            }
        };
        target.addEventListener("click", listener);
        return listener;
    }

    @Test
    public void testListenerGC() {
        final Window w = Window.window();
        final EventListener<ClickEvent> listener = registerClicklistenerOn(w);
        final int ptr = Address.ptrOf(listener);
        Assert.assertTrue(MemoryManager.isUsedAsCallback(ptr));
        Assert.assertFalse(MemoryManager.isUsedByHeapUserSpace(ptr));
        Assert.assertFalse(MemoryManager.isUsedByStackUserSpace(ptr));
        Assert.assertFalse(MemoryManager.isUsedByStaticDataUserSpace(ptr));
        Assert.assertTrue(MemoryManager.indexInAllocationList(ptr) >= 0);

        MemoryManager.GC();

        Assert.assertTrue(MemoryManager.isUsedAsCallback(ptr));
        Assert.assertFalse(MemoryManager.isUsedByHeapUserSpace(ptr));
        Assert.assertFalse(MemoryManager.isUsedByStackUserSpace(ptr));
        Assert.assertFalse(MemoryManager.isUsedByStaticDataUserSpace(ptr));
        Assert.assertTrue(MemoryManager.indexInAllocationList(ptr) >= 0);
    }
}