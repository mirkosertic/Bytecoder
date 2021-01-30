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
package de.mirkosertic.bytecoder.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BytecodeClassTopologicOrder {

    private static final BytecodeObjectTypeRef OBJECT = BytecodeObjectTypeRef.fromRuntimeClass(Object.class);

    private final List<BytecodeLinkedClass> knownClasses;

    public BytecodeClassTopologicOrder(final BytecodeLinkerContext linkerContext) {
        knownClasses = new ArrayList<>();
        linkerContext.linkedClasses().forEach(t -> knownClasses.add(t.targetNode()));
        knownClasses.sort(Comparator.comparingInt(this::topo).thenComparing(o -> o.getClassName().name()));
    }

    private int topo(final BytecodeLinkedClass aClass) {
        if (aClass.getClassName().equals(OBJECT)) {
            return 1;
        }
        int theTopo = topo(aClass.getSuperClass());
        for (final BytecodeLinkedClass theImplementingType : aClass.getImplementingTypes(false, false)) {
            theTopo = Math.max(theTopo, topo(theImplementingType));
        }
        return theTopo + 1;
    }

    public List<BytecodeLinkedClass> getClassesInOrder() {
        return knownClasses;
    }
}
