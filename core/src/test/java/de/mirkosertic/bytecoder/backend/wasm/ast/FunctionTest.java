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

import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.i32;
import static de.mirkosertic.bytecoder.backend.wasm.ast.ConstExpressions.param;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public class FunctionTest {

    @Test
    public void testSimpleCase() throws IOException {
        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module("mod", "mod.wasm.map");
        final FunctionsSection functionsContent = module.getFunctions();
        final ExportableFunction function = functionsContent.newFunction("label",
                Collections.singletonList(param("p1", PrimitiveType.i32)), PrimitiveType.i32);
        function.flow.ret(i32.c(42, null), null);
        try (final TextWriter writer = new TextWriter(pw)) {
            function.writeTo(writer, module);
        }

        Assert.assertEquals("(func $label (type $t0) (param $p1 i32) (result i32)" + System.lineSeparator()
                + "    (return (i32.const 42))" + System.lineSeparator()
                + "    )", strWriter.toString());
    }

    @Test
    public void testNoReturnValue() throws IOException {
        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module("mod", "mod.wasm.map");
        final FunctionsSection functionsContent = module.getFunctions();
        final ExportableFunction function = functionsContent.newFunction("label",
                Collections.singletonList(param("p1", PrimitiveType.i32)));
        function.flow.ret(null);
        try (final TextWriter writer = new TextWriter(pw)) {
            function.writeTo(writer, module);
        }

        Assert.assertEquals("(func $label (type $t0) (param $p1 i32)" + System.lineSeparator()
                + "    (return)" + System.lineSeparator()
                + "    )", strWriter.toString());
    }
}