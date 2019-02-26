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
import java.util.List;

import de.mirkosertic.bytecoder.backend.CompileResult;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;

public class WASMCompileResult implements CompileResult<String> {

    public abstract static class WASMCompileContent implements Content {

        private final WASMMemoryLayouter memoryLayouter;
        private final BytecodeLinkerContext linkerContext;
        private final List<String> generatedFunctions;

        public WASMCompileContent(final WASMMemoryLayouter memoryLayouter, final BytecodeLinkerContext linkerContext,
                                  final List<String> generatedFunctions) {
            this.memoryLayouter = memoryLayouter;
            this.linkerContext = linkerContext;
            this.generatedFunctions = generatedFunctions;
        }

        public int getTypeIDFor(final BytecodeObjectTypeRef aObjecType) {
            return linkerContext.resolveClass(aObjecType).getUniqueId();
        }

        public int getSizeOf(final BytecodeObjectTypeRef aObjectType) {
            final WASMMemoryLayouter.MemoryLayout theLayout = memoryLayouter.layoutFor(aObjectType);
            return theLayout.instanceSize();
        }

        public int getVTableIndexOf(final BytecodeObjectTypeRef aObjectType) {
            final String theClassName = WASMWriterUtils.toClassName(aObjectType);

            final String theMethodName = theClassName + "__resolvevtableindex";
            return generatedFunctions.indexOf(theMethodName);
        }
    }

    public static class WASMTextualCompileResult extends WASMCompileContent {

        private final String data;
        private final String filenamePrefix;

        public WASMTextualCompileResult(final WASMMemoryLayouter memoryLayouter, final BytecodeLinkerContext linkerContext,
                                        final List<String> generatedFunctions, final String data, final String afilenamePrefix) {
            super(memoryLayouter, linkerContext, generatedFunctions);
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

        public WASMTextualJSCompileResult(final WASMMemoryLayouter memoryLayouter, final BytecodeLinkerContext linkerContext,
                                        final List<String> generatedFunctions, final String data, final String afilenamePrefix) {
            super(memoryLayouter, linkerContext, generatedFunctions);
            this.data = data;
            this.filenamePrefix = afilenamePrefix;
        }

        @Override
        public String getFileName() {
            return filenamePrefix + "_wasmbindings.js";
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

        public WASMSourcemapCompileResult(final WASMMemoryLayouter memoryLayouter, final BytecodeLinkerContext linkerContext,
                final List<String> generatedFunctions, final String data, final String afilenamePrefix) {
            super(memoryLayouter, linkerContext, generatedFunctions);
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

        public WASMBinaryCompileResult(final WASMMemoryLayouter memoryLayouter, final BytecodeLinkerContext linkerContext,
                                       final List<String> generatedFunctions, final byte[] data, final String filenamePrefix) {
            super(memoryLayouter, linkerContext, generatedFunctions);
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

    private final WASMCompileContent[] content;
    private final WASMMinifier minifier;

    public WASMCompileResult(
            final WASMMinifier minifier,
            final WASMCompileContent... content) {
        this.minifier = minifier;
        this.content = content;
    }

    public WASMMinifier getMinifier() {
        return minifier;
    }

    @Override
    public WASMCompileContent[] getContent() {
        return content;
    }
}