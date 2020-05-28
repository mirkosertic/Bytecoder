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
package de.mirkosertic.bytecoder.pointsto;

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
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramDescriptor;
import de.mirkosertic.bytecoder.ssa.ProgramDescriptorProvider;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

public class PointsToAnalysisTest {

    final static BytecodeObjectTypeRef OBJECT_TYPE_REF = BytecodeObjectTypeRef.fromRuntimeClass(Object.class);

    public static class A {
        Object o;

        public A(final Object o) {
            this.o = o;
        }
    }

    private static Object ESCAPER;

    enum Enum {
        v1
    }

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
        final Program p = aProgramDescriptor.program();
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(aLinkercontext, new LLVMIntrinsics());

        final PointsToAnalysis analysis = new PointsToAnalysis(new ProgramDescriptorProvider() {
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

    private Object method1(final Object a, final int b1, final Object k) {
        final Object b = a;
        final Object c = b;
        return c;
    }

    @Test
    public void testMethod1() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method1", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<Symbol> returningSymbols = result.returningSymbols();
        Assert.assertEquals(1, returningSymbols.size());

        final Set<Symbol> pointsTo = result.resolvedPointsToFor(returningSymbols.iterator().next());
        Assert.assertEquals(1, pointsTo.size());
        Assert.assertTrue(containsOneInstanceOf(pointsTo, ParamPref.class, t -> t.index() == 1));
    }

    private Object method2(final Object a, final int b1, final Object k) {
        Object b = a;
        for (int i=0;i<10;i++) {
            if (i > 5) {
                b = k;
            }
        }
        return b;
    }

    @Test
    public void testMethod2() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method2", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<Symbol> returningSymbols = result.returningSymbols();
        Assert.assertEquals(1, returningSymbols.size());

        final Set<Symbol> pointsTo = result.resolvedPointsToFor(returningSymbols.iterator().next());
        Assert.assertEquals(2, pointsTo.size());
        Assert.assertTrue(containsOneInstanceOf(pointsTo, ParamPref.class, t -> t.index() == 1));
        Assert.assertTrue(containsOneInstanceOf(pointsTo, ParamPref.class, t -> t.index() == 3));
    }

    private Object method3(final Object a, final int b1, final Object k) {
        Object b = null;
        Object c = "Hello";
        Object d = String.class;
        Object e = String.class.getSuperclass();
        return b;
    }

    @Test
    public void testMethod3() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method3", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<Symbol> returningSymbols = result.returningSymbols();
        Assert.assertEquals(1, returningSymbols.size());

        final Set<Symbol> pointsTo = result.resolvedPointsToFor(returningSymbols.iterator().next());
        Assert.assertEquals(1, pointsTo.size());
        Assert.assertTrue(containsOneInstanceOf(pointsTo, GlobalSymbols.class, t -> t == GlobalSymbols.staticScope));
    }

    private Object method4(final Object a, final int b1, final Object k) {
        Object[] b = (Object[]) a;
        b[1] = k;
        return b;
    }

    @Test
    public void testMethod4() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method4", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<Symbol> returningSymbols = result.returningSymbols();
        Assert.assertEquals(1, returningSymbols.size());

        final Set<Symbol> pointsTo = result.resolvedPointsToFor(returningSymbols.iterator().next());
        Assert.assertEquals(2, pointsTo.size());
        Assert.assertTrue(containsOneInstanceOf(pointsTo, ParamPref.class, t -> t.index() == 1));
        Assert.assertTrue(containsOneInstanceOf(pointsTo, ParamPref.class, t -> t.index() == 3));
    }

    private Object method5(final Object a, final int b1, final Object k) {
        Object[] b = (Object[]) a;
        return b[0];
    }

    @Test
    public void testMethod5() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method5", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<Symbol> returningSymbols = result.returningSymbols();
        Assert.assertEquals(1, returningSymbols.size());

        final Set<Symbol> pointsTo = result.resolvedPointsToFor(returningSymbols.iterator().next());
        Assert.assertEquals(1, pointsTo.size());
        Assert.assertTrue(containsOneInstanceOf(pointsTo, ParamPref.class, t -> t.index() == 1));
    }

    private Object method6(final Object a, final int b1, final Object k) {
        A b = (A) a;
        b.o = k;
        return b;
    }

    @Test
    public void testMethod6() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method6", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<Symbol> returningSymbols = result.returningSymbols();
        Assert.assertEquals(1, returningSymbols.size());

        final Set<Symbol> pointsTo = result.resolvedPointsToFor(returningSymbols.iterator().next());
        Assert.assertEquals(2, pointsTo.size());
        Assert.assertTrue(containsOneInstanceOf(pointsTo, ParamPref.class, t -> t.index() == 1));
        Assert.assertTrue(containsOneInstanceOf(pointsTo, ParamPref.class, t -> t.index() == 3));
    }

    private Object method7(final Object a, final int b1, final Object k) {
        A b = (A) a;
        return b.o;
    }

    @Test
    public void testMethod7() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method7", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<Symbol> returningSymbols = result.returningSymbols();
        Assert.assertEquals(1, returningSymbols.size());

        final Set<Symbol> pointsTo = result.resolvedPointsToFor(returningSymbols.iterator().next());
        Assert.assertEquals(1, pointsTo.size());
        Assert.assertTrue(containsOneInstanceOf(pointsTo, ParamPref.class, t -> t.index() == 1));
    }

    private Object method8(final Object a, final int b1, final Object k) {
        return ESCAPER;
    }

    @Test
    public void testMethod8() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method8", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<Symbol> returningSymbols = result.returningSymbols();
        Assert.assertEquals(1, returningSymbols.size());

        final Set<Symbol> pointsTo = result.resolvedPointsToFor(returningSymbols.iterator().next());
        Assert.assertEquals(1, pointsTo.size());
        Assert.assertTrue(containsOneInstanceOf(pointsTo, GlobalSymbols.class, t -> t == GlobalSymbols.staticScope));
    }

    private Object method9(final Object a, final int b1, final Object k) {
        ESCAPER = a;
        return k;
    }

    @Test
    public void testMethod9() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method9", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<Symbol> returningSymbols = result.returningSymbols();
        Assert.assertEquals(1, returningSymbols.size());

        final Set<Symbol> pointsTo = result.resolvedPointsToFor(returningSymbols.iterator().next());
        Assert.assertEquals(1, pointsTo.size());
        Assert.assertTrue(containsOneInstanceOf(pointsTo, ParamPref.class, t -> t.index() == 3));

        result.computeMergingFlows();
    }

    private Object method10(final Object a, final int b1, final Object k) {
        return Enum.class.getEnumConstants();
    }

    @Test
    public void testMethod10() {
        final PointsToAnalysisResult result = analyzeVirtualMethod(getClass(), "method10", new BytecodeMethodSignature(OBJECT_TYPE_REF,
                new BytecodeTypeRef[]{OBJECT_TYPE_REF, BytecodePrimitiveTypeRef.INT, OBJECT_TYPE_REF}));

        final Set<Symbol> returningSymbols = result.returningSymbols();
        Assert.assertEquals(1, returningSymbols.size());

        final Set<Symbol> pointsTo = result.resolvedPointsToFor(returningSymbols.iterator().next());
        Assert.assertEquals(1, pointsTo.size());
        Assert.assertTrue(containsOneInstanceOf(pointsTo, GlobalSymbols.class, t -> t == GlobalSymbols.staticScope));
    }

    @Test
    public void testMethod11() {
        final PointsToAnalysisResult result = analyzeStaticMethod(Enum.class, "<clinit>", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID,
                new BytecodeTypeRef[]{}));

        final Set<Symbol> returningSymbols = result.returningSymbols();
        Assert.assertTrue(returningSymbols.isEmpty());

        result.computeMergingFlows();
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
}