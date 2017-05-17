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

import org.junit.Test;
import org.junit.runner.RunWith;

import de.mirkosertic.bytecoder.classlib.java.lang.TRuntimeException;
import de.mirkosertic.bytecoder.classlib.org.junit.TAssert;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;

@RunWith(BytecoderUnitTestRunner.class)
public class AbstractClassLinkTest {

    public static abstract class Computer {
        public abstract int compute();
    }

    public static class Instance {
        private Computer computer1 = new Computer() {
            @Override
            public int compute() {
                return 1;
            }
        };

        private Computer computer2 = new Computer() {
            @Override
            public int compute() {
                return 2;
            }
        };

        public Computer get(int i) {
            if (i==1) {
                return computer1;
            }
            return computer2;
        }
    }

    @Test
    public void testLinkAndAbstractInvocation() throws TRuntimeException {
        Instance theInstance = new Instance();
        TAssert.assertEquals(1, theInstance.get(1).compute(), 0);
        TAssert.assertEquals(2, theInstance.get(2).compute(), 0);
    }
}
