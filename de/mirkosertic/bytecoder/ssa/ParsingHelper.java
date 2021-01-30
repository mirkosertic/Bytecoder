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

    private final Program program;
    private final RegionNode block;
    private final Stack<Value> stack;
    private final Map<Integer, Value> localVariables;
    private final ValueProvider valueProvider;
    private final BytecodeLocalVariableTableAttributeInfo localVariableTableAttributeInfo;

    public ParsingHelper(
            final Program aProgram, final BytecodeLocalVariableTableAttributeInfo aDebugInfo, final RegionNode aBlock, final ValueProvider aValueProvider) {
        program = aProgram;
        stack = new Stack<>();
        block = aBlock;
        localVariables = new HashMap<>();
        valueProvider = aValueProvider;
        localVariableTableAttributeInfo = aDebugInfo;
    }

    public RegionNode getBlock() {
        return block;
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
        final Value theValue = stack.pop();
        if (theValue instanceof Variable) {
            ((Variable) theValue).liveRange().usedAt(program.getAnalysisTime());
        }
        return theValue;
    }

    public Value peek() {
        final Value theValue = stack.peek();
        if (theValue instanceof Variable) {
            ((Variable) theValue).liveRange().usedAt(program.getAnalysisTime());
        }
        return theValue;
    }

    public void push(final BytecodeOpcodeAddress aAddress, final Value aValue) {
        if (aValue == null) {
            throw new IllegalStateException("Trying to push null in " + this);
        }
        if (aValue instanceof Variable) {
            final Variable v = (Variable) aValue;
            v.liveRange().usedAt(program.getAnalysisTime());
            stack.push(v);
        } else {
            // store value as variable and push  it onto the stack
            final Variable v = program.createVariable(aValue.resolveType());
            v.initializeWith(aValue, program.getAnalysisTime());
            block.getExpressions().add(new VariableAssignmentExpression(program, aAddress, v, aValue));
            stack.push(v);
        }
    }

    public Value getLocalVariable(final int aIndex, final TypeRef aExpectedType) {
        Value theValue = localVariables.get(aIndex);
        if (theValue == null) {
            final VariableDescription theDesc = new LocalVariableDescription(aIndex, aExpectedType);
            theValue = valueProvider.resolveValueFor(theDesc);
            if (theValue == null) {
                throw new IllegalStateException("Value must not be null from provider for " + theDesc);
            }
            block.addToLiveIn(theValue, theDesc);
            localVariables.put(aIndex, theValue);
        }
        if (theValue instanceof Variable) {
            ((Variable) theValue).liveRange().usedAt(program.getAnalysisTime());
        }
        return theValue;
    }

    public Value requestValue(final VariableDescription aDescription) {
        if (aDescription instanceof LocalVariableDescription) {
            final LocalVariableDescription theDesc = (LocalVariableDescription) aDescription;
            final Value theValue = getLocalVariable(theDesc.getIndex(), ((LocalVariableDescription) aDescription).getTypeRef());
            block.addToLiveOut(theValue, theDesc);
            return theValue;
        }
        final StackVariableDescription theStack = (StackVariableDescription) aDescription;
        if (theStack.getPos() < stack.size()) {
            final Value theValue = stack.get(stack.size() - theStack.getPos() - 1);
            block.addToLiveOut(theValue, theStack);
            return theValue;
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
        if (aValue instanceof Variable) {
            localVariables.put(aIndex, aValue);
            return;
        }
        final Variable v = program.createVariable(aValue.resolveType());
        v.liveRange().usedAt(program.getAnalysisTime());
        block.getExpressions().add(new VariableAssignmentExpression(program, aInstruction, v, aValue));
        v.initializeWith(aValue, program.getAnalysisTime());
        localVariables.put(aIndex, v);
    }

    public void setStackValue(final int aStackPos, final Value aValue) {
        if (aValue instanceof Variable) {
            ((Variable) aValue).liveRange().usedAt(program.getAnalysisTime());
        }

        final List<Value> theValues = new ArrayList<>(stack);
        while (theValues.size() <= aStackPos) {
            theValues.add(null);
        }
        theValues.set(aStackPos, aValue);
        stack.clear();
        stack.addAll(theValues);
    }
}