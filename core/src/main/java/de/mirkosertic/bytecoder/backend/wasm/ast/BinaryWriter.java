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
import java.nio.charset.StandardCharsets;

public class BinaryWriter implements AutoCloseable {

    private static void writeUnsignedLeb128(int value, final OutputStream os) throws IOException {
        int remaining = value >>> 7;

        while (0 != remaining) {
            os.write((byte) ((value & 0x7f) | 0x80));
            value = remaining;
            remaining >>>= 7;
        }

        os.write((byte) (value & 0x7f));
    }

    private static void writeSignedLeb128(int value, final OutputStream os) throws IOException {
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


    public abstract static class Writer implements AutoCloseable {

        protected final OutputStream flushTarget;
        protected final ByteArrayOutputStream bos;

        protected Writer(final OutputStream flushTarget) {
            this.flushTarget = flushTarget;
            this.bos = new ByteArrayOutputStream();
        }

        public BlockWriter blockWriter() {
            return new BlockWriter(bos);
        }

        public void writeByte(final byte value) {
            bos.write(value);
        }

        public void writeUnsignedLeb128(final int value) throws IOException {
            BinaryWriter.writeUnsignedLeb128(value, bos);
        }

        public void writeSignedLeb128(final int value) throws IOException {
            BinaryWriter.writeSignedLeb128(value, bos);
        }

        public void writeUTF8(final String value) throws IOException {
            final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            writeUnsignedLeb128(bytes.length);
            bos.write(bytes);
        }

        public void writeFloat32(final float value) {
            writeInteger32(Float.floatToRawIntBits(value));
        }

        public void writeInteger32(final int value) {
            writeByte((byte) value);
            writeByte((byte) (value >> 8));
            writeByte((byte) (value >> 16));
            writeByte((byte) (value >> 24));
        }
    }

    public static class BlockWriter extends Writer {

        public BlockWriter(final OutputStream flushTarget) {
            super(flushTarget);
        }

        @Override
        public void close() throws IOException {
            bos.flush();

            final byte[] data = bos.toByteArray();
            BinaryWriter.writeUnsignedLeb128(data.length, flushTarget);
            flushTarget.write(data);
        }
    }

    public static class SectionWriter extends Writer {

        private final byte sectionCode;

        public SectionWriter(final byte sectionCode, final OutputStream flushTarget) {
            super(flushTarget);
            this.sectionCode = sectionCode;
        }

        @Override
        public void close() throws IOException {
            bos.flush();
            final byte[] data = bos.toByteArray();

            flushTarget.write(sectionCode);
            BinaryWriter.writeUnsignedLeb128(data.length, flushTarget);
            flushTarget.write(data);
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

    public void header() throws IOException {
        os.write(new byte[] {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00});
    }

    public SectionWriter typeSection() {
        return new SectionWriter((byte) 1, os);
    }

    public SectionWriter importsSection() {
        return new SectionWriter((byte) 2, os);
    }

    public SectionWriter functionSection() {
        return new SectionWriter((byte) 3, os);
    }

    public SectionWriter tablesSection() {
        return new SectionWriter((byte) 4, os);
    }

    public SectionWriter memorySection() {
        return new SectionWriter((byte) 5, os);
    }

    public SectionWriter globalsSection() {
        return new SectionWriter((byte) 6, os);
    }

    public SectionWriter exportsSection() {
        return new SectionWriter((byte) 7, os);
    }

    public SectionWriter elementsSection() {
        return new SectionWriter((byte) 9, os);
    }

    public SectionWriter codeSection() {
        return new SectionWriter((byte) 10, os);
    }
}