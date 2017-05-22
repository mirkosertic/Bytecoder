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
package de.mirkosertic.bytecoder.backend.js;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

import de.mirkosertic.bytecoder.ast.ASTBlock;
import de.mirkosertic.bytecoder.ast.ASTGenerator;
import de.mirkosertic.bytecoder.classlib.ExceptionRethrower;
import de.mirkosertic.bytecoder.classlib.java.lang.TThrowable;
import de.mirkosertic.bytecoder.core.BytecodeBasicBlock;
import de.mirkosertic.bytecoder.core.BytecodeClass;
import de.mirkosertic.bytecoder.core.BytecodeCodeAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeControlFlowGraph;
import de.mirkosertic.bytecoder.core.BytecodeLoader;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePackageReplacer;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeProgram;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

public class JSASTCodeGeneratorTest {

    public static int intAdd(int a, int b) {
        return a + b;
    }

    public static int intSub(int a, int b) {
        return a - b;
    }

    public static int intDiv(int a, int b) {
        return a / b;
    }

    public static int intMul(int a, int b) {
        return a * b;
    }

    @Test
    public void simpleTest() throws IOException, ClassNotFoundException {
        BytecodeLoader theLoader = new BytecodeLoader(new BytecodePackageReplacer());
        BytecodeClass theClass = theLoader.loadByteCode(BytecodeObjectTypeRef.fromRuntimeClass(ExceptionRethrower.class));
        BytecodeMethod theMethod = theClass.methodByNameAndSignatureOrNull("getLastOutcomeOrNullAndReset", new BytecodeMethodSignature(
                BytecodeObjectTypeRef.fromRuntimeClass(TThrowable.class), new BytecodeTypeRef[] {}
        ));
        BytecodeCodeAttributeInfo theCode = theMethod.getCode(theClass);
        BytecodeProgram theProgram = theCode.getProgramm();

        ASTGenerator theGenerator = new ASTGenerator();
        BytecodeControlFlowGraph theGraph = new BytecodeControlFlowGraph(theProgram);
        JSASTCodeGenerator theJSGenerator = new JSASTCodeGenerator();
        StringWriter theWriter = new StringWriter();
        PrintWriter thePW = new PrintWriter(theWriter);
        for (BytecodeBasicBlock theBlock : theGraph.getBlocks()) {
            ASTBlock theASTBlock = theGenerator.generateFrom(theBlock);
            thePW.println(theJSGenerator.generateFor(theASTBlock));
        }
        thePW.flush();
        Assert.assertEquals("", theWriter.toString());
    }

    @Test
    public void testAdd() throws IOException, ClassNotFoundException {
        BytecodeLoader theLoader = new BytecodeLoader(new BytecodePackageReplacer());
        BytecodeClass theClass = theLoader.loadByteCode(BytecodeObjectTypeRef.fromRuntimeClass(JSASTCodeGeneratorTest.class));
        BytecodeMethod theMethod = theClass.methodByNameAndSignatureOrNull("intAdd", new BytecodeMethodSignature(
                BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}
        ));
        BytecodeCodeAttributeInfo theCode = theMethod.getCode(theClass);
        BytecodeProgram theProgram = theCode.getProgramm();

        ASTGenerator theGenerator = new ASTGenerator();
        BytecodeControlFlowGraph theGraph = new BytecodeControlFlowGraph(theProgram);
        JSASTCodeGenerator theJSGenerator = new JSASTCodeGenerator();
        StringWriter theWriter = new StringWriter();
        PrintWriter thePW = new PrintWriter(theWriter);
        for (BytecodeBasicBlock theBlock : theGraph.getBlocks()) {
            ASTBlock theASTBlock = theGenerator.generateFrom(theBlock);
            thePW.println(theJSGenerator.generateFor(theASTBlock));
        }
        thePW.flush();
        Assert.assertEquals("return (local0 + local1);", theWriter.toString().trim());
    }

    @Test
    public void testSub() throws IOException, ClassNotFoundException {
        BytecodeLoader theLoader = new BytecodeLoader(new BytecodePackageReplacer());
        BytecodeClass theClass = theLoader.loadByteCode(BytecodeObjectTypeRef.fromRuntimeClass(JSASTCodeGeneratorTest.class));
        BytecodeMethod theMethod = theClass.methodByNameAndSignatureOrNull("intSub", new BytecodeMethodSignature(
                BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}
        ));
        BytecodeCodeAttributeInfo theCode = theMethod.getCode(theClass);
        BytecodeProgram theProgram = theCode.getProgramm();

        ASTGenerator theGenerator = new ASTGenerator();
        BytecodeControlFlowGraph theGraph = new BytecodeControlFlowGraph(theProgram);
        JSASTCodeGenerator theJSGenerator = new JSASTCodeGenerator();
        StringWriter theWriter = new StringWriter();
        PrintWriter thePW = new PrintWriter(theWriter);
        for (BytecodeBasicBlock theBlock : theGraph.getBlocks()) {
            ASTBlock theASTBlock = theGenerator.generateFrom(theBlock);
            thePW.println(theJSGenerator.generateFor(theASTBlock));
        }
        thePW.flush();
        Assert.assertEquals("return (local0 - local1);", theWriter.toString().trim());
    }

    @Test
    public void testMul() throws IOException, ClassNotFoundException {
        BytecodeLoader theLoader = new BytecodeLoader(new BytecodePackageReplacer());
        BytecodeClass theClass = theLoader.loadByteCode(BytecodeObjectTypeRef.fromRuntimeClass(JSASTCodeGeneratorTest.class));
        BytecodeMethod theMethod = theClass.methodByNameAndSignatureOrNull("intMul", new BytecodeMethodSignature(
                BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}
        ));
        BytecodeCodeAttributeInfo theCode = theMethod.getCode(theClass);
        BytecodeProgram theProgram = theCode.getProgramm();

        ASTGenerator theGenerator = new ASTGenerator();
        BytecodeControlFlowGraph theGraph = new BytecodeControlFlowGraph(theProgram);
        JSASTCodeGenerator theJSGenerator = new JSASTCodeGenerator();
        StringWriter theWriter = new StringWriter();
        PrintWriter thePW = new PrintWriter(theWriter);
        for (BytecodeBasicBlock theBlock : theGraph.getBlocks()) {
            ASTBlock theASTBlock = theGenerator.generateFrom(theBlock);
            thePW.println(theJSGenerator.generateFor(theASTBlock));
        }
        thePW.flush();
        Assert.assertEquals("return (local0 * local1);", theWriter.toString().trim());
    }

    @Test
    public void testDIV() throws IOException, ClassNotFoundException {
        BytecodeLoader theLoader = new BytecodeLoader(new BytecodePackageReplacer());
        BytecodeClass theClass = theLoader.loadByteCode(BytecodeObjectTypeRef.fromRuntimeClass(JSASTCodeGeneratorTest.class));
        BytecodeMethod theMethod = theClass.methodByNameAndSignatureOrNull("intDiv", new BytecodeMethodSignature(
                BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}
        ));
        BytecodeCodeAttributeInfo theCode = theMethod.getCode(theClass);
        BytecodeProgram theProgram = theCode.getProgramm();

        ASTGenerator theGenerator = new ASTGenerator();
        BytecodeControlFlowGraph theGraph = new BytecodeControlFlowGraph(theProgram);
        JSASTCodeGenerator theJSGenerator = new JSASTCodeGenerator();
        StringWriter theWriter = new StringWriter();
        PrintWriter thePW = new PrintWriter(theWriter);
        for (BytecodeBasicBlock theBlock : theGraph.getBlocks()) {
            ASTBlock theASTBlock = theGenerator.generateFrom(theBlock);
            thePW.println(theJSGenerator.generateFor(theASTBlock));
        }
        thePW.flush();
        Assert.assertEquals("return (local0 / local1);", theWriter.toString().trim());
    }
}