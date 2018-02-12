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
package de.mirkosertic.bytecoder.optimizer;

import static org.junit.Assert.*;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.ssa.BinaryExpression;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;
import org.junit.Test;

public class RedundantAssignmentOptimizerTest {

    @Test
    public void testBinaryOptimization() {
        Program theProgram = new Program();
        ControlFlowGraph theGraph = new ControlFlowGraph(theProgram);
        RegionNode theStart = theGraph.createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);

        ExpressionList theExpressions = theStart.getExpressions();
        Variable theVariable1 = new Variable(TypeRef.Native.INT, "var1");
        Variable theVariable2 = new Variable(TypeRef.Native.INT, "var2");

        IntegerValue theInt1 = new IntegerValue(10);
        theVariable1.initializeWith(theInt1);

        theExpressions.add(new VariableAssignmentExpression(theVariable1, theInt1));

        IntegerValue theInt2 = new IntegerValue(20);
        theVariable2.initializeWith(theInt2);

        theExpressions.add(new VariableAssignmentExpression(theVariable2, theInt2));

        Variable theVariable3 = new Variable(TypeRef.Native.INT, "var3");
        BinaryExpression theBinary = new BinaryExpression(TypeRef.Native.INT, theVariable1, BinaryExpression.Operator.ADD, theVariable2);
        theVariable3.initializeWith(theBinary);

        theExpressions.add(new VariableAssignmentExpression(theVariable3, theBinary));

        theExpressions.add(new ReturnValueExpression(theVariable3));

        RedundantAssignmentOptimizer theOptimizer = new RedundantAssignmentOptimizer();

        theOptimizer.optimize(theGraph, null);

        System.out.println(theExpressions);
    }
}