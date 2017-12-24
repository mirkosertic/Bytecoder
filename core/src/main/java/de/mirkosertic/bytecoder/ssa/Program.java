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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

public class Program {

    private final ControlFlowGraph controlFlowGraph;
    private final List<Variable> variables;
    private final Map<String, Variable> globals;

    public Program() {
        controlFlowGraph = new ControlFlowGraph(this);
        variables = new ArrayList<>();
        globals = new HashMap<>();
    }

    public ControlFlowGraph getControlFlowGraph() {
        return controlFlowGraph;
    }

    public Set<Variable> globalVariables() {
        Set<Variable> theVariables = new HashSet<>();
        for (GraphNode theNode : controlFlowGraph.getKnownNodes()) {
            BlockState theStartState = theNode.toStartState();
            for (Value theValue : theStartState.getPorts().values()) {
                if (theValue instanceof Variable) {
                    theVariables.add((Variable) theValue);
                }
            }
        }
        return theVariables;
    }

    public boolean isGlobalVariable(Variable aVariable) {
        for (Variable theVar : globalVariables()) {
            if (theVar.getName().equals(aVariable.getName())) {
                return true;
            }
        }
        return false;
    }

    public List<Variable> getVariables() {
        List<Variable> theVariables = new ArrayList<>();
        theVariables.addAll(variables);
        theVariables.sort(Comparator.comparing(Variable::getName));
        return theVariables;
    }

    public Variable createVariable(TypeRef aType) {
        int theIndex = variables.size();
        Variable theNewVariable = new Variable(aType, "var" + theIndex);
        variables.add(theNewVariable);
        return theNewVariable;
    }

    public Set<BytecodeObjectTypeRef> getStaticReferences() {
        Set<BytecodeObjectTypeRef> theResult = new HashSet<>();
        for (GraphNode theNode : controlFlowGraph.getDominatedNodes()) {
            for (Expression theExpression : theNode.getExpressions().toList()) {
                if (theExpression instanceof PutStaticExpression) {
                    PutStaticExpression theE = (PutStaticExpression) theExpression;
                    theResult.add(BytecodeObjectTypeRef
                            .fromUtf8Constant(theE.getField().getClassIndex().getClassConstant().getConstant()));
                }
            }
        }

        for (Variable theVariable : variables) {
            for (Value theValue : theVariable.consumedValues(Value.ConsumptionType.INITIALIZATION)) {
                if (theValue instanceof GetStaticValue) {
                    GetStaticValue theStaticValue = (GetStaticValue) theValue;
                    theResult.add(BytecodeObjectTypeRef.fromUtf8Constant(theStaticValue.getField().getClassIndex().getClassConstant().getConstant()));
                }
                if (theValue instanceof ClassReferenceValue) {
                    ClassReferenceValue theClassRef = (ClassReferenceValue) theValue;
                    theResult.add(theClassRef.getType());
                }
                if (theValue instanceof NewArrayValue) {
                    NewArrayValue theNewArray = (NewArrayValue) theValue;
                    if (theNewArray.getType() instanceof BytecodeObjectTypeRef) {
                        theResult.add((BytecodeObjectTypeRef) theNewArray.getType());
                    }
                }
                if (theValue instanceof NewMultiArrayValue) {
                    NewMultiArrayValue theNewArray = (NewMultiArrayValue) theValue;
                    BytecodeTypeRef theTypeRef = theNewArray.getType();
                    if (theTypeRef instanceof BytecodeObjectTypeRef) {
                        theResult.add((BytecodeObjectTypeRef) theTypeRef);
                    }
                }
                if (theValue instanceof NewObjectValue) {
                    NewObjectValue theNewObjectValue = (NewObjectValue) theValue;
                    theResult.add(BytecodeObjectTypeRef.fromUtf8Constant(theNewObjectValue.getType().getConstant()));
                }
            }
        }
        return theResult;
    }

    public Variable getOrCreateTrulyGlobal(String aName, TypeRef aType) {
        Variable theVariablle = globals.get(aName);
        if (theVariablle == null) {
            Variable theVariable = new Variable(aType, aName);
            globals.put(aName, theVariable);
        }
        return theVariablle;
    }

    public boolean isTrulyGlobal(Variable aVariable) {
        return globals.containsValue(aVariable);
    }

    public void promoteToTrulyGlobal(Variable aVariable) {
        globals.put(aVariable.getName(), aVariable);
    }
}