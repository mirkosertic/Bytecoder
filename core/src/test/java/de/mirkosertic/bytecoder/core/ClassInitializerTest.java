/*
 * Copyright 2019 Mirko Sertic
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
public class ClassInitializerTest {

    static int value;
    static {
        value = 11 + 1;
    }

    public class TestClass {
        int x;
        public TestClass() {
            x = ClassInitializerTest.value;
        }
    }

    public static class Parent {
        public static int x = 12;
    }

    public static class Child extends Parent {

        int y;

        public Child() {
            y = Parent.x;
        }
    }

    @Test
    public void testInitialized() {
        final TestClass test = new TestClass();
        Assert.assertEquals(12, test.x, 0);
    }

    @Test
    public void testStaticFieldInherited() {
        final Child c = new Child();
        Assert.assertEquals(12, c.y, 0);
        Assert.assertEquals(12, Parent.x, 0);
        Assert.assertEquals(12, Child.x, 0);
    }
}