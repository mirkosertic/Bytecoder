/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.allocator;

import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class LinearRegisterAllocator extends AbstractAllocator {

    public LinearRegisterAllocator(final Program aProgram, final Function<Variable, TypeRef> aTypeConverter, final
            BytecodeLinkerContext aLinkerContext) {
        super(aTypeConverter, aLinkerContext);

        final List<Variable> theVariables = computeSSAReadyVariablesFor(aProgram);

        final Map<Long, List<Variable>> theDefinitionPointsToDefition = new HashMap<>();
        final Set<Long> foundDefinitionPoints = new HashSet<>();

        // Step one : We sort the variables into buckets
        // and try to establish a timeline for the linear scan
        for (final Variable v : theVariables) {
            if (!v.isSynthetic()) {
                final long theDefinition = v.liveRange().getDefinedAt();
                foundDefinitionPoints.add(theDefinition);

                final List<Variable> theDefList = theDefinitionPointsToDefition.computeIfAbsent(theDefinition, k -> new ArrayList<>());
                theDefList.add(v);
            }
        }

        final List<Long> theDefinitionPoints = new ArrayList<>(foundDefinitionPoints);
        Collections.sort(theDefinitionPoints);

        long registerCount=0;

        // Now we do a simple linear scan
        final List<Variable> currentlyActive = new ArrayList<>();
        for (final long theDefinition : theDefinitionPoints) {
            // We remove variables that are no longer active at this point in time
            final List<Variable> theNoLongerActive = new ArrayList<>();
            for (final Variable v : currentlyActive) {
                if (v.liveRange().getLastUsedAt() < theDefinition) {
                    theNoLongerActive.add(v);
                }
            }
            currentlyActive.removeAll(theNoLongerActive);

            // Now we add the variables that are active from now and assign
            // Registers to them
            final List<Variable> activeFromHere = theDefinitionPointsToDefition.get(theDefinition);
            for (final Variable v : activeFromHere) {
                final TypeRef theType = aTypeConverter.apply(v);
                final List<Register> theKnownRegistersOfThisType = knownRegisters.get(theType);
                if (theKnownRegistersOfThisType != null) {
                    // We already know registers of this type
                    // So we can try to find a free one
                    final List<Register> theAvailableRegisters = new ArrayList<>(theKnownRegistersOfThisType);

                    for (final Variable a : currentlyActive) {
                        final TypeRef theOtherType = aTypeConverter.apply(a);
                        if (theOtherType == theType) {
                            theAvailableRegisters.remove(registerAssignments.get(a));
                        }
                    }

                    if (theAvailableRegisters.isEmpty()) {
                        // There are no free registers left
                        // So we introduce a new one
                        final Register theNewRegister = new Register(registerCount++, theType);
                        knownRegisters.get(theType).add(theNewRegister);
                        registerAssignments.put(v, theNewRegister);
                    } else {
                        // We take the first free register
                        registerAssignments.put(v, theAvailableRegisters.iterator().next());
                    }

                } else {

                    // First usage of this type
                    // We introduce a new register of this type
                    final Register theNewRegister = new Register(registerCount++, theType);
                    final List<Register> theRegisterList = new ArrayList<>();
                    theRegisterList.add(theNewRegister);
                    knownRegisters.put(theType, theRegisterList);
                    registerAssignments.put(v, theNewRegister);
                }

                currentlyActive.add(v);
            }
        }
    }
}
