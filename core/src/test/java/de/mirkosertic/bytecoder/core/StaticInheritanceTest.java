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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;

@RunWith(BytecoderUnitTestRunner.class)
public class StaticInheritanceTest {

    public static class BaseClass {

        public static int getInt() {
            return 42;
        }
    }

    public static class Subclass extends BaseClass {

        public int call() {
            return getInt();
        }
    }

    public static class SubSubclass extends Subclass {

        @Override
        public int call() {
            return Subclass.getInt();
        }
    }


    @Test
    public void testInheritedMethodInvocation() {
        Assert.assertEquals(42, Subclass.getInt(), 0);
    }

    @Test
    public void testInvokeFromWithinInstance() {
        Subclass theInstance = new Subclass();
        Assert.assertEquals(42, theInstance.call(), 0);
    }

    @Test
    public void testInvokeFromWithinInstanceSubclass() {
        SubSubclass theInstance = new SubSubclass();
        Assert.assertEquals(42, theInstance.call(), 0);
    }
}
