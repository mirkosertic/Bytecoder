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

import de.mirkosertic.bytecoder.asm.test.UnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(UnitTestRunner.class)
public class AbstractClassLinkTest {

    public static abstract class Computer {
        public abstract int compute();
    }

    public static class Instance {
        private Computer computer1 = new Computer() {
            @Override
            public int compute() {
                return 1;
            }
        };

        private Computer computer2 = new Computer() {
            @Override
            public int compute() {
                return 2;
            }
        };

        public Computer get(int i) {
            if (i==1) {
                return computer1;
            }
            return computer2;
        }
    }

    public abstract class BaseClass {

        public abstract int compute();

        public int getValue() {
            return compute();
        }
    }

    public class SubClass extends BaseClass {

        @Override
        public int compute() {
            return 1000;
        }
    }

    @Test
    public void testInheritedAbstractMethod() {
        SubClass theSub = new SubClass();
        int theResult = theSub.compute();
        Assert.assertEquals(1000, theResult, 0);
    }

    @Test
    public void testLinkAndAbstractInvocation() {
        Instance theInstance = new Instance();
        Computer theComputer = theInstance.get(1);
        Assert.assertNotNull(theComputer);
        Assert.assertEquals(1, theComputer.compute(), 0);
        Assert.assertEquals(2, theInstance.get(2).compute(), 0);
    }
}
