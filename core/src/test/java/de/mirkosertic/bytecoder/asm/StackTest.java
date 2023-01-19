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

import java.util.Stack;

@RunWith(UnitTestRunner.class)
public class StackTest {

    @Test
    public void testPos() {
        Stack<String> theStack = new Stack<>();
        theStack.push("A");
        theStack.push("B");
        Assert.assertEquals(2, theStack.size(), 0);
        Assert.assertEquals("A", theStack.get(0));
        Assert.assertEquals("B", theStack.get(1));
    }
}
