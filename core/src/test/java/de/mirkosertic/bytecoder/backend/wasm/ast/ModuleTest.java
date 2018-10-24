/*
 * Copyright 2018 Mirko Sertic
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
package de.mirkosertic.bytecoder.backend.wasm.ast;

import static de.mirkosertic.bytecoder.backend.wasm.ast.Expressions.c;
import static de.mirkosertic.bytecoder.backend.wasm.ast.Expressions.call;
import static de.mirkosertic.bytecoder.backend.wasm.ast.Expressions.getGlobal;
import static de.mirkosertic.bytecoder.backend.wasm.ast.Expressions.getLocal;
import static de.mirkosertic.bytecoder.backend.wasm.ast.Expressions.i32Add;
import static de.mirkosertic.bytecoder.backend.wasm.ast.Expressions.param;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class ModuleTest {

    @Test
    public void testSimpleCase() throws IOException {
        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final Exporter exporter = new Exporter();
        exporter.export(module, pw);

        final String expected = "(module " + System.lineSeparator()
                + "    )";
        Assert.assertEquals(expected, strWriter.toString());
    }

    @Test
    public void testSimpleCaseBinary() throws IOException {
        final Module module = new Module();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        final Exporter exporter = new Exporter();
        exporter.export(module, bos);

        //try (FileOutputStream fos = new FileOutputStream("/home/sertic/Development/Projects/Bytecoder/core/src/test/resources/de/mirkosertic/bytecoder/backend/wasm/ast/testSimpleCase.wasm")) {
        //    exporter.export(module, fos);
        //}

        final byte[] expected = IOUtils.toByteArray(getClass().getResource("testSimpleCase.wasm"));
        Assert.assertArrayEquals(expected, bos.toByteArray());

    }

    @Test
    public void testSimpleFunction() throws IOException {

        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final ExportableFunction function = functionsContent.newFunction("label", Collections.singletonList(param("p1", PrimitiveType.i32)), PrimitiveType.i32);

        function.flow.ret(c(42));
        function.exportAs("expfunction");

        final Exporter exporter = new Exporter();
        exporter.export(module, pw);

        final String expected = "(module " + System.lineSeparator()
                + "    (type $t0 (func (param i32) (result i32)))" + System.lineSeparator()
                + "    (func $label (type $t0) (param $p1 i32) (result i32)" + System.lineSeparator()
                + "        (return (i32.const 42)))" + System.lineSeparator()
                + "    (export \"expfunction\" (func $label))" + System.lineSeparator()
                + "    )";
        Assert.assertEquals(expected, strWriter.toString());
    }

    @Test
    public void testSimpleFunctionBinary() throws IOException {

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final ExportableFunction function = functionsContent.newFunction("label", Collections.singletonList(param("p1", PrimitiveType.i32)), PrimitiveType.i32);

        function.flow.ret(c(42));
        function.exportAs("expfunction");

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Exporter exporter = new Exporter();
        exporter.export(module, bos);

        //try (FileOutputStream fos = new FileOutputStream("/home/sertic/Development/Projects/Bytecoder/core/src/test/resources/de/mirkosertic/bytecoder/backend/wasm/ast/testSimpleFunction.wasm")) {
        //    exporter.export(module, fos);
        //}

        final byte[] expected = IOUtils.toByteArray(getClass().getResource("testSimpleFunction.wasm"));
        Assert.assertArrayEquals(expected, bos.toByteArray());
    }

    @Test
    public void testWithMemory() throws IOException {

        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();

        final Memory memory = module.getMems().newMemory(10, 20);

        final Exporter exporter = new Exporter();
        exporter.export(module, pw);

        Assert.assertEquals("(module " + System.lineSeparator()
                + "    (memory $mem0 10 20)" + System.lineSeparator()
                + "    )", strWriter.toString());
    }

    @Test
    public void testWithMemoryBinary() throws IOException {

        final Module module = new Module();

        final Memory memory = module.getMems().newMemory(10, 20);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        final Exporter exporter = new Exporter();
        exporter.export(module, bos);

        //try (FileOutputStream fos = new FileOutputStream("/home/sertic/Development/Projects/Bytecoder/core/src/test/resources/de/mirkosertic/bytecoder/backend/wasm/ast/testWithMemory.wasm")) {
        //    exporter.export(module, fos);
        //}

        final byte[] expected = IOUtils.toByteArray(getClass().getResource("testWithMemory.wasm"));
        Assert.assertArrayEquals(expected, bos.toByteArray());
    }

    @Test
    public void testWithExportedMemory() throws IOException {

        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();

        final Memory memory = module.getMems().newMemory(10, 20);
        memory.exportAs("exported");

        final Exporter exporter = new Exporter();
        exporter.export(module, pw);

        Assert.assertEquals("(module " + System.lineSeparator()
                + "    (memory $mem0 10 20)" + System.lineSeparator()
                + "    (export \"exported\" (memory $mem0))" + System.lineSeparator()
                + "    )", strWriter.toString());
    }

    @Test
    public void testWithExportedMemoryBinary() throws IOException {

        final Module module = new Module();

        final Memory memory = module.getMems().newMemory(10, 20);
        memory.exportAs("exported");

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Exporter exporter = new Exporter();
        exporter.export(module, bos);

        //try (final FileOutputStream fos = new FileOutputStream("/home/sertic/Development/Projects/Bytecoder/core/src/test/resources/de/mirkosertic/bytecoder/backend/wasm/ast/testWithExportedMemory.wasm")) {
        //    exporter.export(module, fos);
        //}

        final byte[] expected = IOUtils.toByteArray(getClass().getResource("testWithExportedMemory.wasm"));
        Assert.assertArrayEquals(expected, bos.toByteArray());
    }

    @Test
    public void testFunctionImport() throws IOException {
        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final Function function = module.getImports().importFunction(new ImportReference("mod","obj"),"label", PrimitiveType.i32);

        try (final TextWriter writer = new TextWriter(pw)) {
            module.writeTo(writer);
        }

        Assert.assertEquals("(module " + System.lineSeparator()
                + "    (type $t0 (func (result i32)))" + System.lineSeparator()
                + "    ($import \"mod\" \"obj\" (func $label (result i32)))" + System.lineSeparator()
                + "    )", strWriter.toString());
    }

    @Test
    public void testFunctionImportBinary() throws IOException {
        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final Function function = module.getImports().importFunction(new ImportReference("mod","obj"),"label", PrimitiveType.i32);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Exporter exporter = new Exporter();
        exporter.export(module, bos);

        //try (FileOutputStream fos = new FileOutputStream("/home/sertic/Development/Projects/Bytecoder/core/src/test/resources/de/mirkosertic/bytecoder/backend/wasm/ast/testFunctionImport.wasm")) {
        //    exporter.export(module, fos);
        //}

        final byte[] expected = IOUtils.toByteArray(getClass().getResource("testFunctionImport.wasm"));
        Assert.assertArrayEquals(expected, bos.toByteArray());
    }

    @Test
    public void testLocalAccess() throws IOException {

        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = param("p1", PrimitiveType.i32);
        final Param p2 = param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        final Local tempLocal = function.localByLabel("loc", PrimitiveType.i32);

        function.flow.ret(getLocal(tempLocal));
        function.exportAs("expfunction");

        final Exporter exporter = new Exporter();
        exporter.export(module, pw);

        final String expected = "(module " + System.lineSeparator()
                + "    (type $t0 (func (param i32) (param i32) (result i32)))" + System.lineSeparator()
                + "    (func $label (type $t0) (param $p1 i32) (param $p2 i32) (result i32)" + System.lineSeparator()
                + "        (local $loc i32)" + System.lineSeparator()
                + "        (return (get_local $loc)))" + System.lineSeparator()
                + "    (export \"expfunction\" (func $label))" + System.lineSeparator()
                + "    )";
        Assert.assertEquals(expected, strWriter.toString());
    }

    @Test
    public void testLocalAccessBinary() throws IOException {
        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = param("p1", PrimitiveType.i32);
        final Param p2 = param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        final Local tempLocal = function.localByLabel("loc", PrimitiveType.i32);
        function.flow.ret(getLocal(tempLocal));
        function.exportAs("expfunction");

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Exporter exporter = new Exporter();
        exporter.export(module, bos);

        //try (FileOutputStream fos = new FileOutputStream("/home/sertic/Development/Projects/Bytecoder/core/src/test/resources/de/mirkosertic/bytecoder/backend/wasm/ast/testLocalAccess.wasm")) {
        //    exporter.export(module, fos);
        //}

        final byte[] expected = IOUtils.toByteArray(getClass().getResource("testLocalAccess.wasm"));
        Assert.assertArrayEquals(expected, bos.toByteArray());
    }

    @Test
    public void testBlock() throws IOException {

        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = param("p1", PrimitiveType.i32);
        final Param p2 = param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        final Local tempLocal = function.localByLabel("loc", PrimitiveType.i32);

        final Block block = function.flow.block("outer");
        block.flow.ret(getLocal(tempLocal));
        function.flow.unreachable();
        function.exportAs("expfunction");

        final Exporter exporter = new Exporter();
        exporter.export(module, pw);

        final String expected = "(module " + System.lineSeparator()
                + "    (type $t0 (func (param i32) (param i32) (result i32)))" + System.lineSeparator()
                + "    (func $label (type $t0) (param $p1 i32) (param $p2 i32) (result i32)" + System.lineSeparator()
                + "        (local $loc i32)" + System.lineSeparator()
                + "        (block $outer" + System.lineSeparator()
                + "            (return (get_local $loc))" + System.lineSeparator()
                + "            )" + System.lineSeparator()
                + "        (unreachable))" + System.lineSeparator()
                + "    (export \"expfunction\" (func $label))" + System.lineSeparator()
                + "    )";
        Assert.assertEquals(expected, strWriter.toString());
    }

    @Test
    public void testBlockBinary() throws IOException {
        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = param("p1", PrimitiveType.i32);
        final Param p2 = param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        final Local tempLocal = function.localByLabel("loc", PrimitiveType.i32);

        final Block block = function.flow.block("outer");
        block.flow.ret(getLocal(tempLocal));
        function.flow.unreachable();
        function.exportAs("expfunction");

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Exporter exporter = new Exporter();
        exporter.export(module, bos);

        //try (FileOutputStream fos = new FileOutputStream("/home/sertic/Development/Projects/Bytecoder/core/src/test/resources/de/mirkosertic/bytecoder/backend/wasm/ast/testBlockBinary.wasm")) {
        //    exporter.export(module, fos);
        //}

        final byte[] expected = IOUtils.toByteArray(getClass().getResource("testBlockBinary.wasm"));
        Assert.assertArrayEquals(expected, bos.toByteArray());
    }

    @Test
    public void testIf() throws IOException {

        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = param("p1", PrimitiveType.i32);
        final Param p2 = param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        final Local tempLocal = function.localByLabel("loc", PrimitiveType.i32);

        final Block block = function.flow.block("outer");
        function.flow.unreachable();

        final I32IF ifExp = block.flow.i32ifeq(c(10), c(20));
        ifExp.flow.ret(getLocal(tempLocal));

        function.exportAs("expfunction");

        final Exporter exporter = new Exporter();
        exporter.export(module, pw);

        final String expected = "(module " + System.lineSeparator()
                + "    (type $t0 (func (param i32) (param i32) (result i32)))" + System.lineSeparator()
                + "    (func $label (type $t0) (param $p1 i32) (param $p2 i32) (result i32)" + System.lineSeparator()
                + "        (local $loc i32)" + System.lineSeparator()
                + "        (block $outer" + System.lineSeparator()
                + "            (if" + System.lineSeparator()
                + "                (i32.eq (i32.const 10) (i32.const 20))" + System.lineSeparator()
                + "                (return (get_local $loc)))" + System.lineSeparator()
                + "            )" + System.lineSeparator()
                + "        (unreachable))" + System.lineSeparator()
                + "    (export \"expfunction\" (func $label))" + System.lineSeparator()
                + "    )";
        Assert.assertEquals(expected, strWriter.toString());
    }

    @Test
    public void testIfBinary() throws IOException {
        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = param("p1", PrimitiveType.i32);
        final Param p2 = param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        final Local tempLocal = function.localByLabel("loc", PrimitiveType.i32);

        final Block block = function.flow.block("outer");
        function.flow.unreachable();

        final I32IF ifExp = block.flow.i32ifeq(c(10), c(20));
        ifExp.flow.branchOutOf(block);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Exporter exporter = new Exporter();
        exporter.export(module, bos);

        //try (FileOutputStream fos = new FileOutputStream("/home/sertic/Development/Projects/Bytecoder/core/src/test/resources/de/mirkosertic/bytecoder/backend/wasm/ast/testIf.wasm")) {
        //    exporter.export(module, fos);
        //}

        final byte[] expected = IOUtils.toByteArray(getClass().getResource("testIf.wasm"));
        Assert.assertArrayEquals(expected, bos.toByteArray());
    }


    @Test
    public void testBlockBranch() throws IOException {

        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = param("p1", PrimitiveType.i32);
        final Param p2 = param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        final Local tempLocal = function.localByLabel("loc", PrimitiveType.i32);

        final Block block = function.flow.block("outer");
        block.flow.branchOutOf(block);
        function.flow.unreachable();
        function.exportAs("expfunction");

        final Exporter exporter = new Exporter();
        exporter.export(module, pw);

        final String expected = "(module " + System.lineSeparator()
                + "    (type $t0 (func (param i32) (param i32) (result i32)))" + System.lineSeparator()
                + "    (func $label (type $t0) (param $p1 i32) (param $p2 i32) (result i32)" + System.lineSeparator()
                + "        (local $loc i32)" + System.lineSeparator()
                + "        (block $outer" + System.lineSeparator()
                + "            (br $outer)" + System.lineSeparator()
                + "            )" + System.lineSeparator()
                + "        (unreachable))" + System.lineSeparator()
                + "    (export \"expfunction\" (func $label))" + System.lineSeparator()
                + "    )";
        Assert.assertEquals(expected, strWriter.toString());
    }

    @Test
    public void testBlockBranchBinary() throws IOException {
        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = param("p1", PrimitiveType.i32);
        final Param p2 = param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        final Local tempLocal = function.localByLabel("loc", PrimitiveType.i32);

        final Block block = function.flow.block("outer");
        block.flow.branchOutOf(block);
        function.flow.unreachable();
        function.exportAs("expfunction");

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Exporter exporter = new Exporter();
        exporter.export(module, bos);

        //try (FileOutputStream fos = new FileOutputStream("/home/sertic/Development/Projects/Bytecoder/core/src/test/resources/de/mirkosertic/bytecoder/backend/wasm/ast/testBlockBranch.wasm")) {
        //    exporter.export(module, fos);
        //}

        final byte[] expected = IOUtils.toByteArray(getClass().getResource("testBlockBranch.wasm"));
        Assert.assertArrayEquals(expected, bos.toByteArray());
    }

    @Test
    public void testBlockBranchIf() throws IOException {

        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = param("p1", PrimitiveType.i32);
        final Param p2 = param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        final Local tempLocal = function.localByLabel("loc", PrimitiveType.i32);

        final Block block = function.flow.block("outer");
        block.flow.branchOutIf(block, c(42));
        function.flow.unreachable();
        function.exportAs("expfunction");

        final Exporter exporter = new Exporter();
        exporter.export(module, pw);

        final String expected = "(module " + System.lineSeparator()
                + "    (type $t0 (func (param i32) (param i32) (result i32)))" + System.lineSeparator()
                + "    (func $label (type $t0) (param $p1 i32) (param $p2 i32) (result i32)" + System.lineSeparator()
                + "        (local $loc i32)" + System.lineSeparator()
                + "        (block $outer" + System.lineSeparator()
                + "            (br_if $outer " + System.lineSeparator()
                + "                (i32.const 42))" + System.lineSeparator()
                + "            )" + System.lineSeparator()
                + "        (unreachable))" + System.lineSeparator()
                + "    (export \"expfunction\" (func $label))" + System.lineSeparator()
                + "    )";
        Assert.assertEquals(expected, strWriter.toString());
    }

    @Test
    public void testBlockBranchIfBinary() throws IOException {
        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = param("p1", PrimitiveType.i32);
        final Param p2 = param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        final Local tempLocal = function.localByLabel("loc", PrimitiveType.i32);

        final Block block = function.flow.block("outer");
        block.flow.branchOutIf(block, c(42));
        function.flow.unreachable();
        function.exportAs("expfunction");

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Exporter exporter = new Exporter();
        exporter.export(module, bos);

        //try (final FileOutputStream fos = new FileOutputStream("/home/sertic/Development/Projects/Bytecoder/core/src/test/resources/de/mirkosertic/bytecoder/backend/wasm/ast/testBlockBranchIf.wasm")) {
        //    exporter.export(module, fos);
        //}

        final byte[] expected = IOUtils.toByteArray(getClass().getResource("testBlockBranchIf.wasm"));
        Assert.assertArrayEquals(expected, bos.toByteArray());
    }

    @Test
    public void testTableWithFunction() throws IOException {

        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = param("p1", PrimitiveType.i32);
        final Param p2 = param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        function.flow.unreachable();
        function.toTable();

        final Exporter exporter = new Exporter();
        exporter.export(module, pw);

        final String expected = "(module " + System.lineSeparator() +
                "    (type $t0 (func (param i32) (param i32) (result i32)))" + System.lineSeparator() +
                "    (table 1 anyfunc)" + System.lineSeparator() +
                "    (elem (i32.const 0) $label)" + System.lineSeparator() +
                "    (func $label (type $t0) (param $p1 i32) (param $p2 i32) (result i32)" + System.lineSeparator() +
                "        (unreachable))" + System.lineSeparator() +
                "    )";
        Assert.assertEquals(expected, strWriter.toString());
    }

    @Test
    public void testTableWithFunctionBinary() throws IOException {

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = param("p1", PrimitiveType.i32);
        final Param p2 = param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        function.flow.unreachable();
        function.toTable();

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Exporter exporter = new Exporter();
        exporter.export(module, bos);

        //try (final FileOutputStream fos = new FileOutputStream("D:\\source\\idea_projects\\Bytecoder\\core\\src\\test\\resources\\de\\mirkosertic\\bytecoder\\backend\\wasm\\ast\\testTableWithFunction.wasm")) {
        //    exporter.export(module, fos);
        //}

        final byte[] expected = IOUtils.toByteArray(getClass().getResource("testTableWithFunction.wasm"));
        Assert.assertArrayEquals(expected, bos.toByteArray());
    }

    @Test
    public void testGlobals() throws IOException {

        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();

        final Global g1 = module.getGlobals().newConstantGlobal("constant", PrimitiveType.i32, c(42));
        final Global g2 = module.getGlobals().newMutableGlobal("mutable", PrimitiveType.i32, c(21));

        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = param("p1", PrimitiveType.i32);
        final Param p2 = param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        function.flow.ret(getGlobal(g1));

        final Exporter exporter = new Exporter();
        exporter.export(module, pw);

        final String expected = "(module " + System.lineSeparator()
                + "    (type $t0 (func (param i32) (param i32) (result i32)))" + System.lineSeparator()
                + "    (global $constant i32 (i32.const 42))" + System.lineSeparator()
                + "    (global $mutable (mut i32) (i32.const 21))" + System.lineSeparator()
                + "    (func $label (type $t0) (param $p1 i32) (param $p2 i32) (result i32)" + System.lineSeparator()
                + "        (return (get_global $constant)))" + System.lineSeparator()
                + "    )";
        Assert.assertEquals(expected, strWriter.toString());
    }

    @Test
    public void testGlobalsBinary() throws IOException {

        final Module module = new Module();

        final Global g1 = module.getGlobals().newConstantGlobal("constant", PrimitiveType.i32, c(42));
        final Global g2 = module.getGlobals().newMutableGlobal("mutable", PrimitiveType.i32, c(21));

        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = param("p1", PrimitiveType.i32);
        final Param p2 = param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        function.flow.ret(getGlobal(g1));

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Exporter exporter = new Exporter();
        exporter.export(module, bos);

        //try (final FileOutputStream fos = new FileOutputStream("/home/sertic/Development/Projects/Bytecoder/core/src/test/resources/de/mirkosertic/bytecoder/backend/wasm/ast/testGlobalsBinary.wasm")) {
        //    exporter.export(module, fos);
        //}

        final byte[] expected = IOUtils.toByteArray(getClass().getResource("testGlobalsBinary.wasm"));
        Assert.assertArrayEquals(expected, bos.toByteArray());
    }

    @Test
    public void testIntegerMath() throws IOException {

        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();

        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = param("p1", PrimitiveType.i32);
        final Param p2 = param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        final Local loc1 = function.localByLabel("local1", PrimitiveType.i32);
        function.flow.setLocal(loc1, c(100));
        function.flow.ret(i32Add(getLocal(loc1), c(200)));

        final Exporter exporter = new Exporter();
        exporter.export(module, pw);

        final String expected = "(module " + System.lineSeparator()
                + "    (type $t0 (func (param i32) (param i32) (result i32)))" + System.lineSeparator()
                + "    (func $label (type $t0) (param $p1 i32) (param $p2 i32) (result i32)" + System.lineSeparator()
                + "        (local $local1 i32)" + System.lineSeparator()
                + "        (set_local $local1 (i32.const 100))" + System.lineSeparator()
                + "        (return (i32.add (get_local $local1) (i32.const 200))))" + System.lineSeparator()
                + "    )";
        Assert.assertEquals(expected, strWriter.toString());
    }

    @Test
    public void testIntegerMathBinary() throws IOException {

        final Module module = new Module();

        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = param("p1", PrimitiveType.i32);
        final Param p2 = param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        final Local loc1 = function.localByLabel("local1", PrimitiveType.i32);
        function.flow.setLocal(loc1, c(100));
        function.flow.ret(i32Add(getLocal(loc1), c(200)));

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Exporter exporter = new Exporter();
        exporter.export(module, bos);

        //try (final FileOutputStream fos = new FileOutputStream("/home/sertic/Development/Projects/Bytecoder/core/src/test/resources/de/mirkosertic/bytecoder/backend/wasm/ast/testIntegerMath.wasm")) {
        //    exporter.export(module, fos);
        //}

        final byte[] expected = IOUtils.toByteArray(getClass().getResource("testIntegerMath.wasm"));
        Assert.assertArrayEquals(expected, bos.toByteArray());
    }

    @Test
    public void testCall() throws IOException {

        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();

        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = param("p1", PrimitiveType.i32);
        final Param p2 = param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        final Local loc1 = function.localByLabel("local1", PrimitiveType.i32);
        function.flow.ret(call(function, getLocal(p1), getLocal(p2)));

        final Exporter exporter = new Exporter();
        exporter.export(module, pw);

        final String expected = "(module " + System.lineSeparator()
                + "    (type $t0 (func (param i32) (param i32) (result i32)))" + System.lineSeparator()
                + "    (func $label (type $t0) (param $p1 i32) (param $p2 i32) (result i32)" + System.lineSeparator()
                + "        (local $local1 i32)" + System.lineSeparator()
                + "        (return (call $label (get_local $p1) (get_local $p2))))" + System.lineSeparator()
                + "    )";
        Assert.assertEquals(expected, strWriter.toString());
    }

    @Test
    public void testCallBinary() throws IOException {

        final Module module = new Module();

        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = param("p1", PrimitiveType.i32);
        final Param p2 = param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        final Local loc1 = function.localByLabel("local1", PrimitiveType.i32);
        function.flow.ret(call(function, getLocal(p1), getLocal(p2)));

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final Exporter exporter = new Exporter();
        exporter.export(module, bos);

        //try (final FileOutputStream fos = new FileOutputStream("/home/sertic/Development/Projects/Bytecoder/core/src/test/resources/de/mirkosertic/bytecoder/backend/wasm/ast/testCall.wasm")) {
        //    exporter.export(module, fos);
        //}

        final byte[] expected = IOUtils.toByteArray(getClass().getResource("testCall.wasm"));
        Assert.assertArrayEquals(expected, bos.toByteArray());
    }
}