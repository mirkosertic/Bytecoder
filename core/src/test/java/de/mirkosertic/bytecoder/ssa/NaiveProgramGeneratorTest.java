/*
 * Copyright 2017 Mirko Sertic
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
package de.mirkosertic.bytecoder.ssa;

import de.mirkosertic.bytecoder.allocator.LinearRegisterAllocator;
import de.mirkosertic.bytecoder.backend.js.JSIntrinsics;
import de.mirkosertic.bytecoder.core.BytecodeAccessFlags;
import de.mirkosertic.bytecoder.core.BytecodeAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeAttributes;
import de.mirkosertic.bytecoder.core.BytecodeClass;
import de.mirkosertic.bytecoder.core.BytecodeCodeAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeInstructionACONSTNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGOTO;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericADD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericLOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSUB;
import de.mirkosertic.bytecoder.core.BytecodeInstructionICONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionRETURN;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLoader;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeProgram;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NaiveProgramGeneratorTest {

    private Program newProgramFrom(final BytecodeProgram aProgram, final BytecodeMethodSignature aSignature) {

        final BytecodeLinkerContext theContext = mock(BytecodeLinkerContext.class);
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theContext, new JSIntrinsics());

        final BytecodeClass theClass = mock(BytecodeClass.class);
        when(theClass.getAttributes()).thenReturn(new BytecodeAttributes(new BytecodeAttributeInfo[0]));

        final BytecodeMethod theMethod = mock(BytecodeMethod.class);
        when(theMethod.getAccessFlags()).thenReturn(new BytecodeAccessFlags(0));
        when(theMethod.getSignature()).thenReturn(aSignature);

        final BytecodeCodeAttributeInfo theCodeAttribute = mock(BytecodeCodeAttributeInfo.class);
        when(theCodeAttribute.getProgram()).thenReturn(aProgram);
        when(theMethod.getCode(any())).thenReturn(theCodeAttribute);

        return theGenerator.generateFrom(theClass, theMethod);
    }

    @Test
    public void testWithReturn() {
        final BytecodeProgram theBytecodeProgram = new BytecodeProgram();
        theBytecodeProgram.addInstruction(new BytecodeInstructionRETURN(BytecodeOpcodeAddress.START_AT_ZERO));

        final Program theProgram = newProgramFrom(theBytecodeProgram, new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        assertEquals(0, theProgram.getVariables().size());
        final ControlFlowGraph theCFG = theProgram.getControlFlowGraph();
        assertEquals(1, theCFG.dominators().getPreOrder().size());
        final RegionNode theSingleNode = theCFG.startNode();
        assertEquals(1, theSingleNode.getExpressions().size());
    }

    @Test
    public void testPHIStack() {
        final BytecodeProgram theBytecodeProgram = new BytecodeProgram();
        theBytecodeProgram.addInstruction(new BytecodeInstructionICONST(BytecodeOpcodeAddress.START_AT_ZERO, 11));
        theBytecodeProgram.addInstruction(new BytecodeInstructionICONST(new BytecodeOpcodeAddress(1), 22));
        theBytecodeProgram.addInstruction(new BytecodeInstructionACONSTNULL(new BytecodeOpcodeAddress(2)));
        theBytecodeProgram.addInstruction(new BytecodeInstructionIFNULL(new BytecodeOpcodeAddress(4), 10));
        theBytecodeProgram.addInstruction(new BytecodeInstructionGenericADD(new BytecodeOpcodeAddress(5), BytecodePrimitiveTypeRef.INT));
        theBytecodeProgram.addInstruction(new BytecodeInstructionGenericLOAD(new BytecodeOpcodeAddress(6), BytecodePrimitiveTypeRef.INT, 1));
        theBytecodeProgram.addInstruction(new BytecodeInstructionGenericADD(new BytecodeOpcodeAddress(7), BytecodePrimitiveTypeRef.INT));
        theBytecodeProgram.addInstruction(new BytecodeInstructionGOTO(new BytecodeOpcodeAddress(6), 94));
        theBytecodeProgram.addInstruction(new BytecodeInstructionGenericSUB(new BytecodeOpcodeAddress(14), BytecodePrimitiveTypeRef.INT));
        theBytecodeProgram.addInstruction(new BytecodeInstructionGenericLOAD(new BytecodeOpcodeAddress(15), BytecodePrimitiveTypeRef.INT, 1));
        theBytecodeProgram.addInstruction(new BytecodeInstructionGenericADD(new BytecodeOpcodeAddress(17), BytecodePrimitiveTypeRef.INT));

        theBytecodeProgram.addInstruction(new BytecodeInstructionGenericLOAD(new BytecodeOpcodeAddress(100), BytecodePrimitiveTypeRef.INT, 2));
        theBytecodeProgram.addInstruction(new BytecodeInstructionGenericADD(new BytecodeOpcodeAddress(101), BytecodePrimitiveTypeRef.INT));
        theBytecodeProgram.addInstruction(new BytecodeInstructionGenericRETURN(new BytecodeOpcodeAddress(102), BytecodePrimitiveTypeRef.INT));

        final Program theProgram = newProgramFrom(theBytecodeProgram, new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        assertEquals(6, theProgram.getVariables().size());
        final ControlFlowGraph theCFG = theProgram.getControlFlowGraph();
        assertEquals(4, theCFG.dominators().getPreOrder().size());
        final RegionNode theSingleNode = theCFG.startNode();
        assertEquals(2, theSingleNode.getExpressions().size());
        System.out.println(theCFG.toDOT());
    }

    private static int simpleMethod(final int a, final int b) {
        final int c = a + b + 20;
        return c;
    }

    @Test
    public void testSimpleMethodWithRegisterAllocation() {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new JSIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(NaiveProgramGeneratorTest.class));
        theLinkedClass.resolveStaticMethod("simpleMethod", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull("simpleMethod", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

        final List<Variable> vars = p.getVariables();
        for (final Variable v : vars) {
            System.out.println(String.format("%s Def at %d, LastUsedAt %d", v.getName(), v.getDefinedAt(), v.getLastUsedAt()));
        }

        assertEquals(5, vars.size());

        assertEquals("var0", vars.get(0).getName());
        assertEquals(2, vars.get(0).getDefinedAt());
        assertEquals(4, vars.get(0).getLastUsedAt());

        assertEquals("var1", vars.get(1).getName());
        assertEquals(4, vars.get(1).getDefinedAt());
        assertEquals(5, vars.get(1).getLastUsedAt());

        assertEquals("var2", vars.get(2).getName());
        assertEquals(5, vars.get(2).getDefinedAt());
        assertEquals(6, vars.get(2).getLastUsedAt());

        assertEquals("local_2_INT", vars.get(3).getName());
        assertEquals(5, vars.get(3).getDefinedAt());
        assertEquals(6, vars.get(3).getLastUsedAt());

        assertEquals("var4", vars.get(4).getName());
        assertEquals(6, vars.get(4).getDefinedAt());
        assertEquals(7, vars.get(4).getLastUsedAt());

        final LinearRegisterAllocator theAllocator = new LinearRegisterAllocator(p.getVariables());
        assertEquals(Collections.singleton(TypeRef.Native.INT), theAllocator.usedRegisterTypes());
        assertEquals(3L, theAllocator.registersOfType(TypeRef.Native.INT).size());
        assertEquals(0L, theAllocator.registerAssignmentFor(vars.get(0)).getNumber());
        assertEquals(TypeRef.Native.INT, theAllocator.registerAssignmentFor(vars.get(0)).getType());
        assertEquals(1L, theAllocator.registerAssignmentFor(vars.get(1)).getNumber());
        assertEquals(TypeRef.Native.INT, theAllocator.registerAssignmentFor(vars.get(1)).getType());
        assertEquals(0L, theAllocator.registerAssignmentFor(vars.get(2)).getNumber());
        assertEquals(TypeRef.Native.INT, theAllocator.registerAssignmentFor(vars.get(2)).getType());
        assertEquals(2L, theAllocator.registerAssignmentFor(vars.get(3)).getNumber());
        assertEquals(TypeRef.Native.INT, theAllocator.registerAssignmentFor(vars.get(3)).getType());
        assertEquals(1L, theAllocator.registerAssignmentFor(vars.get(4)).getNumber());
        assertEquals(TypeRef.Native.INT, theAllocator.registerAssignmentFor(vars.get(4)).getType());
    }
}