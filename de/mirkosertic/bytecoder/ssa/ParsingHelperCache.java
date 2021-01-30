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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLocalVariableTableAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

public class ParsingHelperCache {

    private final BytecodeObjectTypeRef linkedClass;
    private final BytecodeMethod method;
    private final RegionNode startNode;
    private final Map<RegionNode, ParsingHelper> finalStatesForNodes;
    private final Program program;
    private final BytecodeLocalVariableTableAttributeInfo localVariableTableAttributeInfo;
    private final BytecodeLinkerContext linkerContext;

    public ParsingHelperCache(
            final Program aProgram, final BytecodeObjectTypeRef aLinkedClass, final BytecodeMethod aMethod, final BytecodeLocalVariableTableAttributeInfo aLocalVariablesInfo, final BytecodeLinkerContext aLinkerContext) {
        linkedClass = aLinkedClass;
        program = aProgram;
        startNode = aProgram.getControlFlowGraph().startNode();
        method = aMethod;
        localVariableTableAttributeInfo = aLocalVariablesInfo;
        finalStatesForNodes = new HashMap<>();
        linkerContext = aLinkerContext;
    }

    public void registerFinalStateForNode(final RegionNode aNode, final ParsingHelper aState) {
        finalStatesForNodes.put(aNode, aState);
    }

    public ParsingHelper resolveInitialProgramFlowState() {
        // No node, so we create the initial state of the whole program
        final Map<VariableDescription, Value> theValues = new HashMap<>();

        // At this point, local variables are initialized based on the method signature
        // The stack is empty
        int theCurrentIndex = 0;
        int theLocalVariableIndex = 0;
        if (!method.getAccessFlags().isStatic()) {
            final TypeRef theThisType = TypeRef.toType(linkedClass);
            final LocalVariableDescription theDesc = new LocalVariableDescription(theLocalVariableIndex, theThisType);

            final Variable theThisRef = program.argumentAt(theCurrentIndex);
            final Variable theShadow = program.createVariable(theThisType);
            theShadow.initializeWith(theThisRef, 0);
            startNode.getExpressions().add(new VariableAssignmentExpression(program, BytecodeOpcodeAddress.START_AT_ZERO, theShadow, theThisRef));

            theValues.put(theDesc, theShadow);
            theCurrentIndex++;
            theLocalVariableIndex++;
        }

        final BytecodeTypeRef[] theTypes = method.getSignature().getArguments();
        for (final BytecodeTypeRef theRef : theTypes) {
            final LocalVariableDescription theDesc = new LocalVariableDescription(theLocalVariableIndex, TypeRef.toType(theRef));

            final Variable theArgument = program.argumentAt(theCurrentIndex);
            final Variable theShadow = program.createVariable(theArgument.resolveType());
            theShadow.initializeWith(theArgument, 0);
            startNode.getExpressions().add(new VariableAssignmentExpression(program, BytecodeOpcodeAddress.START_AT_ZERO, theShadow, theArgument));

            theValues.put(theDesc, theShadow);
            theCurrentIndex++;
            theLocalVariableIndex++;
            if (theRef == BytecodePrimitiveTypeRef.LONG || theRef == BytecodePrimitiveTypeRef.DOUBLE) {
                theLocalVariableIndex++;
            }
        }

        final ParsingHelper.ValueProvider theProvider = (aDescription) -> {
            final Value theValue = theValues.get(aDescription);
            if (theValue == null) {
                throw new IllegalStateException("No value on cfg enter : " + aDescription);
            }
            return theValue;
        };

        return new ParsingHelper(program, localVariableTableAttributeInfo, startNode, theProvider);
    }

    public ParsingHelper resolveFinalStateForNode(final RegionNode aGraphNode) {
        return finalStatesForNodes.get(aGraphNode);
    }

    public ParsingHelper resolveInitialPHIStateForNode(final RegionNode aBlock) {

        if (aBlock.getType() != RegionNode.BlockType.NORMAL) {
            // Exception handler and finally blocks do not import a stack
            final ParsingHelper.ValueProvider theProvider = (aDescription) -> {
                if (aDescription instanceof StackVariableDescription) {
                    throw new IllegalStateException("Stack imports not allowed for EXCEPTION HANDLER or FINALLY blocks");
                }
                final LocalVariableDescription theLocal = (LocalVariableDescription) aDescription;
                final Set<RegionNode> thePredecessors = aBlock.getPredecessorsIgnoringBackEdges();
                if (thePredecessors.size() == 1 && !aBlock.hasIncomingBackEdges()) {
                    final RegionNode theSinglePred = thePredecessors.iterator().next();
                    final ParsingHelper theHelper = finalStatesForNodes.get(theSinglePred);
                    final Value theValue = theHelper.requestValue(theLocal);
                    aBlock.addToLiveIn(theValue, theLocal);
                    return theValue;
                }
                return newPHIFor(thePredecessors, theLocal, aBlock);
            };

            return new ParsingHelper(program, localVariableTableAttributeInfo, aBlock, theProvider);
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
        final ParsingHelper theHelper = new ParsingHelper(program, localVariableTableAttributeInfo, aBlock, theProvider);

        // Now we import the stack and check if we need to insert phi values
        for (final Map.Entry<StackVariableDescription, Set<Value>> theEntry : theStackToImport.entrySet()) {
            final Set<Value> theValues = theEntry.getValue();
            if (theValues.size() == 1 && !aBlock.hasIncomingBackEdges()) {
                // Only one value, we do not need to insert a phi value
                final Value theSingleValue = theValues.iterator().next();
                theHelper.setStackValue(theRequestedStack - theEntry.getKey().getPos() - 1, theSingleValue);
                aBlock.addToLiveIn(theSingleValue, theEntry.getKey());
            } else {
                // We have a PHI value here
                final TypeRef theType = Value.widestTypeOf(theValues, linkerContext);

                final PHIValue thePHI = new PHIValue(theEntry.getKey(), theType);
                for (final Value v : theValues) {
                    thePHI.receivesDataFrom(v);
                }

                theHelper.setStackValue(theRequestedStack - theEntry.getKey().getPos() - 1, thePHI);
                aBlock.addToLiveIn(thePHI, theEntry.getKey());
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
        if (theValues.size() == 1 && !aImportingBlock.hasIncomingBackEdges()) {
            final Value theValue = theValues.iterator().next();
            aImportingBlock.addToLiveIn(theValue, aDescription);
            return theValue;
        }
        final TypeRef theType = Value.widestTypeOf(theValues, linkerContext);
        final PHIValue thePHI = new PHIValue(aDescription, theType);
        for (final Value v : theValues) {
            thePHI.receivesDataFrom(v);
        }

        aImportingBlock.addToLiveIn(thePHI, aDescription);

        return thePHI;
    }

    public ParsingHelper resolveInitialStateFromPredecessorFor(final RegionNode aNode, final ParsingHelper aPredecessor) {
        // The node will import the full stack from its predecessor
        final ParsingHelper.ValueProvider theProvider = aPredecessor::requestValue;
        final ParsingHelper theNew = new ParsingHelper(program, localVariableTableAttributeInfo, aNode, theProvider);
        final Stack<Value> theStackToImport = aPredecessor.getStack();
        for (int i=0;i<theStackToImport.size();i++) {
            final StackVariableDescription theStackDesc = new StackVariableDescription(theStackToImport.size() - i - 1);
            final Value theImportedValue = theStackToImport.get(i);
            theNew.getStack().push(theImportedValue);
            aNode.addToLiveIn(theImportedValue, theStackDesc);

            aPredecessor.getBlock().addToLiveOut(theImportedValue, theStackDesc);
        }
        return theNew;
    }
}