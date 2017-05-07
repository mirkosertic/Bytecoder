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
public class SimpleMathTest {

    public static int sum(int a, int b) {
        return a + b;
    }

    public static int sub(int a, int b) {
        return a - b;
    }

    public static int mul(int a, int b) {
        return a * b;
    }

    public static int div(int a, int b) {
        return a / b;
    }

    @Test
    public void testAdd() throws TRuntimeException {
        int c = sum(10, 20);
        TAssert.assertEquals(30, c, 0);
    }

    @Test
    public void testSub() throws TRuntimeException {
        int c = sub(10, 20);
        TAssert.assertEquals(-10, c, 0);
    }

    @Test
    public void testMul() throws TRuntimeException {
        int c = mul(10, 30);
        TAssert.assertEquals(300, c, 0);
    }

    @Test
    public void testDiv() throws TRuntimeException {
        int c = div(100, 5);
        TAssert.assertEquals(20, c, 0);
    }
}