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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

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

        final String expected = "(module \n"
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
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(new Param("p1", PrimitiveType.i32)), PrimitiveType.i32);

        final Expressions exp = function.expressions();

        function.addChild(exp.control.ret(exp.i32.c(42)));
        function.exportAs("expfunction");

        final Exporter exporter = new Exporter();
        exporter.export(module, pw);

        final String expected = "(module \n"
                + "    (type $t0 (func (param i32) (result i32)))\n"
                + "    (func $label (type $t0) (param $p1 i32) (result i32)\n"
                + "        (return (i32.const 42)))\n"
                + "    (export \"expfunction\" (func $label))\n"
                + "    )";
        Assert.assertEquals(expected, strWriter.toString());
    }

    @Test
    public void testSimpleFunctionBinary() throws IOException {

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(new Param("p1", PrimitiveType.i32)), PrimitiveType.i32);

        final Expressions exp = function.expressions();

        function.addChild(exp.control.ret(exp.i32.c(42)));
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

        Assert.assertEquals("(module \n"
                + "    (memory $mem0 10 20)\n"
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

        Assert.assertEquals("(module \n"
                + "    (memory $mem0 10 20)\n"
                + "    (export \"exported\" (memory $mem0))\n"
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

        Assert.assertEquals("(module \n"
                + "    (type $t0 (func (result i32)))\n"
                + "    ($import \"mod\" \"obj\" (func $label (result i32)))\n"
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
        final Param p1 = new Param("p1", PrimitiveType.i32);
        final Param p2 = new Param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        final Local tempLocal = function.localByLabel("loc", PrimitiveType.i32);
        final Expressions exp = function.expressions();

        function.addChild(exp.control.ret(exp.var.getLocal(tempLocal)));
        function.exportAs("expfunction");

        final Exporter exporter = new Exporter();
        exporter.export(module, pw);

        final String expected = "(module \n"
                + "    (type $t0 (func (param i32) (param i32) (result i32)))\n"
                + "    (func $label (type $t0) (param $p1 i32) (param $p2 i32) (result i32)\n"
                + "        (local $loc i32)\n"
                + "        (return (get_local $loc)))\n"
                + "    (export \"expfunction\" (func $label))\n"
                + "    )";
        Assert.assertEquals(expected, strWriter.toString());
    }

    @Test
    public void testLocalAccessBinary() throws IOException {
        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = new Param("p1", PrimitiveType.i32);
        final Param p2 = new Param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        final Local tempLocal = function.localByLabel("loc", PrimitiveType.i32);
        final Expressions exp = function.expressions();
        function.addChild(exp.control.ret(exp.var.getLocal(tempLocal)));
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
        final Param p1 = new Param("p1", PrimitiveType.i32);
        final Param p2 = new Param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        final Local tempLocal = function.localByLabel("loc", PrimitiveType.i32);
        final Expressions exp = function.expressions();

        final Block block = exp.control.block("outer");
        function.addChild(block);
        function.addChild(exp.control.unreachable());

        block.addChild(exp.control.ret(exp.var.getLocal(tempLocal)));
        function.exportAs("expfunction");

        final Exporter exporter = new Exporter();
        exporter.export(module, pw);

        final String expected = "(module \n"
                + "    (type $t0 (func (param i32) (param i32) (result i32)))\n"
                + "    (func $label (type $t0) (param $p1 i32) (param $p2 i32) (result i32)\n"
                + "        (local $loc i32)\n"
                + "        (block $outer\n"
                + "            (return (get_local $loc))\n"
                + "            )\n"
                + "        (unreachable))\n"
                + "    (export \"expfunction\" (func $label))\n"
                + "    )";
        Assert.assertEquals(expected, strWriter.toString());
    }

    @Test
    public void testBlockBinary() throws IOException {
        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final Param p1 = new Param("p1", PrimitiveType.i32);
        final Param p2 = new Param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        final Local tempLocal = function.localByLabel("loc", PrimitiveType.i32);
        final Expressions exp = function.expressions();

        final Block block = exp.control.block("outer");
        function.addChild(block);
        function.addChild(exp.control.unreachable());

        block.addChild(exp.control.ret(exp.var.getLocal(tempLocal)));
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
        final Param p1 = new Param("p1", PrimitiveType.i32);
        final Param p2 = new Param("p2", PrimitiveType.i32);
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(p1, p2), PrimitiveType.i32);

        final Local tempLocal = function.localByLabel("loc", PrimitiveType.i32);
        final Expressions exp = function.expressions();

        final Block block = exp.control.block("outer");
        function.addChild(block);
        function.addChild(exp.control.unreachable());

        final Expression ret = exp.control.ret(exp.var.getLocal(tempLocal));
        final I32IF ifExp = exp.control.i32ifeq(exp.i32.c(10), exp.i32.c(20));
        ifExp.addChild(ret);

        block.addChild(ifExp);
        function.exportAs("expfunction");

        final Exporter exporter = new Exporter();
        exporter.export(module, pw);

        final String expected = "(module \n"
                + "    (type $t0 (func (param i32) (param i32) (result i32)))\n"
                + "    (func $label (type $t0) (param $p1 i32) (param $p2 i32) (result i32)\n"
                + "        (local $loc i32)\n"
                + "        (block $outer\n"
                + "            (if\n"
                + "                (i32.eq (i32.const 10) (i32.const 20))\n"
                + "                (return (get_local $loc)))\n"
                + "            )\n"
                + "        (unreachable))\n"
                + "    (export \"expfunction\" (func $label))\n"
                + "    )";
        Assert.assertEquals(expected, strWriter.toString());
    }

}