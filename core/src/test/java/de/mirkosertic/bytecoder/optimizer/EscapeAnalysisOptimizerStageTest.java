package de.mirkosertic.bytecoder.optimizer;

import de.mirkosertic.bytecoder.backend.llvm.LLVMIntrinsics;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLoader;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.NaiveProgramGenerator;
import de.mirkosertic.bytecoder.ssa.NewObjectAndConstructExpression;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.ValueWithEscapeCheck;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class EscapeAnalysisOptimizerStageTest {

    public static class TestInstance  {

        private final int aValue;

        public TestInstance(final int aValue) {
            this.aValue = aValue;
        }

        public int getValue() {
            return aValue;
        }

        public void escapingMethod(final TestInstance o) {
        }
    }

    public static Object isEscapingFromNewObject() {
        final TestInstance o = new TestInstance(100);
        final TestInstance b = o;
        return b;
    }

    public static Object isNotEscaping() {
        final TestInstance o = new TestInstance(100);
        return null;
    }

    public static void escapingMethod(final TestInstance a) {
    }

    public static Object isEscapingByParameterOfMethodInvocation() {
        final TestInstance o = new TestInstance(100);
        escapingMethod(o);
        return null;
    }

    public static Object isNotEscapingByParameterOfMethodInvocation() {
        final TestInstance o = new TestInstance(100);
        final int value = o.getValue();
        return null;
    }

    public static Object isEscapingByParameterOfMethodInvocationWithItself() {
        final TestInstance o = new TestInstance(100);
        o.escapingMethod(o);
        return null;
    }

    private Set<Value> escapingAssignments(final Program p) {
        final Set<Value> theEscaping = new HashSet<>();
        return escapingAssignments(p.getControlFlowGraph(), theEscaping);
    }

    private Set<Value> escapingAssignments(final ControlFlowGraph aGraph, final Set<Value> aEscapingValues) {
        for (final RegionNode theNode : aGraph.dominators().getPreOrder()) {
            escapingAssignments(theNode.getExpressions(), aEscapingValues);
        }
        return aEscapingValues;
    }

    private void escapingAssignments(final ExpressionList aExpressionList, final Set<Value> aEscapingValues) {
        for (final Expression theExpression : aExpressionList.toList()) {
            if (theExpression instanceof ExpressionListContainer) {
                final ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (final ExpressionList theList : theContainer.getExpressionLists()) {
                    escapingAssignments(theList,aEscapingValues);
                }
            }
            if (theExpression instanceof VariableAssignmentExpression) {
                final Value theEscapingTest = theExpression.incomingDataFlows().get(0);
                if (theEscapingTest instanceof ValueWithEscapeCheck) {
                    final ValueWithEscapeCheck valueWithEscapeCheck = (ValueWithEscapeCheck) theEscapingTest;
                    if (valueWithEscapeCheck.isEscaping()) {
                        aEscapingValues.add(theEscapingTest);
                    }
                }
            }
        }
    }

    private Program programFor(final String methodName, final BytecodeMethodSignature aSignature) {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new LLVMIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(getClass()));

        theLinkedClass.resolveStaticMethod(methodName, aSignature);
        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull(methodName, aSignature);
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

        KnownOptimizer.LLVM.optimize(p.getControlFlowGraph(), theLinkerContext);

        return p;
    }

    @Test
    public void testEscapingInstanceFromNew() {
        final Program p = programFor("isEscapingFromNewObject", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{}));

        final Set<Value> theEscapingValues = escapingAssignments(p);
        Assert.assertEquals(1, theEscapingValues.size());
        Assert.assertTrue(theEscapingValues.iterator().next() instanceof NewObjectAndConstructExpression);
    }

    @Test
    public void testIsNotEscaping() {
        final Program p = programFor("isNotEscaping", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{}));

        final Set<Value> theEscapingValues = escapingAssignments(p);
        Assert.assertEquals(0, theEscapingValues.size());
    }

    @Test
    public void testEscapingByParameterOfMethodInvocation() {
        final Program p = programFor("isEscapingByParameterOfMethodInvocation", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{}));

        final Set<Value> theEscapingValues = escapingAssignments(p);
        Assert.assertEquals(1, theEscapingValues.size());
        Assert.assertTrue(theEscapingValues.iterator().next() instanceof NewObjectAndConstructExpression);
    }

    @Test
    public void testIsNotEscapingByParameterOfMethodInvocation() {
        final Program p = programFor("isNotEscapingByParameterOfMethodInvocation", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{}));

        final Set<Value> theEscapingValues = escapingAssignments(p);
        Assert.assertEquals(0, theEscapingValues.size());
    }

    @Test
    public void testIsEscapingByParameterOfMethodInvocationWithItself() {
        final Program p = programFor("isEscapingByParameterOfMethodInvocationWithItself", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{}));

        final Set<Value> theEscapingValues = escapingAssignments(p);
        Assert.assertEquals(1, theEscapingValues.size());
        Assert.assertTrue(theEscapingValues.iterator().next() instanceof NewObjectAndConstructExpression);
    }

}