/*
 * Copyright 2020 Mirko Sertic
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
package de.mirkosertic.bytecoder.core;

import de.mirkosertic.bytecoder.backend.llvm.LLVMIntrinsics;
import de.mirkosertic.bytecoder.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.ssa.NaiveProgramGenerator;
import de.mirkosertic.bytecoder.ssa.NewObjectAndConstructExpression;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EscapeAnalysisTest {

    public enum TestEnum {
        ONE
    }

    public static class TestInstance extends RuntimeException  {

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

    public static class AnotherInstance  {

        private final TestInstance testInstance;

        public AnotherInstance(final TestInstance testInstance) {
            this.testInstance = testInstance;
        }
    }

    public static class Keeper {
        TestInstance memberField;
    }

    static Keeper KEEPER = new Keeper();
    static TestInstance staticField;

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

    public static Object isEscapingByStaticFieldWrite() {
        final TestInstance o = new TestInstance(100);
        staticField = o;
        return null;
    }

    public static Object isEscapingByInstanceFieldWrite() {
        final TestInstance o = new TestInstance(100);
        KEEPER.memberField = o;
        return null;
    }

    public static Object isEscapingByArrayWrite() {
        final TestInstance o = new TestInstance(100);
        final Object[] array = new Object[10];
        array[5] = o;
        return null;
    }

    public static Object isEscapingByPHI() {
        final TestInstance o;
        int x = 10;
        if (x > 20) {
            o = new TestInstance(20);
        } else {
            o = new TestInstance(10);
        }
        return o;
    }

    public static Object isEscapingByPHIFromLoop() {
        TestInstance o = null;
        for (int i=0;i<10;i++) {
            if (i > 4) {
                o = new TestInstance(i);
            }
        }
        return o;
    }

    public static Object isEscapingByThrow() {
        final TestInstance o = new TestInstance(10);
        throw o;
    }

    public static Object isEscapingByConstructorArgument() {
        final TestInstance o = new TestInstance(20);
        final AnotherInstance anotherInstance = new AnotherInstance(o);
        return anotherInstance;
    }

    public static Object isEscapingArgument(final Object o) {
        Object x = null;
        for (int i=0;i<10;i++) {
            if (i > 2) {
                x = o;
            }
        }
        return x;
    }

    public static void doSomething(final TestInstance o) {
        o.toString();
    }

    public static void doSomethingStrange(final TestInstance o) {
        KEEPER.memberField = o;
    }

    public static void isNotEscapingByStaticInvocation() {
        TestInstance o = new TestInstance(10);
        doSomething(o);
    }

    public static void isEscapingByStaticInvocation() {
        TestInstance o = new TestInstance(10);
        doSomethingStrange(o);
    }

    private EscapeAnalysis.AnalysisResult analyze(final String methodName, final BytecodeMethodSignature aSignature) {
        return analyze(getClass(), methodName, aSignature);
    }

    private EscapeAnalysis.AnalysisResult analyze(final Class aClazz, final String methodName, final BytecodeMethodSignature aSignature) {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new LLVMIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(aClazz));

        theLinkedClass.resolveStaticMethod(methodName, aSignature);
        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull(methodName, aSignature);
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

        KnownOptimizer.LLVM.optimize(p.getControlFlowGraph(), theLinkerContext);

        final EscapeAnalysis e = new EscapeAnalysis((aAnalysis, aLinkedClass, aMethodName, aRequestedSignature) -> {
            final List<EscapeAnalysis.AnalysisResult> theResult = new ArrayList<>();
            final BytecodeLinkedClass theRequestedClass = theLinkerContext.resolveClass(aLinkedClass);
            final BytecodeMethod theRequestedMethod = theRequestedClass.getBytecodeClass().methodByNameAndSignatureOrNull(aMethodName, aRequestedSignature);
            final Program theProgram = theGenerator.generateFrom(theRequestedClass.getBytecodeClass(), theRequestedMethod);
            theResult.add(aAnalysis.analyze(theRequestedClass, theRequestedMethod, theProgram));
            return theResult;
        });

        return e.analyze(theLinkedClass, theMethod, p);
    }


    @Test
    public void testEscapingInstanceFromNew() {
        final EscapeAnalysis.AnalysisResult theResult =  analyze("isEscapingFromNewObject", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();

        Assert.assertEquals(1, theEscapingValues.size());
        Assert.assertTrue(theEscapingValues.iterator().next() instanceof NewObjectAndConstructExpression);
    }

    @Test
    public void testIsNotEscaping() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("isNotEscaping", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(0, theEscapingValues.size());
    }

    @Test
    public void testEscapingByParameterOfMethodInvocation() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("isEscapingByParameterOfMethodInvocation", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(1, theEscapingValues.size());
        Assert.assertTrue(theEscapingValues.iterator().next() instanceof NewObjectAndConstructExpression);
    }

    @Test
    public void testIsNotEscapingByParameterOfMethodInvocation() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("isNotEscapingByParameterOfMethodInvocation", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(0, theEscapingValues.size());
    }

    @Test
    public void testIsEscapingByParameterOfMethodInvocationWithItself() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("isEscapingByParameterOfMethodInvocationWithItself", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(1, theEscapingValues.size());
        Assert.assertTrue(theEscapingValues.iterator().next() instanceof NewObjectAndConstructExpression);
    }

    @Test
    public void testIsEscapingByStaticFieldWrite() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("isEscapingByStaticFieldWrite", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(1, theEscapingValues.size());
        Assert.assertTrue(theEscapingValues.iterator().next() instanceof NewObjectAndConstructExpression);
    }

    @Test
    public void testIsEscapingByInstanceFieldWrite() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("isEscapingByInstanceFieldWrite", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(1, theEscapingValues.size());
        Assert.assertTrue(theEscapingValues.iterator().next() instanceof NewObjectAndConstructExpression);
    }

    @Test
    public void testIsEscapingByArrayWrite() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("isEscapingByArrayWrite", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(1, theEscapingValues.size());
        Assert.assertTrue(theEscapingValues.iterator().next() instanceof NewObjectAndConstructExpression);
    }

    @Test
    public void testIsEscapingByPHI() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("isEscapingByPHI", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(2, theEscapingValues.size());
        final List<Value> theValues = theEscapingValues.stream().collect(Collectors.toList());
        Assert.assertTrue(theValues.get(0) instanceof NewObjectAndConstructExpression);
        Assert.assertTrue(theValues.get(1) instanceof NewObjectAndConstructExpression);
    }

    @Test
    public void testIsEscapingByPHIFromLoop() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("isEscapingByPHIFromLoop", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(1, theEscapingValues.size());
        Assert.assertTrue(theEscapingValues.iterator().next() instanceof NewObjectAndConstructExpression);
    }

    @Test
    public void testIsEscapingByThrow() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("isEscapingByThrow", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(1, theEscapingValues.size());
        Assert.assertTrue(theEscapingValues.iterator().next() instanceof NewObjectAndConstructExpression);
    }

    @Test
    public void testIsEscapingEnumInit() {
        final EscapeAnalysis.AnalysisResult theResult = analyze(TestEnum.class, "<clinit>", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(2, theEscapingValues.size());
    }

    @Test
    public void testIsEscapingByConstructorArgument() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("isEscapingByConstructorArgument", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(2, theEscapingValues.size());
    }

    @Test
    public void testIsEscapingArgument() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("isEscapingArgument", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{BytecodeObjectTypeRef.fromRuntimeClass(Object.class)}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(1, theEscapingValues.size());
        Assert.assertTrue(theResult.isMethodArgumentEscaping(0));
    }

    @Test
    public void testIsNotEscapingByStaticInvocation() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("isNotEscapingByStaticInvocation", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(0, theEscapingValues.size());
    }

    @Test
    public void testIsEscapingByStaticInvocation() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("isEscapingByStaticInvocation", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(1, theEscapingValues.size());
    }
}