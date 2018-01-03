/*
 * Copyright 2018 Mirko Sertic
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
package de.mirkosertic.bytecoder.ssa;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import org.junit.Assert;
import org.junit.Test;

public class DominanceTest {

    @Test
    public void testDirectFlow() {
        Program theProgram = new Program();
        ControlFlowGraph theGraph = new ControlFlowGraph(theProgram);

        GraphNode theNode1 = theGraph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, GraphNode.BlockType.NORMAL);
        GraphNode theNode2 = theGraph.createAt(new BytecodeOpcodeAddress(10), GraphNode.BlockType.NORMAL);
        GraphNode theNode3 = theGraph.createAt(new BytecodeOpcodeAddress(20), GraphNode.BlockType.NORMAL);

        theNode1.addSuccessor(theNode2);
        theNode2.addSuccessor(theNode3);

        theGraph.calculateReachabilityAndMarkBackEdges();

        Assert.assertTrue(theNode2.isOnlyReachableThru(theNode1));
        Assert.assertTrue(theNode3.isOnlyReachableThru(theNode1));
        Assert.assertTrue(theNode3.isOnlyReachableThru(theNode2));

        Assert.assertFalse(theNode1.isOnlyReachableThru(theNode2));
        Assert.assertFalse(theNode1.isOnlyReachableThru(theNode3));
        Assert.assertFalse(theNode2.isOnlyReachableThru(theNode3));
    }

    @Test
    public void testIFElseWithJoining() {
        Program theProgram = new Program();
        ControlFlowGraph theGraph = new ControlFlowGraph(theProgram);

        GraphNode theNode1 = theGraph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, GraphNode.BlockType.NORMAL);
        GraphNode theNode2 = theGraph.createAt(new BytecodeOpcodeAddress(10), GraphNode.BlockType.NORMAL);
        GraphNode theNode3 = theGraph.createAt(new BytecodeOpcodeAddress(20), GraphNode.BlockType.NORMAL);
        GraphNode theNode4 = theGraph.createAt(new BytecodeOpcodeAddress(30), GraphNode.BlockType.NORMAL);

        theNode1.addSuccessor(theNode2);
        theNode1.addSuccessor(theNode3);
        theNode2.addSuccessor(theNode4);
        theNode3.addSuccessor(theNode4);

        theGraph.calculateReachabilityAndMarkBackEdges();

        Assert.assertTrue(theNode4.isOnlyReachableThru(theNode1));
        Assert.assertFalse(theNode4.isOnlyReachableThru(theNode2));
        Assert.assertFalse(theNode4.isOnlyReachableThru(theNode3));
    }
}
