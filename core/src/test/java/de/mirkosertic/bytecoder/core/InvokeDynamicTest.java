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

import java.util.function.Supplier;

@RunWith(BytecoderUnitTestRunner.class)
public class InvokeDynamicTest {

    private static int compute(final int a, final int b) {
        return a + b;
    }

    public interface Computer {
        int compute(int a, int b);
    }

    private static int computeWith(final Computer aComputer, final int a, final int b) {
        return aComputer.compute(a, b);
    }

    @Test
    public void testLambda() {
        final String theLala = "";
        final int x = 1;
        final int y = 2;
        final Runnable theRun = () -> {
            compute(x, y);
        };
    }

    interface Adder {
        int add(int aValue);
    }

    public static int add(final int aValue) {
        return aValue + 1;
    }

    public int addMethodRef(final int aValue) {
        return aValue + 1;
    }

    public int add(final Adder adder) {
        return adder.add(10);
    }

    @Test
    public void testLambdaArguments() {
        final int theResult = computeWith((x,y) -> x + y, 10, 20);
        Assert.assertEquals(theResult, 30, 0);
    }

    @Test
    public void testLambdaAdd() {
        final int theResult = add((i) -> i + 10);
        Assert.assertEquals(20, theResult, 0);
    }

    @Test
    public void testStaticMethodRefAdd() {
        final int theResult = add(InvokeDynamicTest::add);
        Assert.assertEquals(11, theResult, 0);
    }

    @Test
    public void testInstanceMethodRefAdd() {
        final int theResult = add(this::addMethodRef);
        Assert.assertEquals(11, theResult, 0);
    }

    public static class TestConstructorInvoke<T> {

        TestConstructorInvoke() {

        }

        public void test(final Supplier<T> layoutSupplier) {
            final T t = layoutSupplier.get();
            System.out.println(t.toString());
        }

        public static class TestObject {
            @Override
            public String toString() {
                return "TestConstructorInvoke{}";
            }
        }
    }

    @Test
    public void testConstructorRef() {
        final TestConstructorInvoke<TestConstructorInvoke.TestObject> testConstructorInvoke = new TestConstructorInvoke<>();
        testConstructorInvoke.test(TestConstructorInvoke.TestObject::new);
    }

    interface InterfaceA{
        default String test() {
            return "InterfaceA";
        }
    }

    class ClassA implements InterfaceA {

        @Override
        public String test(){
            return "ClassA";
        }
    }

    @Test
    public void testDefaultMethodOverwrite() {
        final ClassA a = new ClassA();
        System.out.println(a.test());
        Assert.assertEquals("ClassA", a.test());

        final InterfaceA b = new InterfaceA() {
        };
        System.out.println(b.test());
        Assert.assertEquals("InterfaceA", b.test());
    }
}
