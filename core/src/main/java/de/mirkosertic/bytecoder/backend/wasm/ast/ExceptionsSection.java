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

import java.io.IOException;
import java.util.List;

public class ExceptionsSection extends ModuleSection {

    private final ExceptionIndex exceptionIndex;

    public ExceptionsSection(final Module module) {
        super(module);
        exceptionIndex = new ExceptionIndex();
    }

    public ExceptionIndex exceptionIndex() {
        return exceptionIndex;
    }

    public WASMException newException(final String label, final List<PrimitiveType> arguments) {
        final WASMException e = new WASMException(this, label, arguments);
        exceptionIndex.add(e);
        return e;
    }

    public void writeCodeTo(final BinaryWriter writer) throws IOException {
        if (!exceptionIndex.isEmpty()) {
            try (final BinaryWriter.SectionWriter sectionWriter = writer.customSection()) {
                sectionWriter.writeUTF8("exception");
                sectionWriter.writeUnsignedLeb128(exceptionIndex.size());
                for (int i = 0; i< exceptionIndex.size(); i++) {
                    final WASMException event = exceptionIndex.get(i);
                    event.writeTo(sectionWriter);
                }
            }
        }
    }

    public void writeCodeTo(final TextWriter writer) throws IOException {
        for (int i = 0; i< exceptionIndex.size(); i++) {
            final WASMException event = exceptionIndex.get(i);
            event.writeTo(writer);
        }
    }
}
