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
public class SimpleStringTest {

    public static class A {

        public int getInt() {
            return 42;
        }
    }

    public int getInt(A a) {
        return a.getInt();
    }

    @Test
    public void testGetInt() {
        A a = new A();
        Assert.assertEquals(42, getInt(a), 0);
    }

    @Test
    public void testString() {
        String theData = "Lala";
        int theLength = theData.length();
        if (theLength != 4) {
            throw new IllegalStateException();
        }
    }
}
