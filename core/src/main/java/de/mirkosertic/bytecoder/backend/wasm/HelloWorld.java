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
package de.mirkosertic.bytecoder.backend.wasm;

import de.mirkosertic.bytecoder.annotations.Export;

public class HelloWorld {

    public interface Test {

        int getValue();
    }

    public static class Test1 implements Test {
        @Override
        public int getValue() {
            return 10;
        }
    }

    public static class Test2 implements Test {
        @Override
        public int getValue() {
            return 20;
        }
    }

    @Export("compute")
    public static int compute(int a, int b) {
        return a + b;
    }

    public static void main(String[] args) {
        int x = compute(10, 20);
        int z = 1;
        if (x == 30) {
            Test theTest1 = new Test1();
            Test theTest2 = new Test2();
            z = theTest1.getValue() + theTest2.getValue();
        }

    }
}
