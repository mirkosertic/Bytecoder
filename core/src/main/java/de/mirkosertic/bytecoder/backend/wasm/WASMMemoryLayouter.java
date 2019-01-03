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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeResolvedFields;

public class WASMMemoryLayouter {

    public static final int CLASS_HEADER_SIZE = 20; // Object header plus initialization status + enum values offset + classname
    public static final int OBJECT_HEADER_SIZE = 8;
    public static final int OBJECT_FIELDSIZE = 4;

    public interface MemoryLayout {

        int offsetForInstanceMember(String aName);

        int offsetForClassMember(String aName);

        int instanceSize();

        int classSize();
    }

    private final Map<BytecodeObjectTypeRef, BytecodeResolvedFields> fields;

    public WASMMemoryLayouter(BytecodeLinkerContext aLinkerContext) {
        fields = new HashMap<>();
        aLinkerContext.linkedClasses().forEach(aEntry -> registerClass(aEntry.targetNode()));
    }

    private void registerClass(BytecodeLinkedClass aClass) {
        fields.put(aClass.getClassName(), aClass.resolvedFields());
    }

    public MemoryLayout layoutFor(BytecodeObjectTypeRef aType) {
        if (!fields.containsKey(aType)) {
            throw new IllegalArgumentException("No field information found for " + aType.name());
        }
        return new MemoryLayout() {
            @Override
            public int instanceSize() {
                BytecodeResolvedFields theInstanceFields = fields.get(aType);
                return OBJECT_HEADER_SIZE + OBJECT_FIELDSIZE * (int) theInstanceFields.streamForInstanceFields().count();
            }

            @Override
            public int classSize() {
                BytecodeResolvedFields theClassFields = fields.get(aType);
                return CLASS_HEADER_SIZE + OBJECT_FIELDSIZE * (int) theClassFields.streamForStaticFields().count();
            }

            @Override
            public int offsetForInstanceMember(String aName) {
                BytecodeResolvedFields theInstanceFields = fields.get(aType);
                List<BytecodeResolvedFields.FieldEntry> theFields = theInstanceFields.streamForInstanceFields().collect(Collectors.toList());
                int theOffset = OBJECT_HEADER_SIZE;
                for (BytecodeResolvedFields.FieldEntry theField : theFields) {
                    if (Objects.equals(aName, theField.getValue().getName().stringValue())) {
                        return theOffset;
                    }
                    theOffset+= OBJECT_FIELDSIZE;
                }
                throw new IllegalArgumentException("Member field " + aName + " not found for type " + aType.name());
            }

            @Override
            public int offsetForClassMember(String aName) {
                BytecodeResolvedFields theInstanceFields = fields.get(aType);
                List<BytecodeResolvedFields.FieldEntry> theFields = theInstanceFields.streamForStaticFields().collect(Collectors.toList());
                int theOffset = CLASS_HEADER_SIZE;
                for (BytecodeResolvedFields.FieldEntry theField : theFields) {
                    if (Objects.equals(aName, theField.getValue().getName().stringValue())) {
                        return theOffset;
                    }
                    theOffset+= OBJECT_FIELDSIZE;
                }
                throw new IllegalArgumentException("Static field " + aName + " not found for type " + aType.name());
            }
        };
    }
}