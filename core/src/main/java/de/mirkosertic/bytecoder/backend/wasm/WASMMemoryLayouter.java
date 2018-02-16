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

import de.mirkosertic.bytecoder.core.BytecodeField;
import de.mirkosertic.bytecoder.core.BytecodeFieldMap;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;

public class WASMMemoryLayouter {

    public static final int CLASS_HEADER_SIZE = 16; // Object header plus initialization status + enum values offset
    public static final int OBJECT_HEADER_SIZE = 8;
    public static final int OBJECT_FIELDSIZE = 4;

    public interface MemoryLayout {

        int offsetForInstanceMember(String aName);

        int offsetForClassMember(String aName);

        int instanceSize();

        int classSize();
    }

    private final Map<BytecodeObjectTypeRef, BytecodeFieldMap> instanceFields;
    private final Map<BytecodeObjectTypeRef, BytecodeFieldMap> classFields;

    public WASMMemoryLayouter(BytecodeLinkerContext aLinkerContext) {
        instanceFields = new HashMap<>();
        classFields = new HashMap<>();
        aLinkerContext.linkedClasses().forEach(aEntry -> registerClass(aEntry.targetNode()));
    }

    private void registerClass(BytecodeLinkedClass aClass) {
        instanceFields.put(aClass.getClassName(), aClass.instanceFieldMap());
        classFields.put(aClass.getClassName(), aClass.staticFieldMap());
    }

    public MemoryLayout layoutFor(BytecodeObjectTypeRef aType) {
        if (!classFields.containsKey(aType)) {
            throw new IllegalArgumentException("No field information found for " + aType.name());
        }
        return new MemoryLayout() {
            @Override
            public int instanceSize() {
                BytecodeFieldMap theInstanceFields = instanceFields.get(aType);
                return OBJECT_HEADER_SIZE + OBJECT_FIELDSIZE * (int) theInstanceFields.stream().count();
            }

            @Override
            public int classSize() {
                BytecodeFieldMap theClassFields = classFields.get(aType);
                return CLASS_HEADER_SIZE + OBJECT_FIELDSIZE * (int) theClassFields.stream().count();
            }

            @Override
            public int offsetForInstanceMember(String aName) {
                BytecodeFieldMap theInstanceFields = instanceFields.get(aType);
                List<BytecodeFieldMap.Entry<BytecodeField>> theFields = theInstanceFields.stream().collect(Collectors.toList());
                int theOffset = OBJECT_HEADER_SIZE;
                for (BytecodeFieldMap.Entry<BytecodeField> theField : theFields) {
                    if (Objects.equals(aName, theField.getValue().getName().stringValue())) {
                        return theOffset;
                    }
                    theOffset+= OBJECT_FIELDSIZE;
                }
                throw new IllegalArgumentException("Member field " + aName + " not found for type " + aType.name());
            }

            @Override
            public int offsetForClassMember(String aName) {
                BytecodeFieldMap theInstanceFields = classFields.get(aType);
                List<BytecodeFieldMap.Entry<BytecodeField>> theFields = theInstanceFields.stream().collect(Collectors.toList());
                int theOffset = CLASS_HEADER_SIZE;
                for (BytecodeFieldMap.Entry<BytecodeField> theField : theFields) {
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