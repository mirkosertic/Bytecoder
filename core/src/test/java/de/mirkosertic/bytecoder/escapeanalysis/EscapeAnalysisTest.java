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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.Test;

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
import de.mirkosertic.bytecoder.pointsto.AllocationSymbol;
import de.mirkosertic.bytecoder.pointsto.PointsToAnalysis;
import de.mirkosertic.bytecoder.pointsto.PointsToAnalysisResult;
import de.mirkosertic.bytecoder.ssa.NaiveProgramGenerator;
import de.mirkosertic.bytecoder.ssa.NewArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewInstanceAndConstructExpression;
import de.mirkosertic.bytecoder.ssa.NewMultiArrayExpression;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramDescriptor;
import de.mirkosertic.bytecoder.ssa.ProgramDescriptorProvider;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.ValueWithEscapeCheck;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;

public class EscapeAnalysisTest {

    final static BytecodeObjectTypeRef OBJECT_TYPE_REF = BytecodeObjectTypeRef.fromRuntimeClass(Object.class);

    public static class A {
        Object o;

        public A(final Object o) {
            this.o = o;
        }

        public A() {
            this.o = o;
        }
    }

    public static class B {
        Object o;

        public B(final Object o) {
            this.o = o;
        }
    }

    private static Object ESCAPER;

    private PointsToAnalysisResult analyzeVirtualMethod(final Class aClazz, final String aMethodName, final BytecodeMethodSignature aSignature) {
        return analyzeVirtualMethod(BytecodeObjectTypeRef.fromRuntimeClass(aClazz), aMethodName, aSignature);
    }

    private PointsToAnalysisResult analyzeVirtualMethod(final BytecodeObjectTypeRef aClazz, final String aMethodName, final BytecodeMethodSignature aSignature) {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new LLVMIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(aClazz);

        theLinkedClass.resolveVirtualMethod(aMethodName, aSignature);
        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull(aMethodName, aSignature);
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);
        KnownOptimizer.LLVM.optimize(p.getControlFlowGraph(), theLinkerContext);

        return analyzeMethod(theLinkerContext, new ProgramDescriptor(theLinkedClass, theMethod, p));
    }

    private PointsToAnalysisResult analyzeStaticMethod(final Class aClazz, final String aMethodName, final BytecodeMethodSignature aSignature) {
        return analyzeStaticMethod(BytecodeObjectTypeRef.fromRuntimeClass(aClazz), aMethodName, aSignature);
    }

    private PointsToAnalysisResult analyzeStaticMethod(final BytecodeObjectTypeRef aClazz, final String aMethodName, final BytecodeMethodSignature aSignature) {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new LLVMIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(aClazz);

        theLinkedClass.resolveStaticMethod(aMethodName, aSignature);
        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull(aMethodName, aSignature);
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);
        KnownOptimizer.LLVM.optimize(p.getControlFlowGraph(), theLinkerContext);

        return analyzeMethod(theLinkerContext, new ProgramDescriptor(theLinkedClass, theMethod, p));
    }

    private PointsToAnalysisResult analyzeMethod(final BytecodeLinkerContext aLinkercontext, final ProgramDescriptor aProgramDescriptor) {
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(aLinkercontext, new LLVMIntrinsics());

        final PointsToAnalysis analysis = new PointsToAnalysis(new ProgramDescriptorProvider() {
            @Override
            public ProgramDescriptor resolveStaticInvocation(final BytecodeObjectTypeRef aClass, final String aMethodName, final BytecodeMethodSignature aSignature) {
                BytecodeLinkedClass theRequestedClass = aLinkercontext.resolveClass(aClass);
                BytecodeMethod theRequestedMethod = theRequestedClass.getBytecodeClass().methodByNameAndSignatureOrNull(aMethodName, aSignature);
                while (theRequestedMethod == null) {
                    theRequestedClass = theRequestedClass.getSuperClass();
                    theRequestedMethod = theRequestedClass.getBytecodeClass().methodByNameAndSignatureOrNull(aMethodName, aSignature);
                }
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

        final PointsToAnalysisResult analysisResult = analysis.analyze(aProgramDescriptor);
        final EscapeAnalysis escapeAnalysis = new EscapeAnalysis();
        escapeAnalysis.analyze(aLinkercontext.getLogger(), aProgramDescriptor, analysisResult);

        return analysisResult;
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
        return new A(a);
    }

    @Test
    public void testMethod1() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method1", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<AllocationSymbol> allocationSymbols = result.allocationSymbols();
        assertEquals(1, allocationSymbols.size());
        assertTrue(containsOneInstanceOf(allocationSymbols, AllocationSymbol.class, t -> t.value() instanceof NewInstanceAndConstructExpression && ((ValueWithEscapeCheck) (t.value())).isEscaping()));
    }

    private Object method2(final Object a, final int b1, final Object k) {
        return new A[10];
    }

    @Test
    public void testMethod2() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method2", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<AllocationSymbol> allocationSymbols = result.allocationSymbols();
        assertEquals(1, allocationSymbols.size());
        assertTrue(containsOneInstanceOf(allocationSymbols, AllocationSymbol.class, t -> t.value() instanceof NewArrayExpression && ((ValueWithEscapeCheck) (t.value())).isEscaping()));
    }

    private Object method3(final Object a, final int b1, final Object k) {
        return new A[10][10];
    }

    @Test
    public void testMethod3() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method3", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<AllocationSymbol> allocationSymbols = result.allocationSymbols();
        assertEquals(1, allocationSymbols.size());
        assertTrue(containsOneInstanceOf(allocationSymbols, AllocationSymbol.class, t -> t.value() instanceof NewMultiArrayExpression));
    }

    private Object method4(final Object a, final int b1, final Object k) {
        ESCAPER = new A(null);
        return null;
    }

    @Test
    public void testMethod4() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method4", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<AllocationSymbol> allocationSymbols = result.allocationSymbols();
        assertEquals(1, allocationSymbols.size());
        assertTrue(containsOneInstanceOf(allocationSymbols, AllocationSymbol.class, t -> t.value() instanceof NewInstanceAndConstructExpression && ((ValueWithEscapeCheck) (t.value())).isEscaping()));
    }

    private Object method5(final Object a, final int b1, final Object k) {
        A x = new A(null);
        return null;
    }

    @Test
    public void testMethod5() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method5", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<AllocationSymbol> allocationSymbols = result.allocationSymbols();
        assertEquals(1, allocationSymbols.size());
        assertTrue(containsOneInstanceOf(allocationSymbols, AllocationSymbol.class, t -> t.value() instanceof NewInstanceAndConstructExpression && !((ValueWithEscapeCheck) (t.value())).isEscaping()));
    }

    private Object method6(final Object a, final int b1, final Object k) {
        A y = (A) a;
        y.o = new A(null);
        y.o = new A(null);
        return null;
    }

    @Test
    public void testMethod6() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method6", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<AllocationSymbol> allocationSymbols = result.allocationSymbols();
        assertEquals(2, allocationSymbols.size());
        assertTrue(containsNInstancesOf(allocationSymbols, AllocationSymbol.class, t -> t.value() instanceof NewInstanceAndConstructExpression && ((ValueWithEscapeCheck) (t.value())).isEscaping(), 2));
    }

    private static void saveToStatic(final Object o) {
        if (o != null) {
            ESCAPER = o;
        }
    }

    private Object method7(final Object a, final int b1, final Object k) {
        saveToStatic(new A(null));
        return null;
    }

    @Test
    public void testMethod7() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method7", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<AllocationSymbol> allocationSymbols = result.allocationSymbols();
        assertEquals(1, allocationSymbols.size());
        assertTrue(containsOneInstanceOf(allocationSymbols, AllocationSymbol.class, t -> t.value() instanceof NewInstanceAndConstructExpression && ((ValueWithEscapeCheck) (t.value())).isEscaping()));
    }

    private Object method8(final Object a, final int b1, final Object k) {
        ESCAPER = new B(new A(null));
        return null;
    }

    @Test
    public void testMethod8() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method8", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<AllocationSymbol> allocationSymbols = result.allocationSymbols();
        assertEquals(2, allocationSymbols.size());
        assertTrue(containsNInstancesOf(allocationSymbols, AllocationSymbol.class, t -> t.value() instanceof NewInstanceAndConstructExpression && ((ValueWithEscapeCheck) (t.value())).isEscaping(), 2));
    }


    private static <T> T requireNonNull(T obj) {
        if (obj == null)
            throw new NullPointerException();
        return obj;
    }

    private Object method9(final Object a, final int b1, final Object k) {
        ESCAPER = requireNonNull(new A());
        return null;
    }

    @Test
    public void testMethod9() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method9", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<AllocationSymbol> allocationSymbols = result.allocationSymbols();
        assertEquals(1, allocationSymbols.size());
        assertTrue(containsOneInstanceOf(allocationSymbols, AllocationSymbol.class, t -> t.value() instanceof NewInstanceAndConstructExpression && ((ValueWithEscapeCheck) (t.value())).isEscaping()));
    }

    private Object method10(final Object a, final int b1, final Object k) {
        ESCAPER = new PrintStream(new FileOutputStream((FileDescriptor) null), false);
        return null;
    }

    @Test
    public void testMethod10() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method10", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<AllocationSymbol> allocationSymbols = result.allocationSymbols();
        assertEquals(2, allocationSymbols.size());
        assertTrue(containsNInstancesOf(allocationSymbols, AllocationSymbol.class, t -> t.value() instanceof NewInstanceAndConstructExpression && ((ValueWithEscapeCheck) (t.value())).isEscaping(), 2));
    }

}