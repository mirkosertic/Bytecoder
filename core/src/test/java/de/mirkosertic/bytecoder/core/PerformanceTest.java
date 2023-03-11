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

import de.mirkosertic.bytecoder.api.Import;
import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(UnitTestRunner.class)
public class PerformanceTest {

    public static int add(int a, int b) {
        return a + b;
    }

    @Import(module = "math", name = "add")
    public static int addImported(int a, int b) {
        return a + b;
    }

    @Test
    public void testPerformance() {
        // JVM 2ms
        // JS 39ms
        // WASM 49ms

        long theStart = System.currentTimeMillis();
        for (int i=0;i<10_000_000;i++) {
            int theSum = add(i, i);
        }
        long theDuration = System.currentTimeMillis() - theStart;
        System.out.println(theDuration);

    }

    @Test
    public void testPerformanceImported() {
        // JVM 7ms
        // JS 39ms
        // WASM 81ms

        long theStart = System.currentTimeMillis();
        for (int i=0;i<10_000_000;i++) {
            int theSum = addImported(i, i);
        }
        long theDuration = System.currentTimeMillis() - theStart;
        System.out.println(theDuration);

    }

    @Test
    public void testObjectAllocation() {
        // JVM 1
        // JS 4
        // WASM 2
        long theStart = System.currentTimeMillis();
        for (int i=0;i<50_000;i++) {
            Object x = new Object();
        }
        long theDuration = System.currentTimeMillis() - theStart;
        System.out.println(theDuration);
    }

}
