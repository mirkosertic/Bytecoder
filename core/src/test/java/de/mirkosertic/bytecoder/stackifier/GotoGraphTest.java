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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;

public class GotoGraphTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testSimpleLoop() {
        final GotoGraphBuilder builder = new GotoGraphBuilder(Arrays.asList(0, 1, 2, 3));
        builder.add(EdgeType.forward, 0, 1);
        builder.add(EdgeType.forward, 1, 2);
        builder.add(EdgeType.forward, 2, 3);
        builder.add(EdgeType.back, 2, 1);
        final GotoGraph graph = builder.build();
        graph.printDebug(System.out);
        graph.printStructurePseudoCode(System.out);
    }

    @Test
    public void testAdvancedLoop() {
        final GotoGraphBuilder builder = new GotoGraphBuilder(Arrays.asList(0,1, 2, 3, 4));
        builder.add(EdgeType.forward, 0, 1);
        builder.add(EdgeType.forward, 1, 2);
        builder.add(EdgeType.forward, 2, 3);
        builder.add(EdgeType.back, 3, 1);
        builder.add(EdgeType.forward, 2, 4);
        final GotoGraph graph = builder.build();
        graph.printDebug(System.out);
        graph.printStructurePseudoCode(System.out);
    }

    @Test
    public void testTwoDominatedLoops() {
        final GotoGraphBuilder builder = new GotoGraphBuilder(Arrays.asList(0, 1, 2, 3, 4, 5));
        builder.add(EdgeType.forward, 0, 1);
        builder.add(EdgeType.forward, 1, 2);
        builder.add(EdgeType.back, 2, 1);
        builder.add(EdgeType.forward, 0, 3);
        builder.add(EdgeType.forward, 3, 4);
        builder.add(EdgeType.back, 4, 3);
        builder.add(EdgeType.forward, 2, 5);
        builder.add(EdgeType.forward, 4, 5);
        final GotoGraph graph = builder.build();
        graph.printDebug(System.out);
        graph.printStructurePseudoCode(System.out);
    }

    @Test
    public void testHeadToHead() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("{2,3} are head to head");

        final GotoGraphBuilder builder = new GotoGraphBuilder(Arrays.asList(0, 1, 2, 3));
        builder.add(EdgeType.forward,0, 2);
        builder.add(EdgeType.back, 3, 1);
        final GotoGraph graph = builder.build();
        graph.printDebug(System.out);
    }
}