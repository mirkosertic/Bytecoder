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

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final ExportableFunction function = functionsContent.newFunction("label",
                Collections.singletonList(new Param("p1", PrimitiveType.i32)), PrimitiveType.i32);
        final Expressions exp = function.expressions();
        function.addChild(exp.control.ret(exp.i32.c(42)));
        try (final TextWriter writer = new TextWriter(pw)) {
            function.writeTo(writer);
        }

        Assert.assertEquals("(func $label (type $t0) (param $p1 i32) (result i32)\n"
                + "    (return (i32.const 42)))", strWriter.toString());
    }

    @Test
    public void testNoReturnValue() throws IOException {
        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final FunctionsSection functionsContent = module.getFunctions();
        final ExportableFunction function = functionsContent.newFunction("label",
                Collections.singletonList(new Param("p1", PrimitiveType.i32)));
        final Expressions exp = function.expressions();
        function.addChild(exp.control.ret());
        try (final TextWriter writer = new TextWriter(pw)) {
            function.writeTo(writer);
        }

        Assert.assertEquals("(func $label (type $t0) (param $p1 i32)\n"
                + "    (return))", strWriter.toString());
    }
}