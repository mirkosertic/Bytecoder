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
import java.io.IOException;
import java.io.OutputStream;

public class BinaryWriter implements AutoCloseable {

    public class SectionWriter implements AutoCloseable {

        private final byte sectionCode;
        private final ByteArrayOutputStream bos;

        public SectionWriter(final byte sectionCode) {
            this.sectionCode = sectionCode;
            this.bos = new ByteArrayOutputStream();
        }

        public void writeUnsignedLeb128(int value) {
            int remaining = value >>> 7;

            while (0 != remaining) {
                bos.write((byte) ((value & 0x7f) | 0x80));
                value = remaining;
                remaining >>>= 7;
            }

            bos.write((byte) (value & 0x7f));
        }

        public void writeSignedLeb128(int value) {
            int remaining = value >> 7;
            boolean hasMore = true;
            final int end = (0 == (value & Integer.MIN_VALUE)) ? 0 : -1;

            while (hasMore) {
                hasMore = (remaining != end)
                        || ((remaining & 1) != ((value >> 6) & 1));

                bos.write((byte) ((value & 0x7f) | (hasMore ? 0x80 : 0)));
                value = remaining;
                remaining >>= 7;
            }
        }

        @Override
        public void close() throws IOException {
            bos.flush();
            final byte[] data = bos.toByteArray();


            BinaryWriter.this.writeByte(sectionCode);
            BinaryWriter.this.writeUnsignedLeb128(data.length);
            BinaryWriter.this.writeBytes(data);
        }
    }

    private final OutputStream os;

    public BinaryWriter(final OutputStream os) {
        this.os = os;
    }

    @Override
    public void close() throws IOException {
        os.close();
    }

    public void writeByte(final int value) throws IOException {
        os.write(value);
    }

    public void writeBytes(final byte... data) throws IOException {
        os.write(data);
    }

    public void writeUnsignedLeb128(int value) throws IOException {
        int remaining = value >>> 7;

        while (0 != remaining) {
            os.write((byte) ((value & 0x7f) | 0x80));
            value = remaining;
            remaining >>>= 7;
        }

        os.write((byte) (value & 0x7f));
    }

    public void writeSignedLeb128(int value) throws IOException {
        int remaining = value >> 7;
        boolean hasMore = true;
        final int end = (0 == (value & Integer.MIN_VALUE)) ? 0 : -1;

        while (hasMore) {
            hasMore = (remaining != end)
                    || ((remaining & 1) != ((value >> 6) & 1));

            os.write((byte) ((value & 0x7f) | (hasMore ? 0x80 : 0)));
            value = remaining;
            remaining >>= 7;
        }
    }

    public void header() throws IOException {
        os.write(new byte[] {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00});
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
