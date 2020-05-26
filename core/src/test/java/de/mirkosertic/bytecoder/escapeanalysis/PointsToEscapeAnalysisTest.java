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

import de.mirkosertic.bytecoder.api.OpaqueReferenceType;
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
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;
import de.mirkosertic.bytecoder.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.ssa.NaiveProgramGenerator;
import de.mirkosertic.bytecoder.ssa.NewArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewInstanceAndConstructExpression;
import de.mirkosertic.bytecoder.ssa.NewMultiArrayExpression;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
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

    private PointsToEscapeAnalysis.AnalysisResult analyzeVirtualMethod(final Class aClazz, final String aMethodName, final BytecodeMethodSignature aSignature) {
        return analyzeVirtualMethod(BytecodeObjectTypeRef.fromRuntimeClass(aClazz), aMethodName, aSignature);
    }

    private PointsToEscapeAnalysis.AnalysisResult analyzeVirtualMethod(final BytecodeObjectTypeRef aClazz, final String aMethodName, final BytecodeMethodSignature aSignature) {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new LLVMIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(aClazz);

        theLinkedClass.resolveVirtualMethod(aMethodName, aSignature);
        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull(aMethodName, aSignature);
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);
        KnownOptimizer.LLVM.optimize(p.getControlFlowGraph(), theLinkerContext);

        return analyzeMethod(theLinkerContext, new ProgramDescriptor(theLinkedClass, theMethod, p));
    }

    private PointsToEscapeAnalysis.AnalysisResult analyzeStaticMethod(final Class aClazz, final String aMethodName, final BytecodeMethodSignature aSignature) {
        return analyzeStaticMethod(BytecodeObjectTypeRef.fromRuntimeClass(aClazz), aMethodName, aSignature);
    }

    private PointsToEscapeAnalysis.AnalysisResult analyzeStaticMethod(final BytecodeObjectTypeRef aClazz, final String aMethodName, final BytecodeMethodSignature aSignature) {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new LLVMIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(aClazz);

        theLinkedClass.resolveStaticMethod(aMethodName, aSignature);
        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull(aMethodName, aSignature);
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
                if (theRequestedMethod.getAccessFlags().isNative()) {
                    return new ProgramDescriptor(theRequestedClass, theRequestedMethod, null);
                }
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
                if (theRequestedMethod.getAccessFlags().isNative()) {
                    return new ProgramDescriptor(theRequestedClass, theRequestedMethod, null);
                }
                final Program theProgram = theGenerator.generateFrom(theRequestedClass.getBytecodeClass(), theRequestedMethod);
                KnownOptimizer.LLVM.optimize(theProgram.getControlFlowGraph(), aLinkercontext);
                return new ProgramDescriptor(theRequestedClass, theRequestedMethod, theProgram);
            }
        }, aLinkercontext.getLogger());

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
        assertTrue(containsOneInstanceOf(escapedValues, NewInstanceAndConstructExpression.class));
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
        assertEquals(2, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_k".equals(t.getName())));
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_a".equals(t.getName())));

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForThis = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForA = result.argumentsFlowsFor(p.argumentAt(1));
        final Set<PointsToEscapeAnalysis.Scope> scopesForB1 = result.argumentsFlowsFor(p.argumentAt(2));
        final Set<PointsToEscapeAnalysis.Scope> scopesForK = result.argumentsFlowsFor(p.argumentAt(3));
        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertTrue(scopesForThis.isEmpty());
        assertEquals(1, scopesForA.size());
        assertTrue(containsOneInstanceOf(scopesForA, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 3));
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
        assertEquals(2, scopesForA.size());
        assertTrue(containsOneInstanceOf(scopesForA, PointsToEscapeAnalysis.ReturnScope.class));
        assertTrue(containsOneInstanceOf(scopesForA, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 3));
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
        assertEquals(2, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_k".equals(t.getName())));
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_a".equals(t.getName())));

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForThis = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForA = result.argumentsFlowsFor(p.argumentAt(1));
        final Set<PointsToEscapeAnalysis.Scope> scopesForB1 = result.argumentsFlowsFor(p.argumentAt(2));
        final Set<PointsToEscapeAnalysis.Scope> scopesForK = result.argumentsFlowsFor(p.argumentAt(3));
        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertTrue(scopesForThis.isEmpty());
        assertEquals(1, scopesForA.size());
        assertTrue(containsOneInstanceOf(scopesForA, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 3));
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
        assertEquals(2, scopesForA.size());
        assertTrue(containsOneInstanceOf(scopesForA, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 3));
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
        value
    }

    @Test
    public void testEnumInit() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeStaticMethod(TestEnum.class, "<clinit>", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID,
                new BytecodeTypeRef[]{}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(2, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, NewInstanceAndConstructExpression.class));
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

    class B {
        B(A a1, A a2) {
            a1.o = a2;
        }
    }

    private Object method16(final Object a, final int b1, final Object k) {
        A a1 = new A(null);
        A a2 = new A(null);
        new B(a1, a2);
        return a1;
    }

    @Test
    public void testMethod16() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method16", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(2, escapedValues.size());
        assertTrue(containsNInstancesOf(escapedValues, NewInstanceAndConstructExpression.class, 2));

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

        assertEquals(4, returning.size());
        assertTrue(containsNInstancesOf(returning, PointsToEscapeAnalysis.LocalScope.class, 2));
        assertTrue(containsNInstancesOf(returning, PointsToEscapeAnalysis.InvocationResultScope.class, 2));
    }

    private Object method17(final Object a, final int b1, final Object k) {
        A a1 = new A(null);
        A a2 = new A(null);
        new B(a1, a2);
        return a2;
    }

    @Test
    public void testMethod17() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method17", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(1, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, NewInstanceAndConstructExpression.class));

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

        assertEquals(2, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.LocalScope.class));
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.InvocationResultScope.class));
    }

    private static Object staticCopy(final int b1, final Object o1) {
        return o1;
    }

    private Object method18(final Object a, final int b1, final Object k) {
        A a1 = new A(null);
        return staticCopy(b1, a1);
    }

    @Test
    public void testMethod18() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method18", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(1, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, NewInstanceAndConstructExpression.class));

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

        assertEquals(3, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.LocalScope.class));
        assertTrue(containsNInstancesOf(returning, PointsToEscapeAnalysis.InvocationResultScope.class, 2));
    }

    private Object methodCopy(final int b1, final Object o1) {
        return o1;
    }

    private Object method19(final Object a, final int b1, final Object k) {
        A a1 = new A(null);
        return methodCopy(b1, a1);
    }

    @Test
    public void testMethod19() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method19", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(1, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, NewInstanceAndConstructExpression.class));

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

        assertEquals(3, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.LocalScope.class));
        assertTrue(containsNInstancesOf(returning, PointsToEscapeAnalysis.InvocationResultScope.class, 2));
    }

    static abstract class Base {
        abstract void doIt(final Object o, final int a);
    }

    static class Impl1 extends Base {

        @Override
        void doIt(Object o, int a) {
        }
    }

    static class Impl2 extends Base {
        @Override
        void doIt(Object o, int a) {
        }
    }

    private Object method20(final Object a, final int b1, final Object k) {
        A a1 = new A(null);
        Base test1 = new Impl1();
        Base test2 = new Impl2();
        test1.doIt(a1, b1);
        return null;
    }

    @Test
    public void testMethod20() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method20", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(2, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, NewInstanceAndConstructExpression.class, t -> t.getClazz().name().equals(Impl1.class.getName())));
        assertTrue(containsOneInstanceOf(escapedValues, NewInstanceAndConstructExpression.class, t -> t.getClazz().name().equals(A.class.getName())));

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

    private Object method21(final Object a, final int b1, final Object k) {
        return method21(a, b1, k);
    }

    @Test
    public void testMethod21() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method21", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(3, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> t.isSynthetic() && t.getName().equals("_a")));
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> t.isSynthetic() && t.getName().equals("_k")));
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> t.isSynthetic() && t.getName().equals("__tr")));

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForThis = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForA = result.argumentsFlowsFor(p.argumentAt(1));
        final Set<PointsToEscapeAnalysis.Scope> scopesForB1 = result.argumentsFlowsFor(p.argumentAt(2));
        final Set<PointsToEscapeAnalysis.Scope> scopesForK = result.argumentsFlowsFor(p.argumentAt(3));
        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertEquals(3, scopesForThis.size());
        assertTrue(containsOneInstanceOf(scopesForThis, PointsToEscapeAnalysis.StaticScope.class));
        assertTrue(containsOneInstanceOf(scopesForThis, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 3));
        assertTrue(containsOneInstanceOf(scopesForThis, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 1));

        assertEquals(3, scopesForA.size());
        assertTrue(containsOneInstanceOf(scopesForA, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 3));
        assertTrue(containsOneInstanceOf(scopesForA, PointsToEscapeAnalysis.StaticScope.class));
        assertTrue(containsOneInstanceOf(scopesForA, PointsToEscapeAnalysis.ThisScope.class));

        assertNull(scopesForB1);

        assertEquals(3, scopesForK.size());
        assertTrue(containsOneInstanceOf(scopesForK, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 1));
        assertTrue(containsOneInstanceOf(scopesForK, PointsToEscapeAnalysis.StaticScope.class));
        assertTrue(containsOneInstanceOf(scopesForK, PointsToEscapeAnalysis.ThisScope.class));

        assertEquals(1, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.InvocationResultScope.class));
    }

    static abstract class X implements OpaqueReferenceType {

        public static native X current(Object something);

        public abstract void doSomethingWith(final Object o);
    }

    private Object method22(final Object a, final int b1, final Object k) {
        final X x = X.current(null);
        x.doSomethingWith(a);
        return k;
    }

    @Test
    public void testMethod22() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method22", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(2, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> t.isSynthetic() && t.getName().equals("_a")));
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> t.isSynthetic() && t.getName().equals("_k")));

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

        assertEquals(1, scopesForK.size());
        assertTrue(containsOneInstanceOf(scopesForK, PointsToEscapeAnalysis.ReturnScope.class));

        assertEquals(1, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 3));
    }

    private Object method23(final Object a, final int b1, final Object k) {
        throw (RuntimeException) a;
    }

    @Test
    public void testMethod23() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method23", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(1, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> t.isSynthetic() && t.getName().equals("_a")));

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

        assertEquals(1, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 1));
    }

    private Object method24(final Object a, final int b1, final Object k) throws IllegalAccessException, InstantiationException {
        return ((Class)a).newInstance();
    }

    @Test
    public void testMethod24() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method24", new BytecodeMethodSignature(OBJECT_TYPE_REF,
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
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.InvocationResultScope.class));
    }

    private Object method25(final Object a, final int b1, final Object k) {
        System.arraycopy(a, 0, k, 0, 10);
        return null;
    }

    @Test
    public void testMethod25() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method25", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(2, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> t.getName().equals("_a")));
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> t.getName().equals("_k")));

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForThis = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForA = result.argumentsFlowsFor(p.argumentAt(1));
        final Set<PointsToEscapeAnalysis.Scope> scopesForB1 = result.argumentsFlowsFor(p.argumentAt(2));
        final Set<PointsToEscapeAnalysis.Scope> scopesForK = result.argumentsFlowsFor(p.argumentAt(3));

        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertTrue(scopesForThis.isEmpty());

        assertEquals(1, scopesForA.size());
        assertTrue(containsOneInstanceOf(scopesForA, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 3));

        assertNull(scopesForB1);
        assertEquals(1, scopesForK.size());
        assertTrue(containsOneInstanceOf(scopesForK, PointsToEscapeAnalysis.MethodParameterScope.class, t -> t.parameterIndex() == 1));

        assertEquals(1, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.LocalScope.class));
    }

    @Test
    public void testMethod26() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeStaticMethod(BytecodeObjectTypeRef.fromUtf8Constant(new BytecodeUtf8Constant("java/lang/ConditionalSpecialCasing")), "lookUpTable",
                new BytecodeMethodSignature(
                        new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.CHAR, 1),
                new BytecodeTypeRef[]{
                        BytecodeObjectTypeRef.fromRuntimeClass(String.class),
                        BytecodePrimitiveTypeRef.INT,
                        BytecodeObjectTypeRef.fromRuntimeClass(Locale.class),
                        BytecodePrimitiveTypeRef.BOOLEAN}));

        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(2, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> t.getName().equals("_src")));
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> t.getName().equals("_locale")));

        final Program p = result.program();
        final Set<PointsToEscapeAnalysis.Scope> scopesForSrc = result.argumentsFlowsFor(p.argumentAt(0));
        final Set<PointsToEscapeAnalysis.Scope> scopesForLocale = result.argumentsFlowsFor(p.argumentAt(2));

        final Set<PointsToEscapeAnalysis.Scope> returning = result.returnFlows();

        assertTrue(scopesForSrc.isEmpty());
        assertTrue(scopesForLocale.isEmpty());

        assertEquals(4, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.LocalScope.class));
        assertTrue(containsNInstancesOf(returning, PointsToEscapeAnalysis.InvocationResultScope.class, 3));
    }

    private Object method27(final Object a, final int b1, final Object k) {
        return a.getClass();
    }

    @Test
    public void testMethod27() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method27", new BytecodeMethodSignature(OBJECT_TYPE_REF,
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

    private Object method28(final Object a, final int b1, final Object k) {
        A x = (A) (Object) this;
        while (x != null) {
            x = (A) x.o;
        }
        return x;
    }

    @Test
    public void testMethod28() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyzeVirtualMethod(getClass(), "method28", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(1, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> t.getName().equals("__tr")));

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

        assertEquals(1, returning.size());
        assertTrue(containsOneInstanceOf(returning, PointsToEscapeAnalysis.ThisScope.class));
    }
}