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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class BinaryWriter implements AutoCloseable {

    public class SectionWriter implements AutoCloseable {

        private final byte sectionCode;
        private final ByteArrayOutputStream bos;

        public SectionWriter(final byte sectionCode) {
            this.sectionCode = sectionCode;
            this.bos = new ByteArrayOutputStream();
        }

        @Override
        public void close() throws Exception {
        }
    }

    private final OutputStream os;

    public BinaryWriter(final OutputStream os) {
        this.os = os;
    }

    @Override
    public void close() throws Exception {
        os.close();
    }

    public void header() {
    }

    public SectionWriter typeSection() {
        return new SectionWriter((byte) 1);
    }

    public SectionWriter functionSection() {
        return new SectionWriter((byte) 3);
    }

    public SectionWriter memorySection() {
        return new SectionWriter((byte) 5);
    }

    public SectionWriter exportsSection() {
        return new SectionWriter((byte) 7);
    }

    public SectionWriter codeSection() {
        return new SectionWriter((byte) 10);
    }
}
