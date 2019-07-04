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

public class EventSection extends ModuleSection {

    private final EventIndex eventIndex;

    public EventSection(final Module module) {
        super(module);
        eventIndex = new EventIndex();
    }

    public EventIndex eventIndex() {
        return eventIndex;
    }

    public WASMEvent newException(final String label, final List<PrimitiveType> arguments) {
        final WASMType type = getModule().getTypes().typeFor(arguments);

        final WASMEvent e = new WASMEvent(getModule().getTypes(), label, type);
        eventIndex.add(e);
        return e;
    }

    public void writeCodeTo(final BinaryWriter writer) throws IOException {
        if (!eventIndex.isEmpty()) {
            try (final BinaryWriter.SectionWriter sectionWriter = writer.eventSection()) {
                sectionWriter.writeUTF8("event");
                sectionWriter.writeUnsignedLeb128(eventIndex.size());
                for (int i = 0; i< eventIndex.size(); i++) {
                    final WASMEvent event = eventIndex.get(i);
                    event.writeTo(sectionWriter);
                }
            }
        }
    }

    public void writeCodeTo(final TextWriter writer) throws IOException {
        for (int i = 0; i< eventIndex.size(); i++) {
            final WASMEvent event = eventIndex.get(i);
            event.writeTo(writer);
        }
    }
}
