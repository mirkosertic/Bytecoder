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
package de.mirkosertic.bytecoder.ssa;

import de.mirkosertic.bytecoder.classlib.Array;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.graph.Edge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class ClassHierarchyAnalysis {

    private final BytecodeLinkerContext linkerContext;

    public ClassHierarchyAnalysis(final BytecodeLinkerContext linkerContext) {
        this.linkerContext = linkerContext;
    }

    public List<BytecodeLinkedClass> classesProvidingInvocableMethod(final String aMethodName,
                                                                     final BytecodeMethodSignature aSignature,
                                                                     final BytecodeTypeRef aInvocationTarget,
                                                                     final Predicate<BytecodeLinkedClass> aClassFilter,
                                                                     final Predicate<BytecodeMethod> aMethodFilter) {
        final BytecodeLinkedClass theInvocationTarget;
        if (aInvocationTarget.isArray()) {
            theInvocationTarget = linkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class));
        } else {
            theInvocationTarget = linkerContext.resolveClass((BytecodeObjectTypeRef) aInvocationTarget);
        }

        final Set<BytecodeLinkedClass> theResult = new HashSet<>();
        linkerContext.linkedClasses()
                .map(Edge::targetNode)
                .filter(aClassFilter)
                .filter(t -> {
                    final Set<BytecodeLinkedClass> theImplementingTypes = t.getImplementingTypes();
                    return theImplementingTypes.contains(theInvocationTarget);
                })
                .forEach(clz -> {
                    BytecodeLinkedClass theCurrent = clz;
                    test: while(theCurrent != null) {
                        final BytecodeMethod m = theCurrent.getBytecodeClass().methodByNameAndSignatureOrNull(aMethodName, aSignature);
                        if (m != null && aMethodFilter.test(m)) {
                            theResult.add(theCurrent);
                            break test;
                        }
                        theCurrent = theCurrent.getSuperClass();
                    }
                });

        return new ArrayList<>(theResult);
    }
}
