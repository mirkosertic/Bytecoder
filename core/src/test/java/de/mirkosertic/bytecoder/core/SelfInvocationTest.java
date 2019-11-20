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

import de.mirkosertic.bytecoder.backend.CompileTarget;
import de.mirkosertic.bytecoder.classlib.MemoryManager;
import de.mirkosertic.bytecoder.unittest.BytecoderTestOption;
import de.mirkosertic.bytecoder.unittest.BytecoderTestOptions;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
@BytecoderTestOptions(value = {
        @BytecoderTestOption(backend = CompileTarget.BackendType.wasm)
}, includeJVM = false)
public class SelfInvocationTest {

    public static abstract class Stack {

        private final Object[] data;
        private final int size;
        private Object something;

        public Stack(final int aSize) {
            System.out.println(" 1");
            data = new Object[aSize];
            for (int i=0;i<aSize;i++) {
                data[i] = create();
            }
            System.out.println(" 2");
            size = aSize;
            System.out.println(" 3");
            //something = "A";
            System.out.println(" 4");
        }

        public abstract Object create();
    }

    public static class Computer {

        private final Stack stack1;
        private final Stack stack2;

        public Computer(){
            System.out.println("Create A");
            stack1 = new Stack(10) {
                @Override
                public Object create() {
                    System.out.println("   Create AA");
                    return new byte[10];
                }
            };
            System.out.println("Create AF");

            System.out.println("Create B");
            stack2 = new Stack(5) {
                @Override
                public Object create() {
                    System.out.println("   Create BB");
                    return new byte[10];
                }
            };

            System.out.println("Finished");
        }
    }

    @Test
    public void testCreate() {
        System.out.println("Start");
        final Computer theComputer = new Computer();
        System.out.println("Finished test");
    }
}
