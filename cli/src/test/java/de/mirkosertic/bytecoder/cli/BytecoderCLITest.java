/*
 * Copyright 2023 Mirko Sertic
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
package de.mirkosertic.bytecoder.cli;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import picocli.CommandLine;

public class BytecoderCLITest {

    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    public static class MainClass {

        public static void main(final String[] args) {
            final int i = args.length + 10;
        }

    }

    @Test
    public void testCompileToJS() {
        Assert.assertEquals(new CommandLine(new BytecoderCommand()).execute("compile", "js", "-builddirectory=./target", "-classpath=.", "-mainclass=de.mirkosertic.bytecoder.cli.BytecoderCLITest$MainClass"), 0);
    }
    @Test
    public void testCompileToJSMultipleClasspath() {
        Assert.assertEquals(new CommandLine(new BytecoderCommand()).execute("compile", "js", "-builddirectory=./target", "-classpath=./some/nonexisting/path,.", "-mainclass=de.mirkosertic.bytecoder.cli.BytecoderCLITest$MainClass"), 0);
    }

    @Test
    public void testCompileToWasm() {
        Assert.assertEquals(new CommandLine(new BytecoderCommand()).execute("compile", "wasm", "-builddirectory=./target", "-classpath=.", "-mainclass=de.mirkosertic.bytecoder.cli.BytecoderCLITest$MainClass"), 0);
    }

    @Test
    public void testCompileToWasmMultipleClasspath() {
        Assert.assertEquals(new CommandLine(new BytecoderCommand()).execute("compile", "wasm", "-builddirectory=./target", "-classpath=.,./some/nonexisiting/path", "-mainclass=de.mirkosertic.bytecoder.cli.BytecoderCLITest$MainClass"), 0);
    }

    @Test
    public void testGenerateGraph() {
        Assert.assertEquals(new CommandLine(new BytecoderCommand()).execute("graph", "generate", "-builddirectory=./target", "-classpath=.", "-mainclass=de.mirkosertic.bytecoder.cli.BytecoderCLITest$MainClass"), 0);
    }
}
