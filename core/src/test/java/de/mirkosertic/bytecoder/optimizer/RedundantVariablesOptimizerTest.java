package de.mirkosertic.bytecoder.optimizer;

import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.js.JSSSAWriter;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.ssa.BinaryExpression;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class RedundantVariablesOptimizerTest {

    @Test
    public void testOptimize1() {
        final Program theProgram = new Program();

        final ControlFlowGraph theGraph = theProgram.getControlFlowGraph();
        final RegionNode theNode = theGraph.createAt(new BytecodeOpcodeAddress(0), RegionNode.BlockType.NORMAL);

        //
        final Variable theVar1 = theNode.newVariable(TypeRef.Native.INT);
        final IntegerValue theIntValue1 = new IntegerValue(10);
        theVar1.initializeWith(theIntValue1);
        theNode.getExpressions().add(new VariableAssignmentExpression(theVar1, theIntValue1));

        //
        final Variable theVar2 = theNode.newVariable(TypeRef.Native.INT);
        theVar2.initializeWith(theVar1);
        theNode.getExpressions().add(new VariableAssignmentExpression(theVar2, theVar1));

        //
        final Variable theVar3 = theNode.newVariable(TypeRef.Native.INT);
        final BinaryExpression theBinary = new BinaryExpression(TypeRef.Native.INT, theVar2, BinaryExpression.Operator.ADD, new IntegerValue(300));
        theVar3.initializeWith(theBinary);
        theNode.getExpressions().add(new VariableAssignmentExpression(theVar3, theBinary));

        theNode.getExpressions().add(new ReturnValueExpression(theVar3));

        final RedundantVariablesOptimizer theOptimizer = new RedundantVariablesOptimizer();
        theOptimizer.optimize(theGraph, null);

        assertEquals(0, theProgram.getVariables().size(), 0);
        assertEquals(1, theNode.getExpressions().size(), 0);
        assertTrue(theNode.getExpressions().toList().get(0) instanceof ReturnValueExpression);

        final StringWriter theString = new StringWriter();
        final JSSSAWriter theWriter = new JSSSAWriter(mock(CompileOptions.class), theProgram, "", new PrintWriter(theString), null, null, false);
        theWriter.writeExpressions(theNode.getExpressions());

        assertEquals("return (10 + 300);" + System.lineSeparator(), theString.toString());
    }
}