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
package de.mirkosertic.bytecoder.ssa;

import de.mirkosertic.bytecoder.core.BytecodeLocalVariableTableAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class ParsingHelperCache {

    private final BytecodeMethod method;
    private final RegionNode startNode;
    private final Map<RegionNode, ParsingHelper> finalStatesForNodes;
    private final Program program;
    private final BytecodeLocalVariableTableAttributeInfo localVariableTableAttributeInfo;

    public ParsingHelperCache(
            final Program aProgram, final BytecodeMethod aMethod, final RegionNode aStartNode, final BytecodeLocalVariableTableAttributeInfo aLocalVariablesInfo) {
        startNode = aStartNode;
        method = aMethod;
        localVariableTableAttributeInfo = aLocalVariablesInfo;
        finalStatesForNodes = new HashMap<>();
        program = aProgram;
    }

    public void registerFinalStateForNode(final RegionNode aNode, final ParsingHelper aState) {
        finalStatesForNodes.put(aNode, aState);
    }

    public ParsingHelper resolveFinalStateForNode(final RegionNode aGraphNode) {
        if (aGraphNode == null) {
            // No node, so we create the initial state of the whole program
            final Map<VariableDescription, Value> theValues = new HashMap<>();

            // At this point, local variables are initialized based on the method signature
            // The stack is empty
            int theCurrentIndex = 0;
            if (!method.getAccessFlags().isStatic()) {
                final LocalVariableDescription theDesc = new LocalVariableDescription(theCurrentIndex, TypeRef.Native.REFERENCE);
                theValues.put(theDesc, program.matchingArgumentOf(theDesc).getVariable());
                theCurrentIndex++;
            }

            final BytecodeTypeRef[] theTypes = method.getSignature().getArguments();
            for (final BytecodeTypeRef theRef : theTypes) {
                final LocalVariableDescription theDesc = new LocalVariableDescription(theCurrentIndex, TypeRef.toType(theRef));
                theValues.put(theDesc, program.matchingArgumentOf(theDesc).getVariable());
                theCurrentIndex++;
                if (theRef == BytecodePrimitiveTypeRef.LONG || theRef == BytecodePrimitiveTypeRef.DOUBLE) {
                    theCurrentIndex++;
                }
            }

            final ParsingHelper.ValueProvider theProvider = (aDescription) -> {
                final Value theValue = theValues.get(aDescription);
                if (theValue == null) {
                    throw new IllegalStateException("No value on cfg enter : " + aDescription);
                }
                return theValue;
            };

            return new ParsingHelper(localVariableTableAttributeInfo, startNode, theProvider);
        }
        return finalStatesForNodes.get(aGraphNode);
    }

    private TypeRef widestTypeOf(final Collection<Value> aValue) {
        if (aValue.size() == 1) {
            return aValue.iterator().next().resolveType();
        }
        TypeRef.Native theCurrent = null;
        for (final Value theValue : aValue) {
            final TypeRef.Native theValueType = theValue.resolveType().resolve();
            if (theCurrent == null) {
                theCurrent = theValueType;
            } else {
                theCurrent = theCurrent.eventuallyPromoteTo(theValueType);
            }
        }
        return theCurrent;
    }

    public ParsingHelper resolveInitialPHIStateForNode(final RegionNode aBlock) {

        if (aBlock.getType() != RegionNode.BlockType.NORMAL) {
            // Exception handler and finally blocks do not import a stack
            final ParsingHelper.ValueProvider theProvider = (aDescription) -> {
                if (aDescription instanceof StackVariableDescription) {
                    throw new IllegalStateException("Stack imports not allowed for EXCEPTION HANDLER or FINALLY blocks");
                }
                final LocalVariableDescription theLocal = (LocalVariableDescription) aDescription;
                final Variable theVariable = aBlock.findLocalVariable(theLocal.getIndex(), theLocal.getTypeRef());
                aBlock.addToImportedList(theVariable, theLocal);
                return theVariable;
            };

            return new ParsingHelper(localVariableTableAttributeInfo, aBlock, theProvider);
        }
        final ParsingHelper.ValueProvider theProvider;

        // We collect the stacks from all predecessor nodes
        final Map<StackVariableDescription, Set<Value>> theStackToImport = new HashMap<>();
        int theRequestedStack = -1;
        for (final RegionNode thePredecessor : aBlock.getPredecessorsIgnoringBackEdges()) {
            final ParsingHelper theHelper = finalStatesForNodes.get(thePredecessor);
            if (!theHelper.getStack().isEmpty()) {
                if (theRequestedStack == -1) {
                    theRequestedStack = theHelper.getStack().size();
                } else {
                    if (theRequestedStack != theHelper.getStack().size()) {
                        throw new IllegalStateException(
                                "Wrong number of exported stack in " + thePredecessor.getStartAddress().getAddress()
                                        + " expected " + theRequestedStack + " got " + theHelper.getStack().size()
                                        + " to jump to " + aBlock.getStartAddress().getAddress());
                    }
                }
                for (int i = 0; i < theHelper.getStack().size(); i++) {
                    final StackVariableDescription theStackPos = new StackVariableDescription(
                            theHelper.getStack().size() - i - 1);
                    final Value theStackValue = theHelper.getStack().get(i);

                    final Set<Value> theKnownValues = theStackToImport.computeIfAbsent(theStackPos, k -> new HashSet<>());
                    theKnownValues.add(theStackValue);
                }
            }
        }
        theProvider = (aDescription) -> newPHIFor(aBlock.getPredecessorsIgnoringBackEdges(), aDescription, aBlock);
        final ParsingHelper theHelper = new ParsingHelper(localVariableTableAttributeInfo, aBlock, theProvider);

        // Now we import the stack and check if we need to insert phi values
        for (final Map.Entry<StackVariableDescription, Set<Value>> theEntry : theStackToImport.entrySet()) {
            final Set<Value> theValues = theEntry.getValue();
            if (theValues.size() == 1) {
                // Only one value, we do not need to insert a phi value
                final Value theSingleValue = theValues.iterator().next();
                theHelper.setStackValue(theRequestedStack - theEntry.getKey().getPos() - 1, theSingleValue);
                aBlock.addToImportedList(theSingleValue, theEntry.getKey());
            } else {
                // We have a PHI value here
                final TypeRef theType = widestTypeOf(theValues);
                final Variable thePHI = aBlock.newImportedVariable(theType, theEntry.getKey());
                for (final Value theValue : theValues) {
                    thePHI.initializeWith(theValue);
                }
                theHelper.setStackValue(theRequestedStack - theEntry.getKey().getPos() - 1, thePHI);
            }
        }
        return theHelper;
    }

    private Value newPHIFor(final Set<RegionNode> aNodes, final VariableDescription aDescription, final RegionNode aImportingBlock) {
        final Set<Value> theValues = new HashSet<>();
        for (final RegionNode thePredecessor : aNodes) {
            final ParsingHelper theHelper = finalStatesForNodes.get(thePredecessor);
            if (theHelper == null) {
                throw new IllegalStateException("No helper for " + thePredecessor.getStartAddress().getAddress());
            }
            theValues.add(theHelper.requestValue(aDescription));
        }
        if (theValues.isEmpty()) {
            throw new IllegalStateException(
                    "No values for " + aDescription + " in block " + aImportingBlock.getStartAddress().getAddress());
        }
        if (theValues.size() == 1) {
            final Value theValue = theValues.iterator().next();
            aImportingBlock.addToImportedList(theValue, aDescription);
            return theValue;
        }
        final TypeRef theType = widestTypeOf(theValues);
        final Variable thePHI = aImportingBlock.newImportedVariable(theType, aDescription);
        for (final Value theValue : theValues) {
            thePHI.initializeWith(theValue);
        }
        return thePHI;
    }

    public ParsingHelper resolveInitialStateFromPredecessorFor(final RegionNode aNode, final ParsingHelper aPredecessor) {
        // The node will import the full stack from its predecessor
        final ParsingHelper.ValueProvider theProvider = aPredecessor::requestValue;
        final ParsingHelper theNew = new ParsingHelper(localVariableTableAttributeInfo, aNode, theProvider);
        final Stack<Value> theStackToImport = aPredecessor.getStack();
        for (int i=0;i<theStackToImport.size();i++) {
            final StackVariableDescription theStackDesc = new StackVariableDescription(theStackToImport.size() - i - 1);
            final Value theImportedValue = theStackToImport.get(i);
            theNew.getStack().push(theImportedValue);
            aNode.addToImportedList(theImportedValue, theStackDesc);
        }
        return theNew;
    }
}