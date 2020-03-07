/*
 * Copyright 2020 Mirko Sertic
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
package de.mirkosertic.bytecoder.core;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class BytecodeVTable {

    public static class Slot {

        private final int pos;

        Slot(final int pos) {
            this.pos = pos;
        }

        public int getPos() {
            return pos;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final Slot slot = (Slot) o;
            return pos == slot.pos;
        }

        @Override
        public int hashCode() {
            return Objects.hash(pos);
        }
    }

    public static class VPtr {

        private final String methodName;
        private final BytecodeMethodSignature signature;
        private final BytecodeObjectTypeRef implementingClass;

        VPtr(final String methodName, final BytecodeMethodSignature signature, final BytecodeObjectTypeRef implementingClass) {
            this.methodName = methodName;
            this.signature = signature;
            this.implementingClass = implementingClass;
        }

        public String getMethodName() {
            return methodName;
        }

        public BytecodeMethodSignature getSignature() {
            return signature;
        }

        public BytecodeObjectTypeRef getImplementingClass() {
            return implementingClass;
        }
    }

    private final Map<Slot, VPtr> slots;

    public BytecodeVTable() {
        this.slots = new HashMap<>();
    }

    public void register(final BytecodeMethod aMethod, final BytecodeLinkedClass aClass) {
        for (final Map.Entry<Slot, VPtr> theEntry : slots.entrySet()) {
            final Slot slot = theEntry.getKey();
            final VPtr vPtr = theEntry.getValue();
            if (aMethod.getName().stringValue().equals(vPtr.methodName) &&
                aMethod.getSignature().matchesExactlyTo(vPtr.signature)) {
                if (!aMethod.getAccessFlags().isAbstract()) {
                    slots.put(slot, new VPtr(aMethod.getName().stringValue(), aMethod.getSignature(), aClass.getClassName()));
                }
                return;
            }
        }
        final Slot newSlot = new Slot(slots.size());
        slots.put(newSlot, new VPtr(aMethod.getName().stringValue(), aMethod.getSignature(), aClass.getClassName()));
    }

    public List<Slot> sortedSlots() {
        return slots.keySet().stream().sorted(Comparator.comparingInt(o -> o.pos)).collect(Collectors.toList());
    }

    public int numberOfSlots() {
        int max = -1;
        for (final Slot s : slots.keySet()) {
            max = Math.max(max, s.pos);
        }
        return max + 1;
    }

    public VPtr slot(final Slot slot) {
        return slots.get(slot);
    }

    public Slot slotOf(final String methodName, final BytecodeMethodSignature signature) {
        for (final Map.Entry<Slot, VPtr> theEntry : slots.entrySet()) {
            final Slot slot = theEntry.getKey();
            final VPtr vPtr = theEntry.getValue();
            if (methodName.equals(vPtr.methodName) &&
                    signature.matchesExactlyTo(vPtr.signature)) {
                return slot;
            }
        }
        throw new IllegalArgumentException("No slot for " + methodName + " and signature " + signature);
    }
}