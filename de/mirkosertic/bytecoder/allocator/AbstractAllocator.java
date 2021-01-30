/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.allocator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.graph.EdgeType;
import de.mirkosertic.bytecoder.graph.Node;
import de.mirkosertic.bytecoder.graph.Partitioning;
import de.mirkosertic.bytecoder.ssa.PHIValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;

public abstract class AbstractAllocator {

    public enum EdgeTypes implements EdgeType {
        dependson;
    }

    private static class ValueNode extends Node<ValueNode, EdgeTypes> {
        private final Value value;

        public ValueNode(final Value value) {
            this.value = value;
        }
    }

    protected final Map<Variable, Register> registerAssignments;
    protected final Map<TypeRef, List<Register>> knownRegisters;
    protected final Function<Variable, TypeRef> typeConverter;
    protected final Map<PHIValue, Variable> phis;
    protected final Map<Variable, Variable> aliases;
    protected final BytecodeLinkerContext linkerContext;

    public AbstractAllocator(final Function<Variable, TypeRef> aTypeConverter, final BytecodeLinkerContext aLinkerContext) {
        registerAssignments = new HashMap<>();
        knownRegisters = new HashMap<>();
        typeConverter = aTypeConverter;
        phis = new HashMap<>();
        aliases = new HashMap<>();
        linkerContext = aLinkerContext;
    }

    public List<Register> assignedRegister() {
        final List<Register> theList = new ArrayList<>(new HashSet<>(registerAssignments.values()));
        theList.sort(Comparator.comparingLong(Register::getNumber));
        return theList;
    }

    public Set<TypeRef> usedRegisterTypes() {
        return knownRegisters.keySet();
    }

    public List<Register> registersOfType(final TypeRef aType) {
        return knownRegisters.get(aType);
    }

    public Register registerAssignmentFor(final Variable v) {
        final Variable theAlias = aliases.get(v);
        if (theAlias != null) {
            final Register theAliasAssignment = registerAssignments.get(theAlias);
            if (theAliasAssignment == null) {
                throw new IllegalStateException("Cannot find assigned register for alias " + theAlias);
            }
            return theAliasAssignment;
        }
        final Register theRegularAssignment = registerAssignments.get(v);
        if (theRegularAssignment == null) {
            throw new IllegalStateException("Cannot find regular assigned register for " + v);
        }
        return theRegularAssignment;
    }

    public Variable variableAssignmentFor(final PHIValue p) {
        final Variable thePHIVariable = phis.get(p);
        if (thePHIVariable == null) {
            throw new IllegalStateException("Cannot find variable for phi " + p);
        }
        return thePHIVariable;
    }

    protected List<Variable> computeSSAReadyVariablesFor(final Program prog) {

        // First of all, we search for PHI Values in all Liveins
        final Map<PHIValue, Set<Value>> thePHIs = new HashMap<>();

        for (final RegionNode theNode : prog.getControlFlowGraph().dominators().getPreOrder()) {
            theNode.liveIn().getPorts().forEach((d, v) -> {
                if (v instanceof PHIValue) {
                    final PHIValue p = (PHIValue) v;
                    final Set<Value> theIncomingValues = new HashSet<>();
                    for (final RegionNode thePred : theNode.getPredecessors()) {
                        final Value theIncomingValue = thePred.liveOut().getPorts().get(d);
                        if (theIncomingValue != p) {
                            theIncomingValues.add(theIncomingValue);
                        }
                    }
                    if (!theIncomingValues.isEmpty()) {
                        if (thePHIs.containsKey(p)) {
                            throw new IllegalStateException();
                        } else {
                            thePHIs.put(p, theIncomingValues);
                        }
                    }
                }
            });
        }

        // PHI Values that are directly mapped to ONE synthetic variable(argument)
        // are constants, and are directly mapped to this variable
        final Set<Value> alreadyMappped = new HashSet<>();

        thePHIs.forEach((phi, values) -> {
            final Set<Value> theEffective = new HashSet<>(values);
            boolean modified = true;
            final Set<PHIValue> theSeen = new HashSet<>();
            guard: while(modified) {
                modified = false;
                for (final Value v : theEffective) {
                    if (v instanceof PHIValue && v != phi && theSeen.add((PHIValue) v)) {
                        final Set<Value> thePHIValues = thePHIs.get(((PHIValue) v));
                        thePHIValues.removeAll(theSeen);
                        if (!thePHIValues.isEmpty()) {
                            theEffective.addAll(thePHIValues);
                            theEffective.remove(v);
                            modified = true;
                            continue guard;
                        }
                    }
                }
            }
            if (theEffective.size() == 1) {
                final Value v = theEffective.iterator().next();
                if (v instanceof Variable) {
                    final Variable var = (Variable) v;
                    if (var.isSynthetic()) {
                        phis.put(phi, var);
                        alreadyMappped.add(phi);
                    }
                }
            }
        });

        // Now, we create a graph from the value dependencies
        final Map<Value, ValueNode> theValueToNodes = new HashMap<>();
        thePHIs.forEach((phi, values)-> {
            if (!alreadyMappped.contains(phi)) {
                final ValueNode thePhiNode = theValueToNodes.computeIfAbsent(phi, ValueNode::new);
                for (final Value thePhiValue : values) {
                    if (!alreadyMappped.contains(thePhiValue)) {
                        if (thePhiValue instanceof Variable) {
                            final Variable var = (Variable) thePhiValue;
                            if (!var.isSynthetic()) {
                                final ValueNode theValueNode = theValueToNodes
                                        .computeIfAbsent(var, ValueNode::new);
                                thePhiNode.addEdgeTo(EdgeTypes.dependson, theValueNode);
                            }
                        } else {
                            final ValueNode theValueNode = theValueToNodes
                                    .computeIfAbsent(thePhiValue, ValueNode::new);
                            thePhiNode.addEdgeTo(EdgeTypes.dependson, theValueNode);
                        }
                    }
                }
            }
        });

        // We try to find partitions in the graph, which are not connected subgraphs
        // Each subgraph will be mapped to its own phi variable
        final Partitioning<ValueNode> thePartitioning = new Partitioning<>(new HashSet<>(theValueToNodes.values()), t -> true);
        final List<Set<ValueNode>> thePartitions = thePartitioning.partitions();

        final List<Variable> theVariables = prog.getVariables();

        for (int i=0;i<thePartitions.size();i++) {
            final Set<ValueNode> thePartition = thePartitions.get(i);
            final Set<Value> thePartitionValues = new HashSet<>();
            thePartition.forEach(t -> {
                final Value theValue = t.value;
                thePartitionValues.add(theValue);
                if (theValue instanceof PHIValue) {
                    thePartitionValues.addAll(thePHIs.get(theValue));
                }
            });

            final TypeRef theWidestType = Value.widestTypeOf(thePartitionValues, linkerContext);

            final Variable theNewPhiVar = prog.createVariable("phi" + i, theWidestType);
            for (final Value thePartitionValue : thePartitionValues) {
                if (thePartitionValue instanceof PHIValue) {
                    phis.put((PHIValue) thePartitionValue, theNewPhiVar);
                } else if (thePartitionValue instanceof Variable) {
                    final Variable v = (Variable) thePartitionValue;
                    aliases.put(v, theNewPhiVar);
                    theVariables.remove(v);

                    theNewPhiVar.liveRange().usedAt(v.liveRange().getDefinedAt());
                    theNewPhiVar.liveRange().usedAt(v.liveRange().getLastUsedAt());
                }
            }
        }

        // Some sanity checks
        if (phis.keySet().size() != thePHIs.keySet().size()) {
            throw new IllegalStateException("Some phis where not correctly processed!");
        }

        return theVariables;
    }
}
