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
package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.backend.CompileTarget;
import de.mirkosertic.bytecoder.unittest.BytecoderTestOption;
import de.mirkosertic.bytecoder.unittest.BytecoderTestOptions;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
@BytecoderTestOptions(includeJVM = true, includeTestPermutations = false,
        value = {
            @BytecoderTestOption(backend = CompileTarget.BackendType.wasm_llvm)
        }
)
public class Long64Test {

    @Test
    public void testLongValueToString() {
        final long l = 1588577957583L;
        final StringBuilder b = new StringBuilder();
        b.append(l);
        System.out.println(b);
        Assert.assertEquals("1588577957583", b.toString());
    }

    @Test
    public void testBitCount() {
        final int count = Long.bitCount(1234L);
        Assert.assertEquals(5, count, 0);
    }

    @Test
    public void testBase16HexToString() {
        long theValue = Long.parseLong("171f072b1e1", 16);
        System.out.println(theValue);
        Assert.assertEquals(1588876980705L, theValue);
    }

    /*
    @Test
    public void testNumberOfTrailingZeros() {
        final int count = Long.numberOfTrailingZeros(1234L);
        System.out.println(count);
        Assert.assertEquals(1, count, 0);
    }*/
}
