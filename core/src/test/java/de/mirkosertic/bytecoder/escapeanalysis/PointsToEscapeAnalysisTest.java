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

import de.mirkosertic.bytecoder.backend.llvm.LLVMIntrinsics;
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
import de.mirkosertic.bytecoder.ssa.NewArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewMultiArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewObjectAndConstructExpression;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PointsToEscapeAnalysisTest {

    final static BytecodeObjectTypeRef OBJECT_TYPE_REF = BytecodeObjectTypeRef.fromRuntimeClass(Object.class);

    public static class A {
        Object o;

        public A(final Object o) {
            this.o = o;
        }
    }

    private static Object ESCAPER;

    private PointsToEscapeAnalysis.AnalysisResult analyzeVirtualMethod(final Class aClazz, final String methodName, final BytecodeMethodSignature aSignature) {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new LLVMIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(aClazz));

        theLinkedClass.resolveVirtualMethod(methodName, aSignature);
        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull(methodName, aSignature);
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);
        KnownOptimizer.LLVM.optimize(p.getControlFlowGraph(), theLinkerContext);

        return analyzeMethod(theLinkerContext, new ProgramDescriptor(theLinkedClass, theMethod, p));
    }

    private PointsToEscapeAnalysis.AnalysisResult analyzeStaticMethod(final Class aClazz, final String methodName, final BytecodeMethodSignature aSignature) {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new LLVMIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(aClazz));

        theLinkedClass.resolveStaticMethod(methodName, aSignature);
        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull(methodName, aSignature);
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);
        KnownOptimizer.LLVM.optimize(p.getControlFlowGraph(), theLinkerContext);

        return analyzeMethod(theLinkerContext, new ProgramDescriptor(theLinkedClass, theMethod, p));
    }

    private PointsToEscapeAnalysis.AnalysisResult analyzeMethod(final BytecodeLinkerContext aLinkercontext, final ProgramDescriptor aProgramDescriptor) {
        final Program p = aProgramDescriptor.program;
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(aLinkercontext, new LLVMIntrinsics());

        final PointsToEscapeAnalysis analysis = new PointsToEscapeAnalysis(new ProgramDescriptorProvider() {
            @Override
            public ProgramDescriptor resolveStaticInvocation(final BytecodeObjectTypeRef aClass, final String aMethodName, final BytecodeMethodSignature aSignature) {
                final BytecodeLinkedClass theRequestedClass = aLinkercontext.resolveClass(aClass);
                final BytecodeMethod theRequestedMethod = theRequestedClass.getBytecodeClass().methodByNameAndSignatureOrNull(aMethodName, aSignature);
                final Program theProgram = theGenerator.generateFrom(theRequestedClass.getBytecodeClass(), theRequestedMethod);
                KnownOptimizer.LLVM.optimize(theProgram.getControlFlowGraph(), aLinkercontext);
                return new ProgramDescriptor(theRequestedClass, theRequestedMethod, theProgram);
            }

            @Override
            public ProgramDescriptor resolveConstructorInvocation(final BytecodeObjectTypeRef aClass, final BytecodeMethodSignature aSignature) {
                final BytecodeLinkedClass theRequestedClass = aLinkercontext.resolveClass(aClass);
                final BytecodeMethod theRequestedMethod = theRequestedClass.getBytecodeClass().methodByNameAndSignatureOrNull("<init>", aSignature);
                final Program theProgram = theGenerator.generateFrom(theRequestedClass.getBytecodeClass(), theRequestedMethod);
                KnownOptimizer.LLVM.optimize(theProgram.getControlFlowGraph(), aLinkercontext);
                return new ProgramDescriptor(theRequestedClass, theRequestedMethod, theProgram);
            }

            @Override
            public ProgramDescriptor resolveDirectInvocation(final BytecodeObjectTypeRef aClass, final String aMethodName, final BytecodeMethodSignature aSignature) {
                final BytecodeLinkedClass theRequestedClass = aLinkercontext.resolveClass(aClass);
                final BytecodeMethod theRequestedMethod = theRequestedClass.getBytecodeClass().methodByNameAndSignatureOrNull(aMethodName, aSignature);
                final Program theProgram = theGenerator.generateFrom(theRequestedClass.getBytecodeClass(), theRequestedMethod);
                KnownOptimizer.LLVM.optimize(theProgram.getControlFlowGraph(), aLinkercontext);
                return new ProgramDescriptor(theRequestedClass, theRequestedMethod, theProgram);
            }
        });

        return analysis.analyze(aProgramDescriptor);
    }

    <T> boolean containsNInstancesOf(final Collection<T> aCollection, final Class<? extends T> aType, final int aNumber) {
        return aCollection.stream().filter(t -> aType.isAssignableFrom(t.getClass())).count() == aNumber;
    }

    <T> boolean containsOneInstanceOf(final Collection<T> aCollection, final Class<? extends T> aType) {
        return containsNInstancesOf(aCollection, aType, 1);
    }

    <T,X> boolean containsNInstancesOf(final Collection<T> aCollection, final Class<X> aType, final Predicate<X> aPred, final int aNumber) {
        return aCollection.stream().filter(t -> aType.isAssignableFrom(t.getClass())).filter(t -> aPred.test((X) t)).count() == aNumber;
    }

    <T,X> boolean containsOneInstanceOf(final Collection<T> aCollection, final Class<X> aType, final Predicate<X> aPred) {
        return containsNInstancesOf(aCollection, aType, aPred, 1);
    }

    private Object method1(final Object a, final int b1, final Object k) {
        final Object b = a;
        final A c = new A(b);
        return c;
    }

    @Test
    public void testMethod1() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method1", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(2, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, NewObjectAndConstructExpression.class));
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_a".equals(t.getName())));

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForThis = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForA = result.argumentsFlowsFor(p.argumentAt(1));
        final Set<PointsToEscapeAnalysis.Scope> scopesForB1 = result.argumentsFlowsFor(p.argumentAt(2));
        final Set<PointsToEscapeAnalysis.Scope> scopesForK = result.argumentsFlowsFor(p.argumentAt(3));
        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertTrue(scopesForThis.isEmpty());
        assertEquals(1, scopesForA.size());
        assertTrue(containsOneInstanceOf(scopesForA, PointsToEscapeAnalysis.ReturnScope.class));
        assertNull(scopesForB1);
        assertTrue(scopesForK.isEmpty());

        assertEquals(2, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 1));
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.InvocationResultScope.class));
    }

    private Object method2(final Object a, final int b1, final Object k) {
        final A c = new A(a);
        return null;
    }

    @Test
    public void testMethod2() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method2", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertTrue(escapedValues.isEmpty());

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForThis = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForA = result.argumentsFlowsFor(p.argumentAt(1));
        final Set<PointsToEscapeAnalysis.Scope> scopesForB1 = result.argumentsFlowsFor(p.argumentAt(2));
        final Set<PointsToEscapeAnalysis.Scope> scopesForK = result.argumentsFlowsFor(p.argumentAt(3));
        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertTrue(scopesForThis.isEmpty());
        assertTrue(scopesForA.isEmpty());
        assertNull(scopesForB1);
        assertTrue(scopesForK.isEmpty());

        assertEquals(1, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.LocalScope.class));
    }

    private Object method3(final Object a, final int b1, final Object k) {
        ESCAPER = a;
        return null;
    }

    @Test
    public void testMethod3() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method3", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(1, escapedValues.size());
        assertTrue(escapedValues.get(0) instanceof Variable);
        assertEquals("_a", ((Variable) escapedValues.get(0)).getName());

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForThis = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForA = result.argumentsFlowsFor(p.argumentAt(1));
        final Set<PointsToEscapeAnalysis.Scope> scopesForB1 = result.argumentsFlowsFor(p.argumentAt(2));
        final Set<PointsToEscapeAnalysis.Scope> scopesForK = result.argumentsFlowsFor(p.argumentAt(3));
        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertTrue(scopesForThis.isEmpty());
        assertEquals(1, scopesForA.size());
        assertTrue(containsOneInstanceOf(scopesForA, PointsToEscapeAnalysis.StaticScope.class));
        assertNull(scopesForB1);
        assertTrue(scopesForK.isEmpty());

        assertEquals(1, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.LocalScope.class));
    }

    private Object method4(final Object a, final int b1, final Object k) {
        return new Object[b1];
    }

    @Test
    public void testMethod4() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method4", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(1, escapedValues.size());
        assertTrue(escapedValues.get(0) instanceof NewArrayExpression);

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForThis = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForA = result.argumentsFlowsFor(p.argumentAt(1));
        final Set<PointsToEscapeAnalysis.Scope> scopesForB1 = result.argumentsFlowsFor(p.argumentAt(2));
        final Set<PointsToEscapeAnalysis.Scope> scopesForK = result.argumentsFlowsFor(p.argumentAt(3));
        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertTrue(scopesForThis.isEmpty());
        assertTrue(scopesForA.isEmpty());
        assertNull(scopesForB1);
        assertTrue(scopesForK.isEmpty());

        assertEquals(1, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.LocalScope.class));
    }

    private Object method5(final Object a, final int b1, final Object k) {
        return new Object[b1][b1];
    }

    @Test
    public void testMethod5() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method5", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(1, escapedValues.size());
        assertTrue(escapedValues.get(0) instanceof NewMultiArrayExpression);

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForThis = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForA = result.argumentsFlowsFor(p.argumentAt(1));
        final Set<PointsToEscapeAnalysis.Scope> scopesForB1 = result.argumentsFlowsFor(p.argumentAt(2));
        final Set<PointsToEscapeAnalysis.Scope> scopesForK = result.argumentsFlowsFor(p.argumentAt(3));
        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertTrue(scopesForThis.isEmpty());
        assertTrue(scopesForA.isEmpty());
        assertNull(scopesForB1);
        assertTrue(scopesForK.isEmpty());

        assertEquals(1, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.LocalScope.class));
    }

    private Object method6(final Object a, final int b1, final Object k) {
        final Object[] x = new Object[b1];
        x[0] = this;
        return x;
    }

    @Test
    public void testMethod6() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method6", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(2, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, NewArrayExpression.class));
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class));

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForThis = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForA = result.argumentsFlowsFor(p.argumentAt(1));
        final Set<PointsToEscapeAnalysis.Scope> scopesForB1 = result.argumentsFlowsFor(p.argumentAt(2));
        final Set<PointsToEscapeAnalysis.Scope> scopesForK = result.argumentsFlowsFor(p.argumentAt(3));
        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertEquals(1, scopesForThis.size());
        assertTrue(containsOneInstanceOf(scopesForThis, PointsToEscapeAnalysis.ReturnScope.class));
        assertTrue(scopesForA.isEmpty());
        assertNull(scopesForB1);
        assertTrue(scopesForK.isEmpty());

        assertEquals(2, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.LocalScope.class));
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.ThisScope.class));
    }

    private Object method7(final Object a, final int b1, final Object k) {
        final A x = new A(a);
        x.o = k;
        return null;
    }

    @Test
    public void testMethod7() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method7", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertTrue(escapedValues.isEmpty());

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForThis = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForA = result.argumentsFlowsFor(p.argumentAt(1));
        final Set<PointsToEscapeAnalysis.Scope> scopesForB1 = result.argumentsFlowsFor(p.argumentAt(2));
        final Set<PointsToEscapeAnalysis.Scope> scopesForK = result.argumentsFlowsFor(p.argumentAt(3));
        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertTrue(scopesForThis.isEmpty());
        assertTrue(scopesForA.isEmpty());
        assertNull(scopesForB1);
        assertTrue(scopesForK.isEmpty());

        assertEquals(1, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.LocalScope.class));
    }

    private Object method8(final Object a, final int b1, final Object k) {
        Object o = null;
        for (int i=0; i < b1; i++) {
            if (i > 5) {
                o = a;
            } else if (i == 0) {
                o = null;
            } else {
                o = k;
            }
        }
        return o;
    }

    @Test
    public void testMethod8() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method8", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(2, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_a".equals(t.getName())));
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_k".equals(t.getName())));

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForThis = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForA = result.argumentsFlowsFor(p.argumentAt(1));
        final Set<PointsToEscapeAnalysis.Scope> scopesForB1 = result.argumentsFlowsFor(p.argumentAt(2));
        final Set<PointsToEscapeAnalysis.Scope> scopesForK = result.argumentsFlowsFor(p.argumentAt(3));
        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertTrue(scopesForThis.isEmpty());
        assertEquals(2, scopesForA.size());
        assertTrue(containsOneInstanceOf(scopesForA, PointsToEscapeAnalysis.ReturnScope.class));
        assertTrue(containsOneInstanceOf(scopesForA, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 3));
        assertNull(scopesForB1);
        assertEquals(2, scopesForK.size());
        assertTrue(containsOneInstanceOf(scopesForK, PointsToEscapeAnalysis.ReturnScope.class));
        assertTrue(containsOneInstanceOf(scopesForK, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 1));

        assertEquals(4, returning.size());
        assertTrue(containsNInstancesOf(returning, PointsToEscapeAnalysis.LocalScope.class, 2));
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 3));
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 1));
    }

    private Object method9(final Object a, final int b1, final Object k) {
        A o = null;
        for (int i=0; i < b1; i++) {
            o = (A) a;
        }
        o.o = k;
        return null;
    }

    @Test
    public void testMethod9() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method9", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(1, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_k".equals(t.getName())));

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForThis = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForA = result.argumentsFlowsFor(p.argumentAt(1));
        final Set<PointsToEscapeAnalysis.Scope> scopesForB1 = result.argumentsFlowsFor(p.argumentAt(2));
        final Set<PointsToEscapeAnalysis.Scope> scopesForK = result.argumentsFlowsFor(p.argumentAt(3));
        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertTrue(scopesForThis.isEmpty());
        assertTrue(scopesForA.isEmpty());
        assertNull(scopesForB1);
        assertEquals(1, scopesForK.size());
        assertTrue(containsOneInstanceOf(scopesForK, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 1));

        assertEquals(1, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.LocalScope.class));
    }

    private Object method10(final Object a, final int b1, final Object k) {
        A o = null;
        for (int i=0; i < b1; i++) {
            o = (A) a;
        }
        o.o = k;
        return o;
    }

    @Test
    public void testMethod10() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method10", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(2, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_a".equals(t.getName())));
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_k".equals(t.getName())));

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForThis = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForA = result.argumentsFlowsFor(p.argumentAt(1));
        final Set<PointsToEscapeAnalysis.Scope> scopesForB1 = result.argumentsFlowsFor(p.argumentAt(2));
        final Set<PointsToEscapeAnalysis.Scope> scopesForK = result.argumentsFlowsFor(p.argumentAt(3));
        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertTrue(scopesForThis.isEmpty());
        assertEquals(1, scopesForA.size());
        assertTrue(containsOneInstanceOf(scopesForA, PointsToEscapeAnalysis.ReturnScope.class));
        assertNull(scopesForB1);
        assertEquals(2, scopesForK.size());
        assertTrue(containsOneInstanceOf(scopesForK, PointsToEscapeAnalysis.ReturnScope.class));
        assertTrue(containsOneInstanceOf(scopesForK, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 1));

        assertEquals(3, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.LocalScope.class));
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 3));
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 1));
    }

    private Object method11(final Object a, final int b1, final Object k) {
        Object[] o = null;
        for (int i=0; i < b1; i++) {
            o = (Object[]) a;
        }
        o[0] = k;
        return null;
    }

    @Test
    public void testMethod11() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method11", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(1, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_k".equals(t.getName())));

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForThis = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForA = result.argumentsFlowsFor(p.argumentAt(1));
        final Set<PointsToEscapeAnalysis.Scope> scopesForB1 = result.argumentsFlowsFor(p.argumentAt(2));
        final Set<PointsToEscapeAnalysis.Scope> scopesForK = result.argumentsFlowsFor(p.argumentAt(3));
        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertTrue(scopesForThis.isEmpty());
        assertTrue(scopesForA.isEmpty());
        assertNull(scopesForB1);
        assertEquals(1, scopesForK.size());
        assertTrue(containsOneInstanceOf(scopesForK, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 1));

        assertEquals(1, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.LocalScope.class));
    }

    private Object method12(final Object a, final int b1, final Object k) {
        Object[] o = null;
        for (int i=0; i < b1; i++) {
            o = (Object[]) a;
        }
        o[0] = k;
        return o;
    }

    @Test
    public void testMethod12() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method12", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(2, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_a".equals(t.getName())));
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_k".equals(t.getName())));

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForThis = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForA = result.argumentsFlowsFor(p.argumentAt(1));
        final Set<PointsToEscapeAnalysis.Scope> scopesForB1 = result.argumentsFlowsFor(p.argumentAt(2));
        final Set<PointsToEscapeAnalysis.Scope> scopesForK = result.argumentsFlowsFor(p.argumentAt(3));
        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertTrue(scopesForThis.isEmpty());
        assertEquals(1, scopesForA.size());
        assertTrue(containsOneInstanceOf(scopesForA, PointsToEscapeAnalysis.ReturnScope.class));
        assertNull(scopesForB1);
        assertEquals(2, scopesForK.size());
        assertTrue(containsOneInstanceOf(scopesForK, PointsToEscapeAnalysis.ReturnScope.class));
        assertTrue(containsOneInstanceOf(scopesForK, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 1));

        assertEquals(3, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.LocalScope.class));
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 1));
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 3));
    }

    private Object method13(final Object a, final int b1, final Object k) {
        A x = (A) a;
        Object b = x.o;
        A y = (A) k;
        y.o = b;
        return b;
    }

    @Test
    public void testMethod13() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method13", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(1, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_a".equals(t.getName())));

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForThis = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForA = result.argumentsFlowsFor(p.argumentAt(1));
        final Set<PointsToEscapeAnalysis.Scope> scopesForB1 = result.argumentsFlowsFor(p.argumentAt(2));
        final Set<PointsToEscapeAnalysis.Scope> scopesForK = result.argumentsFlowsFor(p.argumentAt(3));
        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertTrue(scopesForThis.isEmpty());
        assertEquals(2, scopesForA.size());
        assertTrue(containsOneInstanceOf(scopesForA, PointsToEscapeAnalysis.ReturnScope.class));
        assertTrue(containsOneInstanceOf(scopesForA, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 3));
        assertNull(scopesForB1);
        assertTrue(scopesForK.isEmpty());

        assertEquals(1, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 1));
    }

    private Object method14(final Object a, final int b1, final Object k) {
        return ESCAPER;
    }

    @Test
    public void testMethod14() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method14", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertTrue(escapedValues.isEmpty());

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForThis = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForA = result.argumentsFlowsFor(p.argumentAt(1));
        final Set<PointsToEscapeAnalysis.Scope> scopesForB1 = result.argumentsFlowsFor(p.argumentAt(2));
        final Set<PointsToEscapeAnalysis.Scope> scopesForK = result.argumentsFlowsFor(p.argumentAt(3));
        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertTrue(scopesForThis.isEmpty());
        assertTrue(scopesForA.isEmpty());
        assertNull(scopesForB1);
        assertTrue(scopesForK.isEmpty());

        assertEquals(1, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.StaticScope.class));
    }

    enum TestEnum {
        value;
    }

    @Test
    public void testEnumInit() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeStaticMethod(TestEnum.class, "<clinit>", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID,
                new BytecodeTypeRef[]{}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(2, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, NewObjectAndConstructExpression.class));
        assertTrue(containsOneInstanceOf(escapedValues, NewArrayExpression.class));

        final Program p = result.program();

        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();
        assertTrue(returning.isEmpty());
    }

    private Object method15(final Object a, final int b1, final Object k) {
        A a1 = new A(a);
        A a2 = new A(a1);
        return null;
    }

    @Test
    public void testMethod15() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method15", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertTrue(escapedValues.isEmpty());

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForThis = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForA = result.argumentsFlowsFor(p.argumentAt(1));
        final Set<PointsToEscapeAnalysis.Scope> scopesForB1 = result.argumentsFlowsFor(p.argumentAt(2));
        final Set<PointsToEscapeAnalysis.Scope> scopesForK = result.argumentsFlowsFor(p.argumentAt(3));
        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertTrue(scopesForThis.isEmpty());
        assertTrue(scopesForA.isEmpty());
        assertNull(scopesForB1);
        assertTrue(scopesForK.isEmpty());

        assertEquals(1, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.LocalScope.class));
    }
}