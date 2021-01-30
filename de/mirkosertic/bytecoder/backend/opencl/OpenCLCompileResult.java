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
package de.mirkosertic.bytecoder.backend.opencl;

import de.mirkosertic.bytecoder.backend.CompileResult;

import java.io.OutputStream;
import java.io.PrintStream;

public class OpenCLCompileResult extends CompileResult<String> {

    public static class OpenCLContent implements Content {
        private final OpenCLInputOutputs inputOutputs;
        private final String kernelSource;

        public OpenCLContent(final OpenCLInputOutputs inputOutputs, final String kernelSource) {
            this.inputOutputs = inputOutputs;
            this.kernelSource = kernelSource;
        }

        public OpenCLInputOutputs getInputOutputs() {
            return inputOutputs;
        }

        @Override
        public String getFileName() {
            return "BytecoderKernel";
        }

        @Override
        public void writeTo(final OutputStream stream) {
            try (final PrintStream ps = new PrintStream(stream)) {
                ps.print(kernelSource);
            }
        }
    }

    public OpenCLCompileResult(final OpenCLContent... content) {
        for (final OpenCLContent c : content) {
            add(c);
        }
    }
}