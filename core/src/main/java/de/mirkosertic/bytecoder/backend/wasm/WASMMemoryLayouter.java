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

import com.google.common.collect.Lists;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final Map<BytecodeObjectTypeRef, List<BytecodeLinkedClass.LinkedField>> classes;
    private final Map<BytecodeObjectTypeRef, BytecodeObjectTypeRef> superClasses;

    public WASMMemoryLayouter(BytecodeLinkerContext aLinkerContext) {
        classes = new HashMap<>();
        superClasses = new HashMap<>();
        aLinkerContext.forEachClass(aEntry -> registerClass(aEntry.getValue()));
   }

    private List<BytecodeLinkedClass.LinkedField> collectFields(BytecodeLinkedClass aClass) {
        List<BytecodeLinkedClass.LinkedField> theFields = new ArrayList<>();

        aClass.forEachMemberField(aEntry -> {
            BytecodeLinkedClass.LinkedField theField = aEntry.getValue();
            if (theField.getDeclaringType().equals(aClass.getClassName())) {
                theFields.add(theField);
            }
        });

        aClass.forEachStaticField(aEntry -> {
            BytecodeLinkedClass.LinkedField theField = aEntry.getValue();
            if (theField.getDeclaringType().equals(aClass.getClassName())) {
                theFields.add(theField);
            }
        });

        return theFields;
    }

    private void registerClass(BytecodeLinkedClass aClass) {
        if (aClass.getSuperClass() != null) {
            registerClass(aClass.getSuperClass());
        }

        if (!classes.containsKey(aClass)) {
            classes.put(aClass.getClassName(), collectFields(aClass));
            if (aClass.getSuperClass() != null) {
                superClasses.put(aClass.getClassName(), aClass.getSuperClass().getClassName());
            }
        }
    }

    private List<BytecodeLinkedClass.LinkedField> allFieldsFor(BytecodeObjectTypeRef aType) {
        List<BytecodeLinkedClass.LinkedField> theFoundFields = new ArrayList<>();
        BytecodeObjectTypeRef theStartingType = aType;
        while (theStartingType != null) {
            theFoundFields.addAll(classes.get(theStartingType));
            theStartingType = superClasses.get(theStartingType);
        }
        // We inverse the list, so the the fields from the inheritance root comes first in the list, and not last
        return Lists.reverse(theFoundFields);
    }


    public MemoryLayout layoutFor(BytecodeObjectTypeRef aType) {
        if (!classes.containsKey(aType)) {
            throw new IllegalArgumentException("No field information found for " + aType.name());
        }
        return new MemoryLayout() {

            @Override
            public int instanceSize() {
                List<BytecodeLinkedClass.LinkedField> theFields = allFieldsFor(aType);
                int theOffset = OBJECT_HEADER_SIZE;
                for (BytecodeLinkedClass.LinkedField theField : theFields) {
                    if (!theField.getField().getAccessFlags().isStatic()) {
                        theOffset+= OBJECT_FIELDSIZE;
                    }
                }
                return theOffset;
            }

            @Override
            public int classSize() {
                List<BytecodeLinkedClass.LinkedField> theFields = allFieldsFor(aType);
                int theOffset = CLASS_HEADER_SIZE;
                for (BytecodeLinkedClass.LinkedField theField : theFields) {
                    if (theField.getField().getAccessFlags().isStatic()) {
                        theOffset+= OBJECT_FIELDSIZE;
                    }
                }
                return theOffset;
            }

            @Override
            public int offsetForInstanceMember(String aName) {
                List<BytecodeLinkedClass.LinkedField> theFields = allFieldsFor(aType);
                int theOffset = OBJECT_HEADER_SIZE;
                for (BytecodeLinkedClass.LinkedField theField : theFields) {
                    if (!theField.getField().getAccessFlags().isStatic()) {
                        if (aName.equals(theField.getField().getName().stringValue())) {
                            return theOffset;
                        }
                        theOffset+= OBJECT_FIELDSIZE;
                    }
                }
                throw new IllegalArgumentException("Field " + aName + " not found for type " + aType.name());
            }

            @Override
            public int offsetForClassMember(String aName) {
                List<BytecodeLinkedClass.LinkedField> theFields = allFieldsFor(aType);
                int theOffset = CLASS_HEADER_SIZE;
                for (BytecodeLinkedClass.LinkedField theField : theFields) {
                    if (theField.getField().getAccessFlags().isStatic()) {
                        if (aName.equals(theField.getField().getName().stringValue())) {
                            return theOffset;
                        }
                        theOffset+= OBJECT_FIELDSIZE;
                    }
                }
                throw new IllegalArgumentException("Static field " + aName + " not found for type " + aType.name());
            }
        };
    }
}
