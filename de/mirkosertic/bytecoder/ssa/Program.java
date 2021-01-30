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

import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeProgram;

import java.util.ArrayList;
import java.util.List;

public class Program {

    private final DebugInformation debugInformation;
    private final ControlFlowGraph controlFlowGraph;
    private final List<Variable> variables;
    private final List<Variable> arguments;
    private BytecodeProgram.FlowInformation flowInformation;
    private long analysisTime;
    private final BytecodeLinkerContext linkerContext;

    public Program(final DebugInformation aDebugInformation, final BytecodeLinkerContext aLinkerContext) {
        controlFlowGraph = new ControlFlowGraph(this);
        variables = new ArrayList<>();
        arguments = new ArrayList<>();
        debugInformation = aDebugInformation;
        analysisTime = 0;
        linkerContext = aLinkerContext;
    }

    public BytecodeLinkerContext getLinkerContext() {
        return linkerContext;
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

    public void addArgument(final Variable aVariable) {
        arguments.add(aVariable);
    }

    public List<Variable> getArguments() {
        return arguments;
    }

    public Variable argumentAt(final int aIndex) {
        if (aIndex>=0 && aIndex < arguments.size()) {
            return arguments.get(aIndex);
        }
        throw new IllegalStateException("No argument at index " + aIndex);
    }

    public ControlFlowGraph getControlFlowGraph() {
        return controlFlowGraph;
    }

    public List<Variable> getVariables() {
        return variables;
    }

    public Variable createVariable(final TypeRef aType) {
        final int theIndex = variables.size();
        return createVariable("var" + theIndex, aType);
    }

    public Variable createVariable(final String aName, final TypeRef aType) {
        final Variable theNewVariable = new Variable(aType, aName, analysisTime);
        variables.add(theNewVariable);
        return theNewVariable;
    }

    public void deleteVariable(final Variable aVariable) {
        variables.remove(aVariable);
    }

    public void incrementAnalysisTime() {
        analysisTime++;
    }

    public long getAnalysisTime() {
        return analysisTime;
    }
}