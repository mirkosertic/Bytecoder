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
package de.mirkosertic.bytecoder.core.backend.wasm.ast;


import de.mirkosertic.bytecoder.core.backend.CompileOptions;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class Exporter {

    private final CompileOptions compileOptions;

    public Exporter(final CompileOptions options) {
        this.compileOptions = options;
    }

    public void export(final Module module, final PrintWriter pw) throws IOException {
        try (final TextWriter writer = new TextWriter(pw)) {
            module.writeTo(writer, compileOptions.isDebugOutput());
        }
        pw.flush();
    }

    public void export(final Module module, final OutputStream binaryOutput) throws IOException {
        try (final BinaryWriter binaryWriter = new BinaryWriter()) {
            module.writeTo(binaryWriter, compileOptions.isDebugOutput());
            final byte[] theData = binaryWriter.toByteArray();
            System.out.println("Output is " + theData.length);
            binaryOutput.write(theData);
        }
        binaryOutput.flush();
    }
}
