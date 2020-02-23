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

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.junit.Test;

import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.ConstantPool;
import de.mirkosertic.bytecoder.backend.SourceMapWriter;
import de.mirkosertic.bytecoder.backend.js.JSIntrinsics;
import de.mirkosertic.bytecoder.backend.js.JSMinifier;
import de.mirkosertic.bytecoder.backend.js.JSPrintWriter;
import de.mirkosertic.bytecoder.backend.js.JSSSAWriter;
import de.mirkosertic.bytecoder.backend.wasm.WASMIntrinsics;
import de.mirkosertic.bytecoder.classlib.Address;
import de.mirkosertic.bytecoder.classlib.MemoryManager;
import de.mirkosertic.bytecoder.classlib.java.nio.charset.StandardCharsets;
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
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.stackifier.HeadToHeadControlFlowException;
import de.mirkosertic.bytecoder.stackifier.Stackifier;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;

public class PassThruRegisterAllocatorTest {

    private static int simpleMethod(final int a, final int b) {
        final int c = a + b + 20;
        return c;
    }

    @Test
    public void testSimpleMethodRegisterAllocation() {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new JSIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(getClass()));
        theLinkedClass.resolveStaticMethod("simpleMethod", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull("simpleMethod", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

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

        final AbstractAllocator theAllocator = Allocator.passthru.allocate(p, Variable::resolveType, theLinkerContext);
        assertEquals(Collections.singleton(TypeRef.Native.INT), theAllocator.usedRegisterTypes());
        assertEquals(9L, theAllocator.registersOfType(TypeRef.Native.INT).size());
        assertEquals(0L, theAllocator.registerAssignmentFor(vars.get(0)).getNumber());
        assertEquals(TypeRef.Native.INT, theAllocator.registerAssignmentFor(vars.get(0)).getType());
        assertEquals(1L, theAllocator.registerAssignmentFor(vars.get(1)).getNumber());
        assertEquals(TypeRef.Native.INT, theAllocator.registerAssignmentFor(vars.get(1)).getType());
        assertEquals(2L, theAllocator.registerAssignmentFor(vars.get(2)).getNumber());
        assertEquals(TypeRef.Native.INT, theAllocator.registerAssignmentFor(vars.get(2)).getType());
        assertEquals(3L, theAllocator.registerAssignmentFor(vars.get(3)).getNumber());
        assertEquals(TypeRef.Native.INT, theAllocator.registerAssignmentFor(vars.get(3)).getType());
        assertEquals(4L, theAllocator.registerAssignmentFor(vars.get(4)).getNumber());
        assertEquals(TypeRef.Native.INT, theAllocator.registerAssignmentFor(vars.get(4)).getType());
    }

    private static int complexPhiFlow(final int a, final int b) {
        int z = a + b;
        if (a > 0) {
            z = z + 1;
        } else {
            if (b > 0) {
                z = z + 2;
            } else {
                z = z + 3;
            }
            z = z + 4;
        }
        if (z > 0) {
            if (z > 1) {
                z = z + 5;
            } else {
                z = z + 6;
            }
            z++;
        } else {
            z += 7;
        }
        return z;
    }

    @Test
    public void testComplexPhiFlowRegisterAllocation() throws HeadToHeadControlFlowException {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new JSIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(getClass()));
        theLinkedClass.resolveStaticMethod("complexPhiFlow", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull("complexPhiFlow", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

        final List<Variable> vars = p.getVariables();
        for (final Variable v : vars) {
            System.out.println(String.format("%s Def at %d, LastUsedAt %d", v.getName(), v.liveRange().getDefinedAt(), v.liveRange().getLastUsedAt()));
        }

        assertEquals(38, vars.size());

        final AbstractAllocator theAllocator = Allocator.passthru.allocate(p, Variable::resolveType, theLinkerContext);

        final CompileOptions theOptions = new CompileOptions(new Slf4JLogger(), true, KnownOptimizer.NONE, false, "ks", 100, 100, false, true, Allocator.passthru, new String[0], new String[0]);
        final JSMinifier theMinifier = new JSMinifier(theOptions);
        final SourceMapWriter theSourcemapWriter = new SourceMapWriter();
        final StringWriter theWriter = new StringWriter();
        final JSPrintWriter theJSWriter = new JSPrintWriter(theWriter, theMinifier, theSourcemapWriter);
        final ConstantPool thePool = new ConstantPool();
        final JSSSAWriter theVariablesWriter = new JSSSAWriter(theOptions, p, 2, theJSWriter, theLinkerContext, thePool, false, theMinifier, theAllocator);
        theVariablesWriter.printRegisterDeclarations();

        final Stackifier stackifier = new Stackifier(p.getControlFlowGraph());
        theVariablesWriter.printStackified(stackifier);

        System.out.println(theWriter);
    }

    private static int simplePhiFlow(final int a, final int b) {
        int z = a + b;
        for (int j = 0; j < 10; j++) {
            z += 2;
        }
        return z;
    }

    @Test
    public void testSimplePhiFlowRegisterAllocation() throws HeadToHeadControlFlowException {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new JSIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(getClass()));
        theLinkedClass.resolveStaticMethod("simplePhiFlow", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull("simplePhiFlow", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

        final List<Variable> vars = p.getVariables();
        for (final Variable v : vars) {
            System.out.println(String.format("%s Def at %d, LastUsedAt %d", v.getName(), v.liveRange().getDefinedAt(), v.liveRange().getLastUsedAt()));
        }

        assertEquals(13, vars.size());

        final AbstractAllocator theAllocator = Allocator.passthru.allocate(p, Variable::resolveType, theLinkerContext);

        assertEquals(11, theAllocator.assignedRegister().size());

        final CompileOptions theOptions = new CompileOptions(new Slf4JLogger(), true, KnownOptimizer.NONE, false, "ks", 100, 100, false, true, Allocator.passthru, new String[0], new String[0]);
        final JSMinifier theMinifier = new JSMinifier(theOptions);
        final SourceMapWriter theSourcemapWriter = new SourceMapWriter();
        final StringWriter theWriter = new StringWriter();
        final JSPrintWriter theJSWriter = new JSPrintWriter(theWriter, theMinifier, theSourcemapWriter);
        final ConstantPool thePool = new ConstantPool();
        final JSSSAWriter theVariablesWriter = new JSSSAWriter(theOptions, p, 2, theJSWriter, theLinkerContext, thePool, false, theMinifier, theAllocator);
        theVariablesWriter.printRegisterDeclarations();

        final Stackifier stackifier = new Stackifier(p.getControlFlowGraph());
        theVariablesWriter.printStackified(stackifier);

        System.out.println(theWriter);
    }

    @Test
    public void testHashMapResizeRegisterAllocation() {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new JSIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(HashMap.class));
        theLinkedClass.resolveVirtualMethod("resize", new BytecodeMethodSignature(new BytecodeArrayTypeRef(BytecodeObjectTypeRef.fromUtf8Constant(new BytecodeUtf8Constant("java.util.HashMap$Node")), 1), new BytecodeTypeRef[]{}));

        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull("resize", new BytecodeMethodSignature(new BytecodeArrayTypeRef(BytecodeObjectTypeRef.fromUtf8Constant(new BytecodeUtf8Constant("java.util.HashMap$Node")), 1), new BytecodeTypeRef[]{}));
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

        final List<Variable> vars = p.getVariables();
        for (final Variable v : vars) {
            System.out.println(String.format("%s Def at %d, LastUsedAt %d", v.getName(), v.liveRange().getDefinedAt(), v.liveRange().getLastUsedAt()));
        }

        assertEquals(124, vars.size());

        final AbstractAllocator theAllocator = Allocator.passthru.allocate(p, Variable::resolveType, theLinkerContext);

        assertEquals(111, theAllocator.assignedRegister().size());
    }

    @Test
    public void testStandardCharsetsForNameRegisterAllocation() throws HeadToHeadControlFlowException {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new JSIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(
                StandardCharsets.class));
        theLinkedClass.resolveVirtualMethod("charsetForName", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Charset.class), new BytecodeTypeRef[]{BytecodeObjectTypeRef.fromRuntimeClass(String.class)}));

        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull("charsetForName", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Charset.class), new BytecodeTypeRef[]{BytecodeObjectTypeRef.fromRuntimeClass(String.class)}));
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

        final List<Variable> vars = p.getVariables();
        for (final Variable v : vars) {
            System.out.println(String.format("%s Def at %d, LastUsedAt %d", v.getName(), v.liveRange().getDefinedAt(), v.liveRange().getLastUsedAt()));
        }

        assertEquals(75, vars.size());

        final AbstractAllocator theAllocator = Allocator.passthru.allocate(p, Variable::resolveType, theLinkerContext);

        assertEquals(72, theAllocator.assignedRegister().size());

        final CompileOptions theOptions = new CompileOptions(new Slf4JLogger(), true, KnownOptimizer.NONE, false, "ks", 100, 100, false, true, Allocator.passthru, new String[0], new String[0]);
        final JSMinifier theMinifier = new JSMinifier(theOptions);
        final SourceMapWriter theSourcemapWriter = new SourceMapWriter();
        final StringWriter theWriter = new StringWriter();
        final JSPrintWriter theJSWriter = new JSPrintWriter(theWriter, theMinifier, theSourcemapWriter);
        final ConstantPool thePool = new ConstantPool();
        final JSSSAWriter theVariablesWriter = new JSSSAWriter(theOptions, p, 2, theJSWriter, theLinkerContext, thePool, false, theMinifier, theAllocator);
        theVariablesWriter.printRegisterDeclarations();

        final Stackifier stackifier = new Stackifier(p.getControlFlowGraph());
        theVariablesWriter.printStackified(stackifier);

        System.out.println(theWriter);
    }

    @Test
    public void testStringArrayInitRegisterAllocation() throws HeadToHeadControlFlowException {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new JSIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(
                String.class));
        theLinkedClass.resolveVirtualMethod("<init>", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[]{new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.CHAR, 1)}));

        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull("<init>", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[]{new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.CHAR, 1)}));
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

        final List<Variable> vars = p.getVariables();
        for (final Variable v : vars) {
            System.out.println(String.format("%s Def at %d, LastUsedAt %d", v.getName(), v.liveRange().getDefinedAt(), v.liveRange().getLastUsedAt()));
        }

        assertEquals(8, vars.size());

        final AbstractAllocator theAllocator = Allocator.passthru.allocate(p, Variable::resolveType, theLinkerContext);

        assertEquals(8, theAllocator.assignedRegister().size());

        final CompileOptions theOptions = new CompileOptions(new Slf4JLogger(), true, KnownOptimizer.NONE, false, "ks", 100, 100, false, true, Allocator.passthru, new String[0], new String[0]);
        final JSMinifier theMinifier = new JSMinifier(theOptions);
        final SourceMapWriter theSourcemapWriter = new SourceMapWriter();
        final StringWriter theWriter = new StringWriter();
        final JSPrintWriter theJSWriter = new JSPrintWriter(theWriter, theMinifier, theSourcemapWriter);
        final ConstantPool thePool = new ConstantPool();
        final JSSSAWriter theVariablesWriter = new JSSSAWriter(theOptions, p, 2, theJSWriter, theLinkerContext, thePool, false, theMinifier, theAllocator);
        theVariablesWriter.printRegisterDeclarations();

        final Stackifier stackifier = new Stackifier(p.getControlFlowGraph());
        theVariablesWriter.printStackified(stackifier);

        System.out.println(theWriter);
    }

    @Test
    public void testCharsetInitRegisterAllocation() throws HeadToHeadControlFlowException {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new JSIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(
                Charset.class));
        theLinkedClass.resolveVirtualMethod("<init>", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[]{BytecodeObjectTypeRef.fromRuntimeClass(String.class), new BytecodeArrayTypeRef(BytecodeObjectTypeRef.fromRuntimeClass(String.class), 1)}));

        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull("<init>", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[]{BytecodeObjectTypeRef.fromRuntimeClass(String.class), new BytecodeArrayTypeRef(BytecodeObjectTypeRef.fromRuntimeClass(String.class), 1)}));
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

        final List<Variable> vars = p.getVariables();
        for (final Variable v : vars) {
            System.out.println(String.format("%s Def at %d, LastUsedAt %d", v.getName(), v.liveRange().getDefinedAt(), v.liveRange().getLastUsedAt()));
        }

        assertEquals(30, vars.size());

        final AbstractAllocator theAllocator = Allocator.passthru.allocate(p, Variable::resolveType, theLinkerContext);

        assertEquals(28, theAllocator.assignedRegister().size());

        final CompileOptions theOptions = new CompileOptions(new Slf4JLogger(), true, KnownOptimizer.NONE, false, "ks", 100, 100, false, true, Allocator.passthru, new String[0], new String[0]);
        final JSMinifier theMinifier = new JSMinifier(theOptions);
        final SourceMapWriter theSourcemapWriter = new SourceMapWriter();
        final StringWriter theWriter = new StringWriter();
        final JSPrintWriter theJSWriter = new JSPrintWriter(theWriter, theMinifier, theSourcemapWriter);
        final ConstantPool thePool = new ConstantPool();
        final JSSSAWriter theVariablesWriter = new JSSSAWriter(theOptions, p, 2, theJSWriter, theLinkerContext, thePool, false, theMinifier, theAllocator);
        theVariablesWriter.printRegisterDeclarations();

        final Stackifier stackifier = new Stackifier(p.getControlFlowGraph());
        theVariablesWriter.printStackified(stackifier);

        System.out.println(theWriter);
    }

    @Test
    public void testCharsetEncoderRegisterAllocation() throws HeadToHeadControlFlowException {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new JSIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(
                CharsetEncoder.class));
        theLinkedClass.resolveVirtualMethod("encode", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(ByteBuffer.class), new BytecodeTypeRef[]{BytecodeObjectTypeRef.fromRuntimeClass(CharBuffer.class)}));

        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull("encode", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(ByteBuffer.class), new BytecodeTypeRef[]{BytecodeObjectTypeRef.fromRuntimeClass(CharBuffer.class)}));
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

        final List<Variable> vars = p.getVariables();
        for (final Variable v : vars) {
            System.out.println(String.format("%s Def at %d, LastUsedAt %d", v.getName(), v.liveRange().getDefinedAt(), v.liveRange().getLastUsedAt()));
        }

        assertEquals(54, vars.size());

        final AbstractAllocator theAllocator = Allocator.passthru.allocate(p, Variable::resolveType, theLinkerContext);

        assertEquals(50, theAllocator.assignedRegister().size());

        final CompileOptions theOptions = new CompileOptions(new Slf4JLogger(), true, KnownOptimizer.NONE, false, "ks", 100, 100, false, true, Allocator.passthru, new String[0], new String[0]);
        final JSMinifier theMinifier = new JSMinifier(theOptions);
        final SourceMapWriter theSourcemapWriter = new SourceMapWriter();
        final StringWriter theWriter = new StringWriter();
        final JSPrintWriter theJSWriter = new JSPrintWriter(theWriter, theMinifier, theSourcemapWriter);
        final ConstantPool thePool = new ConstantPool();
        final JSSSAWriter theVariablesWriter = new JSSSAWriter(theOptions, p, 2, theJSWriter, theLinkerContext, thePool, false, theMinifier, theAllocator);
        theVariablesWriter.printRegisterDeclarations();

        final Stackifier stackifier = new Stackifier(p.getControlFlowGraph());
        theVariablesWriter.printStackified(stackifier);

        System.out.println(theWriter);
    }

    @Test
    public void testHashMapGetNodeRegisterAllocator() throws HeadToHeadControlFlowException {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new JSIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(
                HashMap.class));
        final BytecodeObjectTypeRef theNodeType = BytecodeObjectTypeRef.fromUtf8Constant(new BytecodeUtf8Constant("java.util.HashMap$Node"));
        theLinkedClass.resolveVirtualMethod("getNode", new BytecodeMethodSignature(theNodeType, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodeObjectTypeRef.fromRuntimeClass(Object.class)}));

        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull("getNode", new BytecodeMethodSignature(theNodeType, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodeObjectTypeRef.fromRuntimeClass(Object.class)}));
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

        final List<Variable> vars = p.getVariables();
        for (final Variable v : vars) {
            System.out.println(String.format("%s Def at %d, LastUsedAt %d", v.getName(), v.liveRange().getDefinedAt(), v.liveRange().getLastUsedAt()));
        }

        assertEquals(48, vars.size());

        final AbstractAllocator theAllocator = Allocator.passthru.allocate(p, Variable::resolveType, theLinkerContext);

        assertEquals(47, theAllocator.assignedRegister().size());

        final CompileOptions theOptions = new CompileOptions(new Slf4JLogger(), true, KnownOptimizer.NONE, false, "ks", 100, 100, false, true, Allocator.passthru, new String[0], new String[0]);
        final JSMinifier theMinifier = new JSMinifier(theOptions);
        final SourceMapWriter theSourcemapWriter = new SourceMapWriter();
        final StringWriter theWriter = new StringWriter();
        final JSPrintWriter theJSWriter = new JSPrintWriter(theWriter, theMinifier, theSourcemapWriter);
        final ConstantPool thePool = new ConstantPool();
        final JSSSAWriter theVariablesWriter = new JSSSAWriter(theOptions, p, 2, theJSWriter, theLinkerContext, thePool, false, theMinifier, theAllocator);
        theVariablesWriter.printRegisterDeclarations();

        final Stackifier stackifier = new Stackifier(p.getControlFlowGraph());
        theVariablesWriter.printStackified(stackifier);

        System.out.println(theWriter);
    }

    private static void testWriteMemory() {
        Address.setIntValue(10, 20, 30);
    }

    @Test
    public void testWriteMemoryRegisterAllocator() {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new WASMIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(getClass()));
        theLinkedClass.resolveStaticMethod("testWriteMemory", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[]{}));

        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull("testWriteMemory", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[]{}));
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

        final List<Variable> vars = p.getVariables();
        for (final Variable v : vars) {
            System.out.println(String.format("%s Def at %d, LastUsedAt %d", v.getName(), v.liveRange().getDefinedAt(), v.liveRange().getLastUsedAt()));
        }

        final AbstractAllocator theAllocator = Allocator.passthru.allocate(p, Variable::resolveType, theLinkerContext);
        assertEquals(Collections.singleton(TypeRef.Native.INT), theAllocator.usedRegisterTypes());
    }

    @Test
    public void testFreeMemRegisterAllocator() throws HeadToHeadControlFlowException {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new JSIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(
                MemoryManager.class));
        theLinkedClass.resolveVirtualMethod("freeMem", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.LONG, new BytecodeTypeRef[]{}));

        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull("freeMem", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.LONG, new BytecodeTypeRef[]{}));
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

        final List<Variable> vars = p.getVariables();
        for (final Variable v : vars) {
            System.out.println(String.format("%s Def at %d, LastUsedAt %d", v.getName(), v.liveRange().getDefinedAt(), v.liveRange().getLastUsedAt()));
        }

        assertEquals(23, vars.size());

        final AbstractAllocator theAllocator = Allocator.passthru.allocate(p, Variable::resolveType, theLinkerContext);

        assertEquals(21, theAllocator.assignedRegister().size());

        final CompileOptions theOptions = new CompileOptions(new Slf4JLogger(), true, KnownOptimizer.NONE, false, "ks", 100, 100, false, true, Allocator.passthru, new String[0], new String[0]);
        final JSMinifier theMinifier = new JSMinifier(theOptions);
        final SourceMapWriter theSourcemapWriter = new SourceMapWriter();
        final StringWriter theWriter = new StringWriter();
        final JSPrintWriter theJSWriter = new JSPrintWriter(theWriter, theMinifier, theSourcemapWriter);
        final ConstantPool thePool = new ConstantPool();
        final JSSSAWriter theVariablesWriter = new JSSSAWriter(theOptions, p, 2, theJSWriter, theLinkerContext, thePool, false, theMinifier, theAllocator);
        theVariablesWriter.printRegisterDeclarations();

        final Stackifier stackifier = new Stackifier(p.getControlFlowGraph());
        theVariablesWriter.printStackified(stackifier);

        System.out.println(theWriter);
    }

    @Test
    public void testMapEntryRegisterAllocator() throws HeadToHeadControlFlowException {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new JSIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(
                Map.Entry.class));
        theLinkedClass.resolveStaticMethod("comparingByKey", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Comparator.class), new BytecodeTypeRef[]{}));

        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull("comparingByKey", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Comparator.class), new BytecodeTypeRef[]{}));
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

        final List<Variable> vars = p.getVariables();
        for (final Variable v : vars) {
            System.out.println(String.format("%s Def at %d, LastUsedAt %d", v.getName(), v.liveRange().getDefinedAt(), v.liveRange().getLastUsedAt()));
        }

        assertEquals(3, vars.size());

        final AbstractAllocator theAllocator = Allocator.passthru.allocate(p, Variable::resolveType, theLinkerContext);

        assertEquals(3, theAllocator.assignedRegister().size());

        final CompileOptions theOptions = new CompileOptions(new Slf4JLogger(), true, KnownOptimizer.NONE, false, "ks", 100, 100, false, true, Allocator.passthru, new String[0], new String[0]);
        final JSMinifier theMinifier = new JSMinifier(theOptions);
        final SourceMapWriter theSourcemapWriter = new SourceMapWriter();
        final StringWriter theWriter = new StringWriter();
        final JSPrintWriter theJSWriter = new JSPrintWriter(theWriter, theMinifier, theSourcemapWriter);
        final ConstantPool thePool = new ConstantPool();
        final JSSSAWriter theVariablesWriter = new JSSSAWriter(theOptions, p, 2, theJSWriter, theLinkerContext, thePool, false, theMinifier, theAllocator);
        theVariablesWriter.printRegisterDeclarations();

        final Stackifier stackifier = new Stackifier(p.getControlFlowGraph());
        theVariablesWriter.printStackified(stackifier);

        System.out.println(theWriter);
    }

    @Test
    public void testImageIconGetNext() throws HeadToHeadControlFlowException {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());
        final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new JSIntrinsics());
        final BytecodeLinkedClass theLinkedClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(
                ImageIcon.class));
        theLinkedClass.resolvePrivateMethod("getNextID", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[]{}));

        final BytecodeMethod theMethod = theLinkedClass.getBytecodeClass().methodByNameAndSignatureOrNull("getNextID", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[]{}));
        final Program p = theGenerator.generateFrom(theLinkedClass.getBytecodeClass(), theMethod);

        KnownOptimizer.ALL.optimize(p.getControlFlowGraph(), theLinkerContext);

        final List<Variable> vars = p.getVariables();
        for (final Variable v : vars) {
            System.out.println(String.format("%s Def at %d, LastUsedAt %d", v.getName(), v.liveRange().getDefinedAt(), v.liveRange().getLastUsedAt()));
        }

        assertEquals(6, vars.size());

        final AbstractAllocator theAllocator = Allocator.passthru.allocate(p, Variable::resolveType, theLinkerContext);

        assertEquals(6, theAllocator.assignedRegister().size());

        final CompileOptions theOptions = new CompileOptions(new Slf4JLogger(), true, KnownOptimizer.NONE, false, "ks", 100, 100, false, true, Allocator.passthru, new String[0], new String[0]);
        final JSMinifier theMinifier = new JSMinifier(theOptions);
        final SourceMapWriter theSourcemapWriter = new SourceMapWriter();
        final StringWriter theWriter = new StringWriter();
        final JSPrintWriter theJSWriter = new JSPrintWriter(theWriter, theMinifier, theSourcemapWriter);
        final ConstantPool thePool = new ConstantPool();
        final JSSSAWriter theVariablesWriter = new JSSSAWriter(theOptions, p, 2, theJSWriter, theLinkerContext, thePool, false, theMinifier, theAllocator);
        theVariablesWriter.printRegisterDeclarations();

        final Stackifier stackifier = new Stackifier(p.getControlFlowGraph());
        theVariablesWriter.printStackified(stackifier);

        System.out.println(theWriter);
    }
}