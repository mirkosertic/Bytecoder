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
package de.mirkosertic.bytecoder.asm.backend.wasm;

import de.mirkosertic.bytecoder.asm.backend.CompileOptions;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Exporter;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Module;
import de.mirkosertic.bytecoder.asm.parser.CompileUnit;
import de.mirkosertic.bytecoder.backend.CompileResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class WasmBackend {

    public WasmCompileResult generateCodeFor(final CompileUnit compileUnit, final CompileOptions compileOptions) {

        final Module module = new Module("bytecoder", compileOptions.getFilenamePrefix() + ".wasm.map");

        final StringWriter theStringWriter = new StringWriter();
        final ByteArrayOutputStream theBinaryOutput = new ByteArrayOutputStream();
        final StringWriter theBinarySourceMap = new StringWriter();
        try {
            final PrintWriter theWriter = new PrintWriter(theStringWriter);
            final Exporter exporter = new Exporter(compileOptions);
            exporter.export(module, theWriter);
            exporter.export(module, theBinaryOutput, theBinarySourceMap);

            theBinaryOutput.flush();
            theStringWriter.flush();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        final WasmCompileResult result = new WasmCompileResult();
        result.add(new CompileResult.BinaryContent(compileOptions.getFilenamePrefix() + "wasmclasses.wasm", theBinaryOutput.toByteArray()));
        result.add(new CompileResult.StringContent(compileOptions.getFilenamePrefix() + "wasmclasses.wasm.map", theStringWriter.toString()));

        return result;
    }
}
