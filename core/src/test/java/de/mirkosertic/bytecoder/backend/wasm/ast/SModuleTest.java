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

import org.junit.Assert;
import org.junit.Test;

public class SModuleTest {

    @Test
    public void testSimpleCase() throws IOException {
        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();
        final STextExporter exporter = new STextExporter();
        exporter.export(module, pw);

        Assert.assertEquals("(module \n"
                + "    )", strWriter.toString());
    }

    @Test
    public void testWithMemory() throws IOException {

        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();

        final SMemory memory = new SMemory(10, 20);
        module.getMems().addChild(memory);

        final STextExporter exporter = new STextExporter();
        exporter.export(module, pw);

        Assert.assertEquals("(module \n"
                + "    (memory 10 20)\n"
                + "    )", strWriter.toString());
    }

    @Test
    public void testWithExportedMemory() throws IOException {

        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final Module module = new Module();

        final SMemory memory = new SMemory(new SExport("exported"), 10, 20);
        module.getMems().addChild(memory);

        final STextExporter exporter = new STextExporter();
        exporter.export(module, pw);

        Assert.assertEquals("(module \n"
                + "    (memory (export \"exported\") 10 20)\n"
                + "    )", strWriter.toString());
    }
}