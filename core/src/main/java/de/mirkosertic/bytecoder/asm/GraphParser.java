/*
 * Copyright 2022 Mirko Sertic
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
package de.mirkosertic.bytecoder.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.function.Consumer;

public class GraphParser {

    enum EdgeType {
        FORWARD, BACK
    }

    private final Graph graph;

    private final MethodNode methodNode;

    public GraphParser(final MethodNode methodNode) {
        this.methodNode = methodNode;
        this.graph = new Graph();

        parse();
    }

    private void parse() {
        // Construct the initial parsing state
        final VarNode[] initialStack = new VarNode[0];
        final VarNode[] initialLocals = new VarNode[methodNode.maxLocals];

        final Type methodType = Type.getMethodType(methodNode.desc);

        int localIndex = 0;
        // TODO: Set correct type for this
        initialLocals[localIndex++] = graph.newThisNode(null);
        for (int i = 0; i < methodType.getArgumentTypes().length; i ++)  {
            final Type t = methodType.getArgumentTypes()[i];
            initialLocals[localIndex] = graph.newMethodArgumentNode(t, i);
            localIndex+= t.getSize();
        }

        final RegionNode startRegion = graph.getOrCreateRegionNodeFor("Start");
        final Frame startFrame = new Frame(initialLocals, initialStack);
        startRegion.frame = startFrame;
        final GraphParserState initialState = new GraphParserState(startFrame, startRegion, StandardProjections.DEFAULT, -1);

        // Step 1: We collect all jump targets
        final Map<LabelNode, Map<AbstractInsnNode, EdgeType>> incomingEdgesPerLabel = new HashMap<>();

        // Step 2: Check for back edges
        final Stack<ControlFlow> controlFlowsToCheck = new Stack<>();
        controlFlowsToCheck.push(new ControlFlow(methodNode.instructions.get(0), initialState));
        while (!controlFlowsToCheck.isEmpty()) {
            final ControlFlow flow = controlFlowsToCheck.pop();
            final AbstractInsnNode next = flow.currentNode.getNext();
            if (flow.currentNode instanceof LabelNode) {
                final LabelNode labelNode = (LabelNode) flow.currentNode;
                 if (next != null) {
                     controlFlowsToCheck.push(flow.addLabelAndContinueWith(labelNode, next));
                }
            } else if (flow.currentNode instanceof JumpInsnNode) {
                final JumpInsnNode jump = (JumpInsnNode) flow.currentNode;
                if (jump.getOpcode() == Opcodes.GOTO) {
                    if (flow.visited(jump.label)) {
                        // Back-Edge
                        final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerLabel.computeIfAbsent(jump.label, k -> new HashMap<>());
                        jumps.put(jump, EdgeType.BACK);
                    } else {
                        final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerLabel.computeIfAbsent(jump.label, k -> new HashMap<>());
                        jumps.put(jump, EdgeType.FORWARD);
                        controlFlowsToCheck.push(flow.addLabelAndContinueWith(jump.label, next));
                    }
                } else {
                    if (flow.visited(jump.label)) {
                        // Back-Edge
                        final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerLabel.computeIfAbsent(jump.label, k -> new HashMap<>());
                        jumps.put(jump, EdgeType.BACK);
                    } else {
                        final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerLabel.computeIfAbsent(jump.label, k -> new HashMap<>());
                        jumps.put(jump, EdgeType.FORWARD);
                        controlFlowsToCheck.push(flow.addLabelAndContinueWith(jump.label, next));
                    }

                    final AbstractInsnNode gotoTarget = jump.getNext();
                    if (gotoTarget instanceof LabelNode) {
                        final LabelNode nextNode = (LabelNode) jump.getNext();
                        if (flow.visited(nextNode)) {
                            // Back-Edge
                            final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerLabel.computeIfAbsent(nextNode, k -> new HashMap<>());
                            jumps.put(jump, EdgeType.BACK);
                        } else {
                            final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerLabel.computeIfAbsent(nextNode, k -> new HashMap<>());
                            jumps.put(jump, EdgeType.FORWARD);

                            controlFlowsToCheck.push(flow.addLabelAndContinueWith(nextNode, next));
                        }
                    } else {
                        controlFlowsToCheck.push(flow.continueWith(gotoTarget));
                    }
                }
            } else {
                if (next != null) {
                    if (next instanceof LabelNode) {
                        final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerLabel.computeIfAbsent((LabelNode) next, k -> new HashMap<>());
                        jumps.put(flow.currentNode, EdgeType.FORWARD);
                    }
                    controlFlowsToCheck.push(flow.continueWith(next));
                } else {
                    System.out.println("no more next!");
                }
            }
        }

        // Step 3: We know the back edges,
        // Now we do a forward control flow analysis
        final Set<AbstractInsnNode> alreadyVisited = new HashSet<>();
        controlFlowsToCheck.push(new ControlFlow(methodNode.instructions.get(0), initialState));
        while (!controlFlowsToCheck.isEmpty()) {
            final ControlFlow flow = controlFlowsToCheck.pop();
            System.out.println("Parsing " + flow.currentNode + " opcode " + flow.currentNode.getOpcode());
            alreadyVisited.add(flow.currentNode);
            for (final ControlFlow nextOutCome : parse(flow, incomingEdgesPerLabel)) {
                if (!alreadyVisited.contains(nextOutCome.currentNode)) {
                    controlFlowsToCheck.push(nextOutCome);
                } else {
                    final Node targetNode = graph.registeredMappingFor(nextOutCome.currentNode);
                    if (targetNode instanceof ControlTokenConsumerNode) {
                        flow.graphParserState.controlFlowsTo((ControlTokenConsumerNode) targetNode);
                    } else {
                        System.out.println("Ignoring control from " + flow.graphParserState.lastControlTokenConsumer + " to " + nextOutCome.currentNode + " registered is " + targetNode);
                    }
                }
            }
        }

        // Step 4: Fixup dataflow for copy nodes
        for (final Node n : graph.nodes()) {
            if (n instanceof CopyNode) {
                final CopyNode c = (CopyNode) n;
                c.resolve();
            }
        }
    }

    private List<ControlFlow> parseLabelNode(final ControlFlow currentFlow) {
        final LabelNode node = (LabelNode) currentFlow.currentNode;
        final Label l = node.getLabel();

        final RegionNode region = graph.getOrCreateRegionNodeFor(l.toString());
        graph.registerMapping(node, region);

        region.frame = currentFlow.graphParserState.frame;

        final GraphParserState state = currentFlow.graphParserState;
        final GraphParserState newState = state.controlFlowsTo(region);
        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parseLineNumberNode(final ControlFlow currentFlow) {
        final LineNumberNode node = (LineNumberNode) currentFlow.currentNode;

        final GraphParserState newState = currentFlow.graphParserState.withLineNumber(node.line);
        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parseVarInsnNode(final ControlFlow currentFlow) {
        final VarInsnNode node = (VarInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final int local = node.var;
        if (node.getOpcode() == Opcodes.ALOAD) {
            final VarNode value = currentState.frame.incomingLocals[local];
            final VarNode variable = graph.newVarNode(value.type);
            final CopyNode copy = graph.newCopyNode(value.type, copyNode -> {
                copyNode.addIncomingData(value);
                variable.addIncomingData(copyNode);
            });
            graph.registerMapping(node, copy);

            final Frame newFrame = currentFlow.graphParserState.frame.pushToStack(variable);

            final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
            return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
         } else if (node.getOpcode() == Opcodes.ILOAD) {
            final Node value = currentState.frame.incomingLocals[local];
            final VarNode variable = graph.newVarNode(value.type);
            final CopyNode copy = graph.newCopyNode(value.type, copyNode -> {
                copyNode.addIncomingData(value);
                variable.addIncomingData(copyNode);
            });
            graph.registerMapping(node, copy);

            final Frame newFrame = currentFlow.graphParserState.frame.pushToStack(variable);

            final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
            return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
        } else if (node.getOpcode() == Opcodes.ISTORE) {

            final Frame.PopResult popResult = currentState.frame.popFromStack();

            final VarNode value = popResult.value;
            final VarNode variable = graph.newVarNode(value.type);
            final CopyNode copy = graph.newCopyNode(value.type, copyNode -> {
                copyNode.addIncomingData(value);
                variable.addIncomingData(copyNode);
            });
            graph.registerMapping(node, copy);

            final Frame newFrame = popResult.newFrame.setLocal(local, variable);

            final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
            return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
        }
        throw new IllegalStateException("Not implemented : " + node + " -> " + node.getOpcode());
    }

    private List<ControlFlow> parseMethodInsNode(final ControlFlow currentFlow) {
        final MethodInsnNode node = (MethodInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final Type methodType = Type.getMethodType(node.desc);
        if (node.getOpcode() == Opcodes.INVOKESPECIAL) {
            final Type[] argumentTypes = methodType.getArgumentTypes();
            final Node[] incomingData = new Node[argumentTypes.length + 1];

            Frame.PopResult latest = currentState.frame.popFromStack();
            incomingData[0] = latest.value;
            for (int i = 0; i < argumentTypes.length; i++) {
                latest = currentState.frame.popFromStack();
                incomingData[i + 1] = latest.value;
            }

            final ControlTokenConsumerNode n = graph.newMethodInvocationNode(node);
            n.addIncomingData(incomingData);
            graph.registerMapping(node, n);

            final GraphParserState newState = currentState.controlFlowsTo(n).withFrame(latest.newFrame);
            return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
        }
        throw new IllegalStateException("Not implemented : " + node + " -> " + node.getOpcode());
    }

    private List<ControlFlow> parseIntInsnNode(final ControlFlow currentFlow) {
        final IntInsnNode node = (IntInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        if (node.getOpcode() == Opcodes.BIPUSH) {
            final Node value = graph.newIntNode(node.operand);
            final VarNode varNode = graph.newVarNode(value.type);
            final CopyNode copyNode = graph.newCopyNode(value.type, copyNode1 -> {
                copyNode1.addIncomingData(value);
                varNode.addIncomingData(copyNode1);
            });
            graph.registerMapping(node, copyNode);

            final Frame newFrame = currentState.frame.pushToStack(varNode);

            final GraphParserState newState = currentState.controlFlowsTo(copyNode).withFrame(newFrame);

            return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
        }
        throw new IllegalStateException("Not implemented : " + node + " -> " + node.getOpcode());
    }

    private List<ControlFlow> parseInsnNode(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        if (node.getOpcode() == Opcodes.RETURN) {
            final ReturnNothingNode value = graph.newReturnNode();
            currentState.controlFlowsTo(value);
            return Collections.emptyList();
        }
        if (node.getOpcode() == Opcodes.ICONST_0) {
            final Node value = graph.newIntNode(0);
            final VarNode variable = graph.newVarNode(value.type);
            final CopyNode copy = graph.newCopyNode(value.type, copyNode -> {
                copyNode.addIncomingData(value);
                variable.addIncomingData(copyNode);
            });
            graph.registerMapping(node, copy);

            final Frame newFrame = currentState.frame.pushToStack(variable);

            final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
            return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
        }
        if (node.getOpcode() == Opcodes.IADD) {

            final Frame.PopResult pop1 = currentState.frame.popFromStack();
            final Frame.PopResult pop2 = pop1.newFrame.popFromStack();

            final Node addNode = graph.newAddNode(Type.INT_TYPE);
            addNode.addIncomingData(pop1.value, pop2.value);

            final VarNode varNode = graph.newVarNode(Type.INT_TYPE);
            final CopyNode copy = graph.newCopyNode(Type.INT_TYPE, copyNode -> {
                copyNode.addIncomingData(addNode);
                varNode.addIncomingData(copyNode);
            });
            graph.registerMapping(node, copy);

            final Frame newFrame = pop2.newFrame.pushToStack(varNode);

            final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
            return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));

        }
        throw new IllegalStateException("Not implemented : " + node + " -> " + node.getOpcode());
    }

    private List<ControlFlow> parseFrame(final ControlFlow currentFlow) {
        return Collections.singletonList(currentFlow.continueWith(currentFlow.currentNode.getNext()));
    }

    private GraphParserState introduceCopyInstructions(final GraphParserState graphParserState, final Projection firstProjection, final LabelNode targetNode, final Consumer<CopyNode> consumeFirstCopy) {
        final RegionNode targetRegion = graph.getOrCreateRegionNodeFor(targetNode.getLabel().toString());
        GraphParserState state = graphParserState;

        Consumer<CopyNode> c = consumeFirstCopy;
        Projection f = firstProjection;
        for (int i = 0; i < state.frame.incomingLocals.length; i++) {
            final Node source = state.frame.incomingLocals[i];
            final int index = i;
            final CopyNode.DataFlowResolver resolver = copy -> {
                final Node target = targetRegion.frame.incomingLocals[index];
                if (source != target) {
                    copy.addIncomingData(source);
                    target.addIncomingData(copy);
                }
            };
            final CopyNode copy = graph.newCopyNode(source.type, resolver);
            if (c != null) {
                c.accept(copy);
                c = null;
            }
            state = state.projection(f).controlFlowsTo(copy);
            f = StandardProjections.DEFAULT;
        }
        for (int i = 0; i < state.frame.incomingStack.length; i++) {
            final Node source = state.frame.incomingStack[i];
            final int index = i;
            final CopyNode.DataFlowResolver resolver = copy -> {
                final Node target = targetRegion.frame.incomingStack[index];
                if (source != target) {
                    copy.addIncomingData(source);
                    target.addIncomingData(copy);
                }
            };
            final CopyNode copy = graph.newCopyNode(source.type, resolver);
            if (c != null) {
                c.accept(copy);
                c = null;
            }
            state = state.projection(f).controlFlowsTo(copy);
            f = StandardProjections.DEFAULT;
        }
        return state;
    }

    private List<ControlFlow> parseJumpInsnNode(final ControlFlow currentFlow, final Map<LabelNode, Map<AbstractInsnNode, EdgeType>> incomingEdgesPerLabel) {
        final JumpInsnNode node = (JumpInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        if (node.getOpcode() == Opcodes.GOTO) {
            final Map<AbstractInsnNode, EdgeType> edges = incomingEdgesPerLabel.get(node.label);

            final RegionNode targetNode = graph.getOrCreateRegionNodeFor(node.label.getLabel().toString());
            final GraphParserState afterCopy = introduceCopyInstructions(currentState, StandardProjections.DEFAULT, node.label, copyNode -> graph.registerMapping(node, copyNode));
            final GraphParserState newState = afterCopy.controlFlowsTo(targetNode);

            if (EdgeType.BACK == edges.get(node)) {
                // Back Edge, do nothing
                return Collections.emptyList();
            }
            return Collections.singletonList(currentFlow.continueWith(node.label, newState));

        } else {

            final Frame.PopResult pop1 = currentState.frame.popFromStack();
            final Frame.PopResult pop2 = pop1.newFrame.popFromStack();

            final IfNode ifNode = graph.newIfNode();
            graph.registerMapping(node, ifNode);
            ifNode.addIncomingData(pop1.value, pop2.value);

            final List<ControlFlow> results = new ArrayList<>();

            final GraphParserState origin = currentState.controlFlowsTo(ifNode).withFrame(pop2.newFrame);

            final Map<AbstractInsnNode, EdgeType> edges = incomingEdgesPerLabel.get(node.label);

            // True-Case
            final GraphParserState afterTrueCopy = introduceCopyInstructions(origin, StandardProjections.TRUE, node.label, copyNode -> {
            });
            final RegionNode trueTargetNode = graph.getOrCreateRegionNodeFor(node.label.getLabel().toString());
            //final GraphParserState newTrueState = afterTrueCopy.controlFlowsTo(trueTargetNode);
            if (EdgeType.BACK == edges.get(node)) {
                // Do nothing
            } else {
                results.add(currentFlow.continueWith(node.label, afterTrueCopy));
            }

            // False-Case
            final AbstractInsnNode nextNode = node.getNext();
            if (nextNode instanceof LabelNode) {
                final LabelNode falseLabel = (LabelNode) nextNode;
                final GraphParserState afterFalseCopy = introduceCopyInstructions(origin, StandardProjections.FALSE, falseLabel, copyNode -> {
                });
                if (EdgeType.BACK == edges.get(node)) {
                    // Do nothing
                } else {
                    results.add(currentFlow.continueWith(nextNode, afterFalseCopy));
                }
            } else {
                final GraphParserState newFalseState = origin.projection(StandardProjections.FALSE);
                results.add(currentFlow.continueWith(nextNode, newFalseState));
            }

            return results;
        }
    }

    private List<ControlFlow> parseIincInsnNode(final ControlFlow currentFlow) {
        final IincInsnNode node = (IincInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final int local = node.var;
        final Node currentValue = currentState.frame.incomingLocals[local];
        final Node amountNode = graph.newIntNode(node.incr);

        final Node addNode = graph.newAddNode(Type.INT_TYPE);
        addNode.addIncomingData(currentValue, amountNode);

        final VarNode varNode = graph.newVarNode(Type.INT_TYPE);

        final CopyNode copy = graph.newCopyNode(Type.INT_TYPE, copyNode -> {
            copyNode.addIncomingData(addNode);
            varNode.addIncomingData(copyNode);
        });
        graph.registerMapping(node, copy);

        final Frame newFrame = currentState.frame.setLocal(local, varNode);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse(final ControlFlow currentFlow, final Map<LabelNode, Map<AbstractInsnNode, EdgeType>> incomingEdgesPerLabel) {
        if (currentFlow.currentNode instanceof LabelNode) {
            final LabelNode labelNode = (LabelNode) currentFlow.currentNode;

            final Map<AbstractInsnNode, EdgeType> incomingEdges = incomingEdgesPerLabel.get(labelNode);
            if (incomingEdges != null) {
                if (incomingEdges.size() > 1) {
                    // We do have multiple incoming edges, hence we need PHI nodes
                    GraphParserState graphParserState = currentFlow.graphParserState;
                    final VarNode[] newLocals = new VarNode[graphParserState.frame.incomingLocals.length];
                    final VarNode[] newStack = new VarNode[graphParserState.frame.incomingStack.length];
                    // Copy data to phi nodes

                    for (int i = 0; i < graphParserState.frame.incomingLocals.length; i++) {
                        final Node source = graphParserState.frame.incomingLocals[i];
                        final PHINode phi = graph.newPHINode(source.type);
                        final CopyNode.DataFlowResolver resolver = copy -> {
                            copy.addIncomingData(source);
                            phi.addIncomingData(copy);
                        };
                        final CopyNode copy = graph.newCopyNode(source.type, resolver);
                        graphParserState = graphParserState.controlFlowsTo(copy);

                        newLocals[i] = phi;
                    }
                    for (int i = 0; i < graphParserState.frame.incomingStack.length; i++) {
                        final Node source = graphParserState.frame.incomingStack[i];
                        final PHINode phi = graph.newPHINode(source.type);
                        final CopyNode.DataFlowResolver resolver = copy -> {
                            copy.addIncomingData(source);
                            phi.addIncomingData(copy);
                        };
                        final CopyNode copy = graph.newCopyNode(source.type, resolver);
                        graphParserState = graphParserState.controlFlowsTo(copy);

                        newStack[i] = phi;
                    }

                    final Frame newFrame = graphParserState.frame.withLocalsAndStack(newLocals, newStack);

                    final GraphParserState newState = graphParserState.withFrame(newFrame);
                    final ControlFlow mergedFlow = currentFlow.continueWith(newState);
                    return parseLabelNode(mergedFlow);
                }
            }

            // Do nothing with this label
            if (labelNode.getNext() == null) {
                return Collections.emptyList();
            }

            return parseLabelNode(currentFlow);
        }
        if (currentFlow.currentNode instanceof LineNumberNode) {
            return parseLineNumberNode(currentFlow);
        }
        if (currentFlow.currentNode instanceof VarInsnNode) {
            return parseVarInsnNode(currentFlow);
        }
        if (currentFlow.currentNode instanceof MethodInsnNode) {
            return parseMethodInsNode(currentFlow);
        }
        if (currentFlow.currentNode instanceof IntInsnNode) {
            return parseIntInsnNode(currentFlow);
        }
        if (currentFlow.currentNode instanceof InsnNode) {
            return parseInsnNode(currentFlow);
        }
        if (currentFlow.currentNode instanceof FrameNode) {
            return parseFrame(currentFlow);
        }
        if (currentFlow.currentNode instanceof JumpInsnNode) {
            return parseJumpInsnNode(currentFlow, incomingEdgesPerLabel);
        }
        if (currentFlow.currentNode instanceof IincInsnNode) {
            return parseIincInsnNode(currentFlow);
        }
        throw new IllegalStateException("Not implemented : " + currentFlow.currentNode + " -> " + currentFlow.currentNode.getOpcode());
    }

    public Graph graph() {
        return graph;
    }
}
