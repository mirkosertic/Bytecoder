/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.allocator;

import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.ConstantPool;
import de.mirkosertic.bytecoder.backend.SourceMapWriter;
import de.mirkosertic.bytecoder.backend.js.JSMinifier;
import de.mirkosertic.bytecoder.backend.js.JSPrintWriter;
import de.mirkosertic.bytecoder.backend.js.JSSSAWriter;
import de.mirkosertic.bytecoder.core.AnalysisStack;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLoader;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.intrinsics.Intrinsics;
import de.mirkosertic.bytecoder.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.ssa.NaiveProgramGenerator;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.stackifier.HeadToHeadControlFlowException;
import de.mirkosertic.bytecoder.stackifier.Stackifier;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.junit.Test;

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LinearRegisterAllocatorTest {

    private static int simpleMethod(final int a, final int b) {
        final int c = a + b + 20;
        return c;
    }

    @Test
    public void testSimpleMethodWithLinearRegisterAllocation() {
        final AnalysisStack analysisStack = new AnalysisStack();
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger(), NaiveProgramGenerator.FACTORY, new Intrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(getClass()), analysisStack);
        theLinkedClass.resolveStaticMethod("simpleMethod", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}), analysisStack);

        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull("simpleMethod", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        final Program p = theMethod.getProgram();

        final List<Variable> vars = p.getVariables();
        for (final Variable v : vars) {
            System.out.println(String.format("%s Def at %d, LastUsedAt %d", v.getName(), v.liveRange().getDefinedAt(), v.liveRange().getLastUsedAt()));
        }

        assertEquals(9, vars.size());

        assertEquals("var0", vars.get(0).getName());
        assertEquals(0, vars.get(0).liveRange().getDefinedAt());
        assertEquals(0, vars.get(0).liveRange().getLastUsedAt());

        assertEquals("var1", vars.get(1).getName());
        assertEquals(0, vars.get(1).liveRange().getDefinedAt());
        assertEquals(1, vars.get(1).liveRange().getLastUsedAt());

        assertEquals("var2", vars.get(2).getName());
        assertEquals(0, vars.get(2).liveRange().getDefinedAt());
        assertEquals(2, vars.get(2).liveRange().getLastUsedAt());

        assertEquals("var3", vars.get(3).getName());
        assertEquals(1, vars.get(3).liveRange().getDefinedAt());
        assertEquals(2, vars.get(3).liveRange().getLastUsedAt());

        assertEquals("var4", vars.get(4).getName());
        assertEquals(2, vars.get(4).liveRange().getDefinedAt());
        assertEquals(4, vars.get(4).liveRange().getLastUsedAt());

        final AbstractAllocator theAllocator = Allocator.linear.allocate(p, Variable::resolveType, theLinkerContext);
        assertEquals(Collections.singleton(TypeRef.Native.INT), theAllocator.usedRegisterTypes());
        assertEquals(3L, theAllocator.registersOfType(TypeRef.Native.INT).size());
        assertEquals(0L, theAllocator.registerAssignmentFor(vars.get(0)).getNumber());
        assertEquals(TypeRef.Native.INT, theAllocator.registerAssignmentFor(vars.get(0)).getType());
        assertEquals(1L, theAllocator.registerAssignmentFor(vars.get(1)).getNumber());
        assertEquals(TypeRef.Native.INT, theAllocator.registerAssignmentFor(vars.get(1)).getType());
        assertEquals(2L, theAllocator.registerAssignmentFor(vars.get(2)).getNumber());
        assertEquals(TypeRef.Native.INT, theAllocator.registerAssignmentFor(vars.get(2)).getType());
        assertEquals(0L, theAllocator.registerAssignmentFor(vars.get(3)).getNumber());
        assertEquals(TypeRef.Native.INT, theAllocator.registerAssignmentFor(vars.get(3)).getType());
        assertEquals(1L, theAllocator.registerAssignmentFor(vars.get(4)).getNumber());
        assertEquals(TypeRef.Native.INT, theAllocator.registerAssignmentFor(vars.get(4)).getType());
    }

    @Test
    public void testStringArrayInitRegisterAllocation() throws HeadToHeadControlFlowException {
        final AnalysisStack analysisStack = new AnalysisStack();
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger(), NaiveProgramGenerator.FACTORY, new Intrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(
                String.class), analysisStack);
        theLinkedClass.resolveVirtualMethod("<init>", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[]{new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.CHAR, 1)}), analysisStack);

        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull("<init>", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[]{new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.CHAR, 1)}));
        final Program p = theMethod.getProgram();

        final List<Variable> vars = p.getVariables();

        assertEquals(8, vars.size());

        final AbstractAllocator theAllocator = Allocator.linear.allocate(p, Variable::resolveType, theLinkerContext);
        for (final Variable v : vars) {
            System.out.println(String.format("%s Def at %d, LastUsedAt %d assigned to register %d", v.getName(), v.liveRange().getDefinedAt(), v.liveRange().getLastUsedAt(), theAllocator.registerAssignmentFor(v).getNumber()));
        }

        assertEquals(8, theAllocator.assignedRegister().size());

        final CompileOptions theOptions = new CompileOptions(new Slf4JLogger(), true, KnownOptimizer.NONE, false, "ks", 100, 100, false, true, Allocator.passthru, new String[0], new String[0], null, false);
        final JSMinifier theMinifier = new JSMinifier(theOptions);
        final SourceMapWriter theSourcemapWriter = new SourceMapWriter();
        final StringWriter theWriter = new StringWriter();
        final JSPrintWriter theJSWriter = new JSPrintWriter(theWriter, theMinifier, theSourcemapWriter);
        final ConstantPool thePool = new ConstantPool();
        final JSSSAWriter theVariablesWriter = new JSSSAWriter(theOptions, p, 2, theJSWriter, theLinkerContext, thePool, false, theMinifier, theAllocator, null, analysisStack);
        theVariablesWriter.printRegisterDeclarations();

        final Stackifier stackifier = new Stackifier(p.getControlFlowGraph());
        theVariablesWriter.printStackified(stackifier);

        System.out.println(theWriter);
    }

}