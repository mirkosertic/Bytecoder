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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

public class GraphNode {

    public enum BlockType {
        NORMAL,
        INFINITELOOP,
        EXCEPTION_HANDLER,
        FINALLY
    }

    private final BytecodeOpcodeAddress startAddress;
    private final ExpressionList expressions;
    private final Program program;
    private final Set<GraphNode> successors;
    private BlockType type;
    private final Map<VariableDescription, Value> imported;
    private final Map<VariableDescription, Value> exported;

    public GraphNode(BlockType aType, Program aProgram, BytecodeOpcodeAddress aStartAddress) {
        type = aType;
        startAddress = aStartAddress;
        program = aProgram;
        expressions = new ExpressionList();
        successors = new HashSet<>();
        imported = new HashMap<>();
        exported = new HashMap<>();
    }

    public void markAsInfiniteLoop() {
        type = BlockType.INFINITELOOP;
    }

    public BlockType getType() {
        return type;
    }

    public Set<GraphNode> getPredecessors() {
        Set<GraphNode> theResult = new HashSet<>();
        for (GraphNode theBlock: program.getControlFlowGraph().getKnownNodes()) {
            if (theBlock.getSuccessors().contains(this)) {
                theResult.add(theBlock);
            }
        }
        return theResult;
    }

    public void addSuccessor(GraphNode aBlock) {
        successors.add(aBlock);
    }

    public Set<GraphNode> getSuccessors() {
        return successors;
    }

    public BytecodeOpcodeAddress getStartAddress() {
        return startAddress;
    }

    public Variable newVariable(TypeRef aType, Value aValue)  {
        return newVariable(aType, aValue, false);
    }

    public Variable newVariable(TypeRef aType, Value aValue, boolean aIsImport)  {
        Variable theNewVariable = program.createVariable(aType, aValue);
        if (!aIsImport) {
            expressions.add(new InitVariableExpression(theNewVariable));
        }
        return theNewVariable;
    }

    public Variable newImportedVariable(TypeRef aType, Value aValue, VariableDescription aDescription) {
        Variable theVariable = newVariable(aType, aValue, true);
        imported.put(aDescription, theVariable);
        return theVariable;
    }

    public void addToExportedList(Value aValue, VariableDescription aDescription) {
        exported.put(aDescription, aValue);
    }

    public void addToImportedList(Value aValue, VariableDescription aDescription) {
        imported.put(aDescription, aValue);
    }

    public void addExpression(Expression aExpression) {
        expressions.add(aExpression);
    }

    public ExpressionList getExpressions() {
        return expressions;
    }

    public BlockState toFinalState() {
        BlockState theState = new BlockState();
        for (Map.Entry<VariableDescription, Value> theEntry : exported.entrySet()) {
            theState.assignToPort(theEntry.getKey(), theEntry.getValue());
        }
        return theState;
    }

    public BlockState toStartState() {
        BlockState theState = new BlockState();
        for (Map.Entry<VariableDescription, Value> theEntry : imported.entrySet()) {
            theState.assignToPort(theEntry.getKey(), theEntry.getValue());
        }
        return theState;
    }

    public void removeVariable(Variable aVariable) {
        for (Expression theExpression : expressions.toList()) {
            if (theExpression instanceof InitVariableExpression) {
                InitVariableExpression theInit = (InitVariableExpression) theExpression;
                if (theInit.getVariable() == aVariable) {
                    expressions.remove(theExpression);
                }
            }
        }
        program.removeVariable(aVariable);
    }

    public boolean endWithNeverReturningExpression() {
        Expression theLastExpression = expressions.lastExpression();
        return theLastExpression instanceof ReturnExpression ||
                theLastExpression instanceof ReturnValueExpression ||
                theLastExpression instanceof TableSwitchExpression ||
                theLastExpression instanceof LookupSwitchExpression ||
                theLastExpression instanceof ThrowExpression ||
                theLastExpression instanceof GotoExpression;
    }

    public boolean isStrictlyDominatedBy(GraphNode aNode) {
        Set<GraphNode> thePredecessors = new HashSet<>(getPredecessors());
        return thePredecessors.size() == 1 && thePredecessors.contains(aNode);
    }

    public boolean containsGoto() {
        return containsGoto(expressions);
    }

    private boolean containsGoto(ExpressionList aExpressionList) {
        for (Expression theExpression : aExpressionList.toList()) {
            if (theExpression instanceof GotoExpression) {
                return true;
            }
            if (theExpression instanceof ExpressionListContainer) {
                ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (ExpressionList theList : theContainer.getExpressionLists()) {
                    if (containsGoto(theList)) {
                        return true;
                    }
                }
            }
            if (theExpression instanceof InlinedNodeExpression) {
                InlinedNodeExpression theInline = (InlinedNodeExpression) theExpression;
                if (theInline.getNode().containsGoto()) {
                    return true;
                }
            }
        }
        return false;
    }
}