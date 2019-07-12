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
package de.mirkosertic.bytecoder.stackifier;

import de.mirkosertic.bytecoder.ssa.EdgeType;
import org.junit.Test;

import java.util.Arrays;

public class JumpArrowStackTest {

    @Test
    public void testSimpleLoop() {
        final JumpArrowStack stack = new JumpArrowStack();
        stack.add(EdgeType.forward, 0, 1);
        stack.add(EdgeType.forward, 1, 2);
        stack.add(EdgeType.forward, 2, 3);
        stack.add(EdgeType.back, 2, 1);
        stack.stackify(Arrays.asList(0, 1, 2, 3));
    }

    @Test
    public void testAdvancedLoop() {
        final JumpArrowStack stack = new JumpArrowStack();
        stack.add(EdgeType.forward, 0, 1);
        stack.add(EdgeType.forward, 1, 2);
        stack.add(EdgeType.forward, 2, 3);
        stack.add(EdgeType.forward, 3, 4);
        stack.add(EdgeType.forward, 2, 5);
        stack.add(EdgeType.back, 4, 1);
        stack.stackify(Arrays.asList(0,1, 2, 3, 4, 5));
    }

    @Test
    public void testTwoDominatedLoops() {
        final JumpArrowStack stack = new JumpArrowStack();
        stack.add(EdgeType.forward, 0, 1);
        stack.add(EdgeType.forward, 1, 2);
        stack.add(EdgeType.forward, 0, 3);
        stack.add(EdgeType.forward, 3, 4);
        stack.add(EdgeType.forward, 2, 5);
        stack.add(EdgeType.forward, 4, 5);
        stack.add(EdgeType.back, 2, 1);
        stack.add(EdgeType.back, 4, 3);
        stack.stackify(Arrays.asList(0, 1, 2, 3, 4, 5));
        System.out.println(stack);
    }
}