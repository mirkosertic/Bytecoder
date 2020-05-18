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
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
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

    private Object method1(final Object a, final int b1, final Object k) {
        final Object b = a;
        final A c = new A(b);
        return c;
    }

    private Object method2(final Object a, final int b1, final Object k) {
        final A c = new A(a);
        return null;
    }

    private Object method3(final Object a, final int b1, final Object k) {
        ESCAPER = a;
        return null;
    }

    private Object method4(final Object a, final int b1, final Object k) {
        return new Object[b1];
    }

    private Object method5(final Object a, final int b1, final Object k) {
        return new Object[b1][b1];
    }

    private Object method6(final Object a, final int b1, final Object k) {
        final Object[] x = new Object[b1];
        x[0] = this;
        return x;
    }

    private Object method7(final Object a, final int b1, final Object k) {
        final A x = new A(a);
        x.o = k;
        return null;
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

    private PointsToEscapeAnalysis.AnalysisResult analyze(final Class aClazz, final String methodName, final BytecodeMethodSignature aSignature) {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new LLVMIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(aClazz));

        theLinkedClass.resolveVirtualMethod(methodName, aSignature);
        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull(methodName, aSignature);
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

        KnownOptimizer.LLVM.optimize(p.getControlFlowGraph(), theLinkerContext);

        final PointsToEscapeAnalysis analysis = new PointsToEscapeAnalysis(new ProgramDescriptorProvider() {
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
        });

        return analysis.analyze(new ProgramDescriptor(theLinkedClass, theMethod, p));
    }

    <T> boolean containsOneInstanceOf(final Collection<T> aCollection, final Class<? extends T> aType) {
        return aCollection.stream().filter(t -> aType.isAssignableFrom(t.getClass())).count() == 1;
    }

    <T,X> boolean containsOneInstanceOf(final Collection<T> aCollection, final Class<X> aType, final Predicate<X> aPred) {
        return aCollection.stream().filter(t -> aType.isAssignableFrom(t.getClass())).filter(t -> aPred.test((X) t)).count() == 1;
    }

    @Test
    public void testMethod1() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyze(getClass(), "method1", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(2, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, NewObjectAndConstructExpression.class));
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_a".equals(t.getName())));
    }

    @Test
    public void testMethod2() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyze(getClass(), "method2", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(1, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_a".equals(t.getName())));
    }

    @Test
    public void testMethod3() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyze(getClass(), "method3", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(1, escapedValues.size());
        assertTrue(escapedValues.get(0) instanceof Variable);
        assertEquals("_a", ((Variable) escapedValues.get(0)).getName());
    }

    @Test
    public void testMethod4() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyze(getClass(), "method4", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(1, escapedValues.size());
        assertTrue(escapedValues.get(0) instanceof NewArrayExpression);
    }

    @Test
    public void testMethod5() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyze(getClass(), "method5", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(1, escapedValues.size());
        assertTrue(escapedValues.get(0) instanceof NewMultiArrayExpression);
    }

    @Test
    public void testMethod6() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyze(getClass(), "method6", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(2, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, NewArrayExpression.class));
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class));
    }

    @Test
    public void testMethod7() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyze(getClass(), "method7", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(2, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_a".equals(t.getName())));
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_k".equals(t.getName())));
    }

    @Test
    public void testMethod8() {
        final PointsToEscapeAnalysis.AnalysisResult result = analyze(getClass(), "method8", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));
        result.printDebugDotTree();
        final List<Value> escapedValues = new ArrayList<>(result.escapedValues());
        assertEquals(2, escapedValues.size());
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_a".equals(t.getName())));
        assertTrue(containsOneInstanceOf(escapedValues, Variable.class, t -> "_k".equals(t.getName())));
    }
}