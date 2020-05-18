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
package de.mirkosertic.bytecoder.escapeanalysis;

import de.mirkosertic.bytecoder.api.web.Event;
import de.mirkosertic.bytecoder.api.web.EventListener;
import de.mirkosertic.bytecoder.api.web.Window;
import de.mirkosertic.bytecoder.backend.llvm.LLVMIntrinsics;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLoader;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.ssa.NaiveProgramGenerator;
import de.mirkosertic.bytecoder.ssa.NewObjectAndConstructExpression;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Type;
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
            KEEPER.memberField = o;
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

    static final Keeper KEEPER = new Keeper();
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

    public static Object isEscapingByPHI(final int value) {
        final TestInstance o;
        if (value > 20) {
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

    public static Object isNotEscapingArgument(final TestInstance o) {
        TestInstance x = null;
        for (int i=0;i<10;i++) {
            if (i > 2) {
                x = o;
            }
        }
        return x;
    }

    public static Object isEscapingArgument(final TestInstance o) {
        TestInstance x = null;
        for (int i=0;i<10;i++) {
            if (i > 2) {
                KEEPER.memberField = o;
                x = o;
            }
        }
        return x;
    }

    public static void doSomething(final TestInstance o) {
    }

    public static void doSomethingStrange(final TestInstance o) {
        KEEPER.memberField = o;
    }

    public static void isNotEscapingByStaticInvocation() {
        final TestInstance o = new TestInstance(10);
        doSomething(o);
    }

    public static void isEscapingByStaticInvocation() {
        final TestInstance o = new TestInstance(10);
        doSomethingStrange(o);
    }

    private static void setTitle(final Window w) {
        w.document().title("Title");
    }

    public static void opaqueNotEscaping() {
        setTitle(Window.window());
    }

    public static void opaqueEventListenerEscaping() {
        Window.window().document().addEventListener("click", new EventListener<Event>() {
            @Override
            public void run(final Event aEvent) {
                System.out.println("Clicked");
            }
        });
    }

    private static TestInstance escapeMember(final AnotherInstance o) {
        return o.testInstance;
    }

    public static void memberEscaping() {
        escapeMember(new AnotherInstance(new TestInstance(10)));
    }

    public static Type[] escapingArray() {
        return new Type[0];
    }

    public static Object isNotEscapingByConstructorArgument() {
        final TestInstance o = new TestInstance(20);
        final AnotherInstance anotherInstance = new AnotherInstance(o);
        return null;
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

        final EscapeAnalysis e = new EscapeAnalysis(new ProgramDescriptorProvider() {
            @Override
            public ProgramDescriptor resolveStaticInvocation(final BytecodeObjectTypeRef aClass, final String aMethodName, final BytecodeMethodSignature aSignature) {
                final BytecodeLinkedClass theRequestedClass = theLinkerContext.resolveClass(aClass);
                final BytecodeMethod theRequestedMethod = theRequestedClass.getBytecodeClass().methodByNameAndSignatureOrNull(aMethodName, aSignature);
                final Program theProgram = theGenerator.generateFrom(theRequestedClass.getBytecodeClass(), theRequestedMethod);
                KnownOptimizer.LLVM.optimize(theProgram.getControlFlowGraph(), theLinkerContext);
                return new ProgramDescriptor(theRequestedClass, theRequestedMethod, theProgram);
            }

            @Override
            public ProgramDescriptor resolveConstructorInvocation(final BytecodeObjectTypeRef aClass, final BytecodeMethodSignature aSignature) {
                final BytecodeLinkedClass theRequestedClass = theLinkerContext.resolveClass(aClass);
                final BytecodeMethod theRequestedMethod = theRequestedClass.getBytecodeClass().methodByNameAndSignatureOrNull("<init>", aSignature);
                final Program theProgram = theGenerator.generateFrom(theRequestedClass.getBytecodeClass(), theRequestedMethod);
                KnownOptimizer.LLVM.optimize(theProgram.getControlFlowGraph(), theLinkerContext);
                return new ProgramDescriptor(theRequestedClass, theRequestedMethod, theProgram);
            }

            @Override
            public ProgramDescriptor resolveDirectInvocation(final BytecodeObjectTypeRef aClass, final String aMethodName, final BytecodeMethodSignature aSignature) {
                final BytecodeLinkedClass theRequestedClass = theLinkerContext.resolveClass(aClass);
                final BytecodeMethod theRequestedMethod = theRequestedClass.getBytecodeClass().methodByNameAndSignatureOrNull(aMethodName, aSignature);
                final Program theProgram = theGenerator.generateFrom(theRequestedClass.getBytecodeClass(), theRequestedMethod);
                KnownOptimizer.LLVM.optimize(theProgram.getControlFlowGraph(), theLinkerContext);
                return new ProgramDescriptor(theRequestedClass, theRequestedMethod, theProgram);
            }
        }, theLinkerContext.getStatistics());

        return e.analyze(new ProgramDescriptor(theLinkedClass, theMethod, p));
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
        final EscapeAnalysis.AnalysisResult theResult = analyze("isEscapingByPHI", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT}));
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
    public void testIsNotEscapingByConstructorArgument() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("isNotEscapingByConstructorArgument", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(0, theEscapingValues.size());
    }

    @Test
    public void testIsNotEscapingArgument() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("isNotEscapingArgument", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{BytecodeObjectTypeRef.fromRuntimeClass(TestInstance.class)}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(0, theEscapingValues.size());
   }

    @Test
    public void testIsEscapingArgument() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("isEscapingArgument", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new BytecodeTypeRef[]{BytecodeObjectTypeRef.fromRuntimeClass(TestInstance.class)}));
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

    @Test
    public void testOpaqueNotEscaping() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("opaqueNotEscaping", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(0, theEscapingValues.size());
    }

    @Test
    public void testOpaqueEventListenerEscaping() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("opaqueEventListenerEscaping", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(1, theEscapingValues.size());
    }

    @Test
    public void testMemberEscaping() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("memberEscaping", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(2, theEscapingValues.size());
    }

    @Test
    public void testEscapingArray() {
        final EscapeAnalysis.AnalysisResult theResult = analyze("escapingArray", new BytecodeMethodSignature(new BytecodeArrayTypeRef(BytecodeObjectTypeRef.fromRuntimeClass(Type.class),1), new BytecodeTypeRef[]{}));
        final Set<Value> theEscapingValues = theResult.getEscapingValues();
        Assert.assertEquals(1, theEscapingValues.size());
    }

}