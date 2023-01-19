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
package de.mirkosertic.bytecoder.asm;

import de.mirkosertic.bytecoder.asm.AbstractClassLinkTest;
import de.mirkosertic.bytecoder.asm.test.UnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(UnitTestRunner.class)
public class OverriddenClassLinkTest {

    public static abstract class Computer {
        public abstract int compute();
    }

    public static class Computer1 extends Computer {
        @Override
        public int compute() {
            return 1;
        }
    }

    public static class Computer2 extends Computer {
        @Override
        public int compute() {
            return 1;
        }
    }

    public static abstract class Factory<T> {
        public abstract T create();
    }

    public static class Instance {

        private Factory<Computer> factory1 = new Factory<Computer>() {
            @Override
            public Computer1 create() {
                return new Computer1();
            }
        };

        private Factory<Computer> factory2 = new Factory<Computer>() {
            @Override
            public Computer2 create() {
                return new Computer2();
            }
        };

        public Computer get(int i) {
            if (i==1) {
                return factory1.create();
            }
            return factory2.create();
        }
    }

    @Test
    public void testOverridden() {
        AbstractClassLinkTest.Instance theInstance = new AbstractClassLinkTest.Instance();
        Assert.assertEquals(1, theInstance.get(1).compute(), 0);
        Assert.assertEquals(2, theInstance.get(2).compute(), 0);
    }
}
