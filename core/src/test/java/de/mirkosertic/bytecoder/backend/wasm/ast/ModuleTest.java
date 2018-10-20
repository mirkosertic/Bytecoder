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

import static de.mirkosertic.bytecoder.backend.wasm.ast.Expressions.control;
import static de.mirkosertic.bytecoder.backend.wasm.ast.Expressions.i32;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

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

        Assert.assertEquals("(module \n"
                + "    )", strWriter.toString());
    }

    @Test
    public void testSimpleCaseBinary() throws IOException {
        final Module module = new Module();
        final FileOutputStream fos = new FileOutputStream("D:\\Temp\\wasm.wasm");
        final Exporter exporter = new Exporter();
        exporter.export(module, fos);
    }

    @Test
    public void testSimpleBinaryFunction() throws IOException {

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(new Param("p1", PrimitiveType.i32)), PrimitiveType.i32);
        function.addChild(control.ret(i32.c(42)));
        function.exportAs("expfunction");

        final FileOutputStream fos = new FileOutputStream("D:\\Temp\\wasm.wasm");
        final Exporter exporter = new Exporter();
        exporter.export(module, fos);
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
    public void testModuleWithSimpleFunction() throws IOException {

        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final ExportableFunction function = functionsContent.newFunction("label", Arrays
                .asList(new Param("p1", PrimitiveType.i32)), PrimitiveType.i32);
        function.addChild(control.ret(i32.c(42)));
        function.exportAs("expfunction");

        try (final TextWriter writer = new TextWriter(pw)) {
            module.writeTo(writer);
        }

        Assert.assertEquals("(module \n"
                + "    (func $label (param $p1 i32) (result i32)\n"
                + "        (return (i32.const 42)))\n"
                + "    (type $t0 (func (param i32) (result i32)))\n"
                + "    (export \"expfunction\" (func $label))\n"
                + "    )", strWriter.toString());

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
}