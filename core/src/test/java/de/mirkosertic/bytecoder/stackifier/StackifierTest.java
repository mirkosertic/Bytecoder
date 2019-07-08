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

import static org.junit.Assert.*;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.DebugInformation;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import org.junit.Test;

public class StackifierTest {

    @Test
    public void testOnlyOneNode() {
        final Program p = new Program(DebugInformation.empty());
        final ControlFlowGraph g = new ControlFlowGraph(p);
        final RegionNode startNode = g.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);
        g.calculateReachabilityAndMarkBackEdges();

        final Stackifier stackifier = new Stackifier();

        final Sequence s = stackifier.stackify(g);
        final String output = DebugOutput.toString(s);

        assertEquals("$0:" + System.lineSeparator(), output);
    }
}