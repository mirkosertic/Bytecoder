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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class ClassHierarchyAnalysis {

    private final BytecodeLinkerContext linkerContext;

    public ClassHierarchyAnalysis(final BytecodeLinkerContext linkerContext) {
        this.linkerContext = linkerContext;
    }

    public Optional<BytecodeLinkedClass> classProvidingInvokableMethod(final String aMethodName,
                                                                       final BytecodeMethodSignature aSignature,
                                                                       final BytecodeTypeRef aInvocationTarget,
                                                                       final Value aReceiver,
                                                                       final Predicate<BytecodeLinkedClass> aClassFilter,
                                                                       final Predicate<BytecodeMethod> aMethodFilter) {
        final BytecodeLinkedClass theInvocationTarget;
        if (aInvocationTarget.isArray()) {
            theInvocationTarget = linkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class));
        } else {
            theInvocationTarget = linkerContext.resolveClass((BytecodeObjectTypeRef) aInvocationTarget);
        }

        if (aClassFilter.test(theInvocationTarget)) {
            // We don't have to check the type hierarchy for final classes
            // Finding the implementation type can be done faster in this case
            if (theInvocationTarget.getBytecodeClass().getAccessFlags().isFinal()) {
                BytecodeLinkedClass theCurrent = theInvocationTarget;
                while (theCurrent != null) {
                    final BytecodeMethod m = theCurrent.getBytecodeClass().methodByNameAndSignatureOrNull(aMethodName, aSignature);
                    if (m != null && aMethodFilter.test(m)) {
                        return Optional.of(theCurrent);
                    }
                    theCurrent = theCurrent.getSuperClass();
                }
                return Optional.empty();
            }

            // We have to check the whole type hierarchy
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
                        test:
                        while (theCurrent != null) {
                            final BytecodeMethod m = theCurrent.getBytecodeClass().methodByNameAndSignatureOrNull(aMethodName, aSignature);
                            if (m != null && aMethodFilter.test(m)) {
                                theResult.add(theCurrent);
                                break test;
                            }
                            theCurrent = theCurrent.getSuperClass();
                        }
                    });

            if (theResult.size() == 1) {
                return Optional.of(theResult.iterator().next());
            } else if (theResult.size() > 1) {
                // We might check the type of the receiver here,
                // but i haven't found a stable solution for this problem
                // This happens when calling abstract methods in base types and there
                // are multiple implementation types available.
                // Bytecoder Dataflow Analysis cannot do this
            }
        }
        return Optional.empty();
    }
}
