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
package de.mirkosertic.bytecoder.backend.js;

import de.mirkosertic.bytecoder.backend.CompileResult;

import java.io.OutputStream;
import java.io.PrintStream;

public class JSCompileResult implements CompileResult<String> {

    public static class JSContent implements Content {

        private final String fileName;
        private final String data;

        public JSContent(final String fileName, final String data) {
            this.fileName = fileName;
            this.data = data;
        }

        @Override
        public String getFileName() {
            return fileName;
        }

        @Override
        public void writeTo(final OutputStream stream) {
            try (final PrintStream ps = new PrintStream(stream)) {
                ps.print(data);
            }
        }
    }

    private final JSContent[] content;
    private final JSMinifier minifier;

    public JSCompileResult(final JSMinifier aMinifier, final JSContent... content) {
        this.minifier = aMinifier;
        this.content = content;
    }

    @Override
    public JSContent[] getContent() {
        return content;
    }

    public JSMinifier getMinifier() {
        return minifier;
    }
}