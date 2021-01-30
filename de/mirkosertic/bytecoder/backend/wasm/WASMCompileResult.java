/*
 * Copyright 2017 Mirko Sertic
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
package de.mirkosertic.bytecoder.backend.wasm;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import de.mirkosertic.bytecoder.backend.CompileResult;

public class WASMCompileResult extends CompileResult<String> {

    public abstract static class WASMCompileContent implements CompileResult.Content {

        public WASMCompileContent() {
        }
    }

    public static class WASMTextualCompileResult extends WASMCompileContent {

        private final String data;
        private final String filenamePrefix;

        public WASMTextualCompileResult(final String data, final String afilenamePrefix) {
            this.data = data;
            this.filenamePrefix = afilenamePrefix;
        }

        @Override
        public String getFileName() {
            return filenamePrefix + ".wat";
        }

        @Override
        public void writeTo(final OutputStream stream) {
            try (final PrintStream ps = new PrintStream(stream)) {
                ps.print(data);
            }
        }
    }

    public static class WASMTextualJSCompileResult extends WASMCompileContent {

        private final String data;
        private final String filenamePrefix;

        public WASMTextualJSCompileResult(final String data, final String afilenamePrefix) {
            this.data = data;
            this.filenamePrefix = afilenamePrefix;
        }

        @Override
        public String getFileName() {
            return filenamePrefix + ".js";
        }

        @Override
        public void writeTo(final OutputStream stream) {
            try (final PrintStream ps = new PrintStream(stream)) {
                ps.print(data);
            }
        }
    }

    public static class WASMSourcemapCompileResult extends WASMCompileContent {

        private final String data;
        private final String filenamePrefix;

        public WASMSourcemapCompileResult(final String data, final String afilenamePrefix) {
            this.data = data;
            this.filenamePrefix = afilenamePrefix;
        }

        @Override
        public String getFileName() {
            return filenamePrefix + ".wasm.map";
        }

        @Override
        public void writeTo(final OutputStream stream) {
            try (final PrintStream ps = new PrintStream(stream)) {
                ps.print(data);
            }
        }
    }

    public static class WASMBinaryCompileResult extends WASMCompileContent {

        private final byte[] data;
        private final String filenamePrefix;

        public WASMBinaryCompileResult(final byte[] data, final String filenamePrefix) {
            this.data = data;
            this.filenamePrefix = filenamePrefix;
        }

        @Override
        public String getFileName() {
            return filenamePrefix + ".wasm";
        }

        @Override
        public void writeTo(final OutputStream stream) throws IOException {
            stream.write(data);
        }
    }

    private final WASMMinifier minifier;

    public WASMCompileResult(
            final WASMMinifier minifier,
            final WASMCompileContent... content) {
        this.minifier = minifier;
        for (final WASMCompileContent c : content) {
            add(c);
        }
    }

    public WASMMinifier getMinifier() {
        return minifier;
    }
}