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

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
public class InvokeDynamicTest {

    private static int compute(int a, int b) {
        return a + b;
    }

    public interface Computer {
        int compute(int a, int b);
    }

    private static int computeWith(Computer aComputer, int a, int b) {
        return aComputer.compute(a, b);
    }

    @Test
    public void testLambda() {
        String theLala = "";
        final int x = 1;
        final int y = 2;
        Runnable theRun = () -> {
            compute(x, y);
        };
    }

    interface Adder {
        int add(int aValue);
    }

    public static int add(int aValue) {
        return aValue + 1;
    }

    public int addMethodRef(int aValue) {
        return aValue + 1;
    }

    public int add(Adder adder) {
        return adder.add(10);
    }

    @Test
    public void testLambdaArguments() {
        int theResult = computeWith((x,y) -> x + y, 10, 20);
        Assert.assertEquals(theResult, 30, 0);
    }

    @Test
    public void testLambdaAdd() {
        int theResult = add((i) -> i + 10);
        Assert.assertEquals(20, theResult, 0);
    }

    @Test
    public void testStaticMethodRefAdd() {
        int theResult = add(InvokeDynamicTest::add);
        Assert.assertEquals(11, theResult, 0);
    }

    @Test
    public void testInstanceMethodRefAdd() {
        int theResult = add(this::addMethodRef);
        Assert.assertEquals(11, theResult, 0);
    }
}
