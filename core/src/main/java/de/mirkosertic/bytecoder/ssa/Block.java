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
package de.mirkosertic.bytecoder.ssa;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Block {

    public enum Type {
        NORMAL,
        EXCEPTION_HANDLER,
        FINALLY
    }

    private final BytecodeOpcodeAddress startAddress;
    private final ExpressionList expressions;
    private final Program program;
    private final Set<Block> successors;
    private final Type type;
    private BlockFinalState finalState;

    public Block(Type aType, Program aProgram, BytecodeOpcodeAddress aStartAddress) {
        type = aType;
        startAddress = aStartAddress;
        program = aProgram;
        expressions = new ExpressionList();
        successors = new HashSet<>();
    }

    public boolean isStartBlock() {
        return program.getBlocks().indexOf(this) == 0;
    }

    public BlockFinalState getFinalState() {
        return finalState;
    }

    public void setFinalState(BlockFinalState aFinalState) {
        finalState = aFinalState;
        for (Map.Entry<Integer, Variable> theEntry : aFinalState.getLocalVariables().entrySet()) {
            System.out.println(theEntry.getKey() +  " -> " + theEntry.getValue());
        }
    }

    public Type getType() {
        return type;
    }

    public Set<Block> getPredecessors() {
        Set<Block> theResult = new HashSet<>();
        for (Block theBlock: program.getBlocks()) {
            if (theBlock.getSuccessors().contains(this)) {
                theResult.add(theBlock);
            }
        }
        return theResult;
    }

    public void addSuccessor(Block aBlock) {
        successors.add(aBlock);
    }

    public Set<Block> getSuccessors() {
        return successors;
    }

    public BytecodeOpcodeAddress getStartAddress() {
        return startAddress;
    }

    public Variable newVariable(Value aValue)  {
        Variable theNewVariable = program.createVariable(aValue);
        expressions.add(new InitVariableExpression(theNewVariable));
        return theNewVariable;
    }

    public void addExpression(Expression aExpression) {
        expressions.add(aExpression);
    }

    public ExpressionList getExpressions() {
        return expressions;
    }

    public boolean endWithNeverReturningExpression() {
        Expression theLastExpression = expressions.lastExpression();
        return theLastExpression instanceof ReturnExpression ||
                theLastExpression instanceof ReturnVariableExpression ||
                theLastExpression instanceof TableSwitchExpression ||
                theLastExpression instanceof LookupSwitchExpression ||
                theLastExpression instanceof ThrowExpression ||
                theLastExpression instanceof GotoExpression;
    }
}
