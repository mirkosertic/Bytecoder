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
package de.mirkosertic.bytecoder.core;

import de.mirkosertic.bytecoder.classlib.java.lang.TRuntimeException;
import de.mirkosertic.bytecoder.classlib.org.junit.TAssert;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
public class TestVirtualMethod {

    public static class SimpleThing {
        public int compute() {
            return 0;
        }
    }

    public static class ExtendedThing extends SimpleThing {

        @Override
        public int compute() {
            return 10;
        }
    }

    @Test
    public void testSimpleThing() throws TRuntimeException {
        TAssert.assertEquals(0, new SimpleThing().compute(), 0);
    }

    @Test
    public void testExtendedThing() throws TRuntimeException {
        TAssert.assertEquals(10, new ExtendedThing().compute(), 0);
    }

    @Test
    public void testVirtualInvocation() throws TRuntimeException {
        ExtendedThing theThing = new ExtendedThing();
        SimpleThing theSimple = theThing;
        TAssert.assertEquals(10, theSimple.compute(), 0);
    }
}
