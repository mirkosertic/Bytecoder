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
import de.mirkosertic.bytecoder.core.BytecodeLocalVariableTableEntry;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class ParsingHelper {

    @FunctionalInterface
    interface ValueProvider {
        Value resolveValueFor(VariableDescription aDescription);
    }

    private final RegionNode block;
    private final Stack<Value> stack;
    private final Map<Integer, Variable> localVariables;
    private final ValueProvider valueProvider;
    private final BytecodeLocalVariableTableAttributeInfo localVariableTableAttributeInfo;

    public ParsingHelper(
            final BytecodeLocalVariableTableAttributeInfo aDebugInfo, final RegionNode aBlock, final ValueProvider aValueProvider) {
        stack = new Stack<>();
        block = aBlock;
        localVariables = new HashMap<>();
        valueProvider = aValueProvider;
        localVariableTableAttributeInfo = aDebugInfo;
    }

    public int numberOfLocalVariables() {
        return localVariables.size();
    }

    public Stack<Value> getStack() {
        return stack;
    }

    public Value pop() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Stack is empty!!!");
        }
        return stack.pop();
    }

    public Value peek() {
        return stack.peek();
    }

    public void push(final Value aValue) {
        if (aValue == null) {
            throw new IllegalStateException("Trying to push null in " + this);
        }
        stack.push(aValue);
    }

    public Value getLocalVariable(final int aIndex, final TypeRef aExpectedType) {
        Variable theValue = localVariables.get(aIndex);
        if (theValue == null) {
            final VariableDescription theDesc = new LocalVariableDescription(aIndex, aExpectedType);
            theValue = (Variable) valueProvider.resolveValueFor(theDesc);
            if (theValue == null) {
                throw new IllegalStateException("Value must not be null from provider for " + theDesc);
            }
            block.addToImportedList(theValue, theDesc);
            block.addToExportedList(theValue, theDesc);
            localVariables.put(aIndex, theValue);
        }
        return theValue;
    }

    public Value requestValue(final VariableDescription aDescription) {
        if (aDescription instanceof LocalVariableDescription) {
            final LocalVariableDescription theDesc = (LocalVariableDescription) aDescription;
            return getLocalVariable(theDesc.getIndex(), ((LocalVariableDescription) aDescription).getTypeRef());
        }
        final StackVariableDescription theStack = (StackVariableDescription) aDescription;
        if (theStack.getPos() < stack.size()) {
            return stack.get(stack.size() - theStack.getPos() - 1);
        }
        throw new IllegalStateException("Invalid stack index : " + theStack.getPos() + " with total size of " + stack.size());
    }

    public void setLocalVariable(final BytecodeOpcodeAddress aInstruction, final int aIndex, final TypeRef aType, final Value aValue) {
        if (aValue == null) {
            throw new IllegalStateException("local variable " + aIndex + " must not be null in " + this);
        }
        if (localVariableTableAttributeInfo != null) {
            final BytecodeLocalVariableTableEntry theEntry = localVariableTableAttributeInfo.matchingEntryFor(aInstruction, aIndex);
            if (theEntry != null) {
                    /*String theVariableName = localVariableTableAttributeInfo.resolveVariableName(theEntry);
                    Variable theGlobal = program.getOrCreateTrulyGlobal(theVariableName, aValue.resolveType());
                    theGlobal.initializeWith(aValue);
                    block.addExpression(new VariableAssignmentExpression(theGlobal, aValue));
                    localVariables.put(aIndex, theGlobal);
                    block.addToExportedList(theGlobal, new LocalVariableDescription(aIndex));
                    return;*/
            }
        }

        final Variable v = block.setLocalVariable(aInstruction, aIndex, aType, aValue);
        localVariables.put(aIndex, v);
        block.addToExportedList(v, new LocalVariableDescription(aIndex, aType));
    }

    public void setStackValue(final int aStackPos, final Value aValue) {
        final List<Value> theValues = new ArrayList<>(stack);
        while (theValues.size() <= aStackPos) {
            theValues.add(null);
        }
        theValues.set(aStackPos, aValue);
        stack.clear();
        stack.addAll(theValues);
    }

    public void finalizeExportState() {
        for (final Map.Entry<Integer, Variable> theEntry : localVariables.entrySet()) {
            block.addToExportedList(theEntry.getValue(), new LocalVariableDescription(theEntry.getKey(), theEntry.getValue().resolveType()));
        }
        for (int i=stack.size() - 1 ; i>= 0; i--) {
            // Numbering must be consistent here!!
            block.addToExportedList(stack.get(i), new StackVariableDescription(stack.size() - 1 - i));
        }
    }
}