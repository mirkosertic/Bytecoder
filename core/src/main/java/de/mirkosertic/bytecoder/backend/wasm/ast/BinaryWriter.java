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

import de.mirkosertic.bytecoder.backend.SourceMapWriter;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.ssa.DebugInformation;
import de.mirkosertic.bytecoder.ssa.DebugPosition;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.Program;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BinaryWriter implements AutoCloseable {

    private static int writeUnsignedLeb128(int value, final OutputStream os) throws IOException {
        int count = 0;
        int remaining = value >>> 7;

        while (0 != remaining) {
            os.write((byte) ((value & 0x7f) | 0x80));
            count++;
            value = remaining;
            remaining >>>= 7;
        }

        os.write((byte) (value & 0x7f));
        count++;
        return count;
    }

    private static int writeSignedLeb128(int value, final OutputStream os) throws IOException {
        int remaining = value >> 7;
        boolean hasMore = true;
        final int end = (0 == (value & Integer.MIN_VALUE)) ? 0 : -1;

        int count = 0;

        while (hasMore) {
            hasMore = (remaining != end)
                    || ((remaining & 1) != ((value >> 6) & 1));

            os.write((byte) ((value & 0x7f) | (hasMore ? 0x80 : 0)));
            count++;
            value = remaining;
            remaining >>= 7;
        }

        return count;
    }


    public abstract class Writer implements AutoCloseable {

        protected class DebugInfo {
            protected final int binaryPosition;
            protected final Expression expression;

            DebugInfo(final int binaryPosition, final Expression expression) {
                this.binaryPosition = binaryPosition;
                this.expression = expression;
            }
        }

        protected final OutputStream flushTarget;
        protected final ByteArrayOutputStream bos;
        protected final int offset;

        protected Writer(final OutputStream flushTarget, final int offset) {
            this.flushTarget = flushTarget;
            this.bos = new ByteArrayOutputStream();
            this.offset = offset;
        }

        public BlockWriter blockWriter() {
            return new BlockWriter(bos, offset + bos.size());
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

        public void registerDebugInformationFor(final Expression aExpression) {
            if (aExpression != null) {
                debugInfos.add(new DebugInfo(offset + bos.size(), aExpression));
            }
        }
    }

    public class BlockWriter extends Writer {

        public BlockWriter(final OutputStream flushTarget, final int offset) {
            super(flushTarget, offset);
        }

        @Override
        public void close() throws IOException {
            bos.flush();

            final byte[] data = bos.toByteArray();
            BinaryWriter.writeUnsignedLeb128(data.length, flushTarget);
            flushTarget.write(data);
        }
    }

    public class SectionWriter extends Writer {

        private final byte sectionCode;

        public SectionWriter(final byte sectionCode, final OutputStream flushTarget, final int offset) {
            super(flushTarget, offset);
            this.sectionCode = sectionCode;
        }

        public SectionWriter subSection(final byte subSectionCode) {
            return new SectionWriter(subSectionCode, bos, offset + bos.size());
        }

        @Override
        public void close() throws IOException {
            bos.flush();
            final byte[] data = bos.toByteArray();

            int theDelta = offset;

            flushTarget.write(sectionCode);
            theDelta++;

            theDelta += BinaryWriter.writeUnsignedLeb128(data.length, flushTarget);
            flushTarget.write(data);

            for (final DebugInfo theInfo : debugInfos) {
                final Program theProgram = theInfo.expression.getProgram();
                if (theProgram != null) {
                    final DebugInformation theDebugInformation = theProgram.getDebugInformation();
                    if (theDebugInformation != null) {
                        final BytecodeOpcodeAddress theAddress = theInfo.expression.getAddress();
                        if (theAddress != null) {
                            final DebugPosition thePos = theDebugInformation.debugPositionFor(theAddress);
                            if (thePos != null) {
                                final int theRealPositionInFile = theDelta + theInfo.binaryPosition;
                                sourceMapWriter.assignDebugPosition(0, theRealPositionInFile, thePos);
                            }
                        }
                    }
                }
            }
        }
    }

    private final ByteArrayOutputStream os;
    private final SourceMapWriter sourceMapWriter;
    private final List<Writer.DebugInfo> debugInfos;

    public BinaryWriter(final SourceMapWriter sourceMapWriter) {
        this.os = new ByteArrayOutputStream();
        this.sourceMapWriter = sourceMapWriter;
        this.debugInfos = new ArrayList<>();
    }

    public byte[] toByteArray() throws IOException {
        os.flush();
        return os.toByteArray();
    }

    @Override
    public void close() throws IOException {
        os.close();
    }

    public void header() throws IOException {
        os.write(new byte[] {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00});
    }

    public SectionWriter typeSection() {
        return new SectionWriter((byte) 1, os, os.size());
    }

    public SectionWriter importsSection() {
        return new SectionWriter((byte) 2, os, os.size());
    }

    public SectionWriter functionSection() {
        return new SectionWriter((byte) 3, os, os.size());
    }

    public SectionWriter tablesSection() {
        return new SectionWriter((byte) 4, os, os.size());
    }

    public SectionWriter memorySection() {
        return new SectionWriter((byte) 5, os, os.size());
    }

    public SectionWriter globalsSection() {
        return new SectionWriter((byte) 6, os, os.size());
    }

    public SectionWriter exportsSection() {
        return new SectionWriter((byte) 7, os, os.size());
    }

    public SectionWriter elementsSection() {
        return new SectionWriter((byte) 9, os, os.size());
    }

    public SectionWriter codeSection() {
        return new SectionWriter((byte) 10, os, os.size());
    }

    public SectionWriter customSection() {
        return new SectionWriter((byte) 0, os, os.size());
    }
}