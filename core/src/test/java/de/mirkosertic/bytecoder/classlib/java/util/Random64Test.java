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
package de.mirkosertic.bytecoder.classlib.java.util;

import de.mirkosertic.bytecoder.backend.CompileTarget;
import de.mirkosertic.bytecoder.unittest.BytecoderTestOption;
import de.mirkosertic.bytecoder.unittest.BytecoderTestOptions;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

@RunWith(BytecoderUnitTestRunner.class)
@BytecoderTestOptions(includeJVM = true, includeTestPermutations = false,
        value = {
                @BytecoderTestOption(backend = CompileTarget.BackendType.wasm_llvm)
        }
)
public class Random64Test {

    @Test
    public void testNextLong() {
        final long x1 = new Random().nextLong();
    }

    @Test
    public void testNextDouble() {
        final double x1 = new Random().nextDouble();
    }
}
