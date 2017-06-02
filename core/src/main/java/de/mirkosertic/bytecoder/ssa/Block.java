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

import java.util.ArrayList;
import java.util.List;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

public class Block {

    public enum Type {
        NORMAL,
        EXCEPTION_HANDLER,
        FINALLY
    }

    private final BytecodeOpcodeAddress startAddress;
    private final ExpressionList expressions;
    private final List<Variable> importedStack;
    private final List<Variable> exitStack;
    private final Program program;
    private final List<Block> successors;
    private final Type type;

    public Block(Type aType, Program aProgram, BytecodeOpcodeAddress aStartAddress) {
        type = aType;
        startAddress = aStartAddress;
        program = aProgram;
        exitStack = new ArrayList<>();
        expressions = new ExpressionList();
        importedStack = new ArrayList<>();
        successors = new ArrayList<>();
    }

    public Type getType() {
        return type;
    }

    public List<Block> getPredecessors() {
        List<Block> theResult = new ArrayList<>();
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

    public List<Block> getSuccessors() {
        return successors;
    }

    public BytecodeOpcodeAddress getStartAddress() {
        return startAddress;
    }

    public Variable newImportedStackVariable() {
        Variable theNewVariable = program.createVariable(new PHIFunction());
        importedStack.add(theNewVariable);
        expressions.add(new InitVariableExpression(theNewVariable));
        return theNewVariable;
    }

    public Variable newImportedLocalVariable(int aIndex) {
        Variable theNewVariable = program.createVariable(new ExternalReferenceValue(aIndex));
        expressions.add(new InitVariableExpression(theNewVariable));
        return theNewVariable;
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

    public List<Variable> getImportedStack() {
        return importedStack;
    }

    public void addToExitStack(Variable aVariable) {
        exitStack.add(aVariable);
    }

    public List<Variable> getRemainingStack() {
        return exitStack;
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
