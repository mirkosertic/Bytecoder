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

import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Program {

    public static class Argument {

        private final LocalVariableDescription variableDescription;
        private final Variable variable;

        public Argument(LocalVariableDescription aVariableDescription, Variable aVariable) {
            variableDescription = aVariableDescription;
            variable = aVariable;
        }

        public Variable getVariable() {
            return variable;
        }
    }

    private final ControlFlowGraph controlFlowGraph;
    private final List<Variable> variables;
    private final Set<Variable> globals;
    private final List<Argument> arguments;

    public Program() {
        controlFlowGraph = new ControlFlowGraph(this);
        variables = new ArrayList<>();
        globals = new HashSet<>();
        arguments = new ArrayList<>();
    }

    public void addArgument(LocalVariableDescription aVariableDescription, Variable aVariable) {
        arguments.add(new Argument(aVariableDescription, aVariable));
        globals.add(aVariable);
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public Argument matchingArgumentOf(LocalVariableDescription aVariableDescription) {
        for (Argument theArgument : arguments) {
            if (Objects.equals(theArgument.variableDescription, aVariableDescription)) {
                return theArgument;
            }
        }
        throw new IllegalStateException("No argument matching " + aVariableDescription);
    }

    public ControlFlowGraph getControlFlowGraph() {
        return controlFlowGraph;
    }

    public Set<Variable> globalVariables() {
        Set<Variable> theVariables = new HashSet<>();
        for (RegionNode theNode : controlFlowGraph.getKnownNodes()) {
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
            if (Objects.equals(theVar.getName(), aVariable.getName())) {
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
        for (RegionNode theNode : controlFlowGraph.getKnownNodes()) {
            for (Expression theExpression : theNode.getExpressions().toList()) {
                if (theExpression instanceof PutStaticExpression) {
                    PutStaticExpression theE = (PutStaticExpression) theExpression;
                    theResult.add(BytecodeObjectTypeRef
                            .fromUtf8Constant(theE.getField().getClassIndex().getClassConstant().getConstant()));
                }
            }
        }

        for (Variable theVariable : variables) {
            for (Value theValue : theVariable.incomingDataFlows()) {
                if (theValue instanceof InvokeStaticMethodExpression) {
                    InvokeStaticMethodExpression theInvokeStatic = (InvokeStaticMethodExpression) theValue;
                    theResult.add(theInvokeStatic.getClassName());
                }
                if (theValue instanceof GetStaticExpression) {
                    GetStaticExpression theStaticValue = (GetStaticExpression) theValue;
                    theResult.add(BytecodeObjectTypeRef.fromUtf8Constant(theStaticValue.getField().getClassIndex().getClassConstant().getConstant()));
                }
                if (theValue instanceof StringValue) {
                    theResult.add(BytecodeObjectTypeRef.fromRuntimeClass(String.class));
                }
                if (theValue instanceof ClassReferenceValue) {
                    ClassReferenceValue theClassRef = (ClassReferenceValue) theValue;
                    theResult.add(theClassRef.getType());
                }
                if (theValue instanceof NewArrayExpression) {
                    NewArrayExpression theNewArray = (NewArrayExpression) theValue;
                    if (theNewArray.getType() instanceof BytecodeObjectTypeRef) {
                        theResult.add((BytecodeObjectTypeRef) theNewArray.getType());
                    }
                }
                if (theValue instanceof NewMultiArrayExpression) {
                    NewMultiArrayExpression theNewArray = (NewMultiArrayExpression) theValue;
                    BytecodeTypeRef theTypeRef = theNewArray.getType();
                    if (theTypeRef instanceof BytecodeObjectTypeRef) {
                        theResult.add((BytecodeObjectTypeRef) theTypeRef);
                    }
                }
                if (theValue instanceof NewObjectExpression) {
                    NewObjectExpression theNewObjectValue = (NewObjectExpression) theValue;
                    theResult.add(BytecodeObjectTypeRef.fromUtf8Constant(theNewObjectValue.getType().getConstant()));
                }
            }
        }
        return theResult;
    }

    public void deleteVariable(Variable aVariable) {
        variables.remove(aVariable);
        globals.remove(aVariable);
    }
}