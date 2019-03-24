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

import de.mirkosertic.bytecoder.core.BytecodeProgram;

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

        public Argument(final LocalVariableDescription aVariableDescription, final Variable aVariable) {
            variableDescription = aVariableDescription;
            variable = aVariable;
        }

        public Variable getVariable() {
            return variable;
        }
    }

    private final DebugInformation debugInformation;
    private final ControlFlowGraph controlFlowGraph;
    private final List<Variable> variables;
    private final List<Argument> arguments;
    private BytecodeProgram.FlowInformation flowInformation;

    public Program(final DebugInformation aDebugInformation) {
        controlFlowGraph = new ControlFlowGraph(this);
        variables = new ArrayList<>();
        arguments = new ArrayList<>();
        debugInformation = aDebugInformation;
    }

    public DebugInformation getDebugInformation() {
        return debugInformation;
    }

    public void setFlowInformation(final BytecodeProgram.FlowInformation flowInformation) {
        this.flowInformation = flowInformation;
    }

    public BytecodeProgram.FlowInformation getFlowInformation() {
        return flowInformation;
    }

    public void addArgument(final LocalVariableDescription aVariableDescription, final Variable aVariable) {
        arguments.add(new Argument(aVariableDescription, aVariable));
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public Argument matchingArgumentOf(final LocalVariableDescription aVariableDescription) {
        for (final Argument theArgument : arguments) {
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
        final Set<Variable> theVariables = new HashSet<>();
        for (final RegionNode theNode : controlFlowGraph.getKnownNodes()) {
            final BlockState theStartState = theNode.toStartState();
            for (final Value theValue : theStartState.getPorts().values()) {
                if (theValue instanceof Variable) {
                    theVariables.add((Variable) theValue);
                }
            }
        }
        return theVariables;
    }

    public boolean isGlobalVariable(final Variable aVariable) {
        for (final Variable theVar : globalVariables()) {
            if (Objects.equals(theVar.getName(), aVariable.getName())) {
                return true;
            }
        }
        return false;
    }

    public List<Variable> getVariables() {
        final List<Variable> theVariables = new ArrayList<>(variables);
        theVariables.sort(Comparator.comparing(Variable::getName));
        return theVariables;
    }

    public Variable createVariable(final TypeRef aType) {
        final int theIndex = variables.size();
        return createVariable("var" + theIndex, aType);
    }

    public Variable createVariable(final String aName, final TypeRef aType) {
        final Variable theNewVariable = new Variable(aType, aName);
        variables.add(theNewVariable);
        return theNewVariable;
    }

    public void deleteVariable(final Variable aVariable) {
        variables.remove(aVariable);
    }
}