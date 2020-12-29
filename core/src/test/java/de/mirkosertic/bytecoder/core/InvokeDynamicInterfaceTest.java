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
package de.mirkosertic.bytecoder.core;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.function.Consumer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(BytecoderUnitTestRunner.class)
public class InvokeDynamicInterfaceTest {

    interface TestInterface {
        void print();
    }

    @FunctionalInterface
    interface Checker {

        boolean is(int ch);

        default Checker union(final Checker p) {
            return ch -> {
                System.out.println("Union checker for " + ch);
                return is(ch) || p.is(ch);
            };
        }

        default Checker and(final Checker p) {
            return ch -> {
                System.out.println("And checker");
                return is(ch) && p.is(ch);
            };
        }
    }

    static class ConstChecker implements Checker {
        private final int value;

        public ConstChecker(final int value) {
            this.value = value;
        }

        @Override
        public boolean is(final int ch) {
            System.out.println("Const check for " + ch + ", expected " + value);
            return value == ch;
        }
    }

    @Test
    public void testInterfaceRef() {
        fire(new TestInterface() {
            @Override
            public void print() {
                System.out.println("TestInterface");
            }
        }, TestInterface::print);
    }

    private static <E> void fire(final TestInterface listener, final Consumer<E> callback) {
        callback.accept((E) listener);
    }

    private static boolean inRange(final int lower, final int ch, final int upper) {
        return lower <= ch && ch <= upper;
    }

    static Checker Range(final int lower, final int upper) {
        return ch -> inRange(lower, ch, upper);
    }

    @Test
    public void testUnionChecker() {

        final Checker a = new ConstChecker(10);
        final Checker b = new ConstChecker(20);
        final Checker union = a.union(b);
        assertTrue(a.is(10));
        assertFalse(a.is(30));
        assertTrue(b.is(20));
        assertFalse(b.is(30));

        assertTrue(union.is(10));
        assertTrue(union.is(20));
        assertFalse(union.is(30));
    }

    @Test
    public void testRangeChecker() {
        final Checker b = Range(5, 8);

        assertFalse(b.is(-10));
        assertTrue(b.is(6));
    }

    @Test
    public void testAndRangeChecker() {
        final Checker a = new ConstChecker(6);
        final Checker b = Range(5, 8);
        final Checker andChecker = a.and(b);

        assertFalse(andChecker.is(-10));
        assertTrue(andChecker.is(6));
    }

    @Test
    public void testAndRangeCheckerInverted() {
        final Checker a = new ConstChecker(6);
        final Checker b = Range(5, 8);
        final Checker andChecker = b.and(a);

        assertFalse(andChecker.is(-10));
        assertTrue(andChecker.is(6));
    }

    @Test
    public void testLambdaInstanceOf() {
        final Object c = Range(10, 10);
        assertTrue(c instanceof Checker);
        assertFalse(c instanceof String);
    }
}