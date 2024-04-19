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
package de.mirkosertic.bytecoder.core.backend.wasm.ast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NameSection extends ModuleSection {

    public NameSection(final Module module) {
        super(module);
    }

    public void writeCodeTo(final BinaryWriter writer, final WasmValue.ExportContext exportContext) throws IOException {
        try (final BinaryWriter.SectionWriter sectionWriter = writer.customSection()) {
            sectionWriter.writeUTF8("name");
            try (final BinaryWriter.SectionWriter moduleSection = sectionWriter.subSection((byte) 0)) {
                moduleSection.writeUTF8(getModule().getLabel());
            }
            final FunctionIndex functions = exportContext.functionIndex();
            try (final BinaryWriter.SectionWriter functionSection = sectionWriter.subSection((byte) 1)) {
                functionSection.writeUnsignedLeb128(functions.size());
                for (int i=0;i<functions.size();i++) {
                    final Function f = functions.get(i);
                    functionSection.writeUnsignedLeb128(i);
                    functionSection.writeUTF8(f.getLabel());
                }
            }
            try (final BinaryWriter.SectionWriter localSection = sectionWriter.subSection((byte) 2)) {
                localSection.writeUnsignedLeb128(functions.size());
                for (int i=0;i<functions.size();i++) {
                    final Function f = functions.get(i);
                    localSection.writeUnsignedLeb128(i);
                    final List<String> labels = new ArrayList<>();
                    if (f.getParams() != null) {
                        for (final Param p : f.getParams()) {
                            labels.add(p.getLabel());
                        }
                    }
                    if (f instanceof ExportableFunction) {
                        final ExportableFunction ef = (ExportableFunction) f;
                        final LocalIndex localIndex = ef.localIndex();
                        for (final Local l : localIndex.localsExcludingParams()) {
                            labels.add(l.getLabel());
                        }
                    }
                    localSection.writeUnsignedLeb128(labels.size());
                    for (int j=0;j<labels.size();j++) {
                        localSection.writeUnsignedLeb128(j);
                        localSection.writeUTF8(labels.get(j));
                    }
                }
            }
            final TagIndex tagIndex = getModule().tagIndex();
            if (!tagIndex.isEmpty()) {
                try (final BinaryWriter.SectionWriter functionSection = sectionWriter.subSection((byte) 11)) {
                    functionSection.writeUnsignedLeb128(tagIndex.size());
                    for (int i = 0; i < tagIndex.size(); i++) {
                        final Tag f = tagIndex.get(i);
                        functionSection.writeUnsignedLeb128(i);
                        functionSection.writeUTF8(f.getLabel());
                    }
                }
            }

            final List<StructType> structTypes = getModule().getTypes().structTypes();
            if (!structTypes.isEmpty()) {
                try (final BinaryWriter.SectionWriter functionSection = sectionWriter.subSection((byte) 4)) {
                    functionSection.writeUnsignedLeb128(structTypes.size());
                    for (int i = 0; i < structTypes.size(); i++) {
                        final StructType f = structTypes.get(i);
                        functionSection.writeUnsignedLeb128(getModule().getTypes().indexOf(f));
                        functionSection.writeUTF8(f.name);
                    }
                }

                try (final BinaryWriter.SectionWriter functionSection = sectionWriter.subSection((byte) 10)) {
                    functionSection.writeUnsignedLeb128(structTypes.size());
                    for (int i = 0; i < structTypes.size(); i++) {
                        final StructType f = structTypes.get(i);
                        functionSection.writeUnsignedLeb128(getModule().getTypes().indexOf(f));
                        functionSection.writeUnsignedLeb128(f.fields.size());
                        for (int j = 0; j < f.fields.size(); j++) {
                            final StructType.Field k = f.fields.get(j);
                            functionSection.writeUnsignedLeb128(j);
                            functionSection.writeUTF8(k.name);
                        }
                    }
                }

            }
        }
    }
}
