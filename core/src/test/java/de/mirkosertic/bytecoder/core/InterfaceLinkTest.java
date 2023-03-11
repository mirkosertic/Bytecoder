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

import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(UnitTestRunner.class)
public class InterfaceLinkTest {

    public interface Computer {

        int compute();
    }

    public static class ComputerImpl implements Computer {

        @Override
        public int compute() {
            return 10;
        }
    }

    public static class Runner {

        private final Computer computer;

        public Runner() {
            computer = new ComputerImpl();
        }

        public int compute() {
            return computer.compute();
        }
    }

    public interface DefaultInterface {

        default int getValue() {
            return 42;
        }
    }

    public static class DefaultClass implements DefaultInterface {

    }

    public interface WithDefaultMethod {

        default int getValue() {
            return 42;
        }
    }

    public static class ImplWithDefaultMethod implements WithDefaultMethod  {

        public int computeValue() {
            return getValue();
        }
    }

    @Test
    public void testDefaultMethodInvocation() {
        ImplWithDefaultMethod theImpl = new ImplWithDefaultMethod();
        Assert.assertEquals(theImpl.computeValue(), 42, 0);
    }

    @Test
    public void testDefaultMethodInvocationAnonymous() {
        WithDefaultMethod theImpl = new WithDefaultMethod() {};
        Assert.assertEquals(theImpl.getValue(), 42, 0);
    }

    @Test
    public void testCompute() {
        Runner theRunner = new Runner();
        Assert.assertEquals(theRunner.compute(), 10, 0);
    }

    @Test
    public void testDefaultMethod() {
        DefaultClass theInstance = new DefaultClass();
        Assert.assertEquals(theInstance.getValue(), 42, 0);
    }
}
