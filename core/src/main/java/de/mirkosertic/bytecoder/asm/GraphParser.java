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
        final Node[] initialStack = new Node[0];
        final Node[] initialLocals = new Node[methodNode.maxLocals];

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
        final Frame startFrame = new Frame();
        startFrame.incomingLocals = initialLocals;
        startFrame.incomingStack = initialStack;
        startRegion.frame = startFrame;
        final GraphParserState initialState = new GraphParserState(initialLocals, initialStack, startRegion, -1);

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
            alreadyVisited.add(flow.currentNode);
            for (final ControlFlow nextOutCome : parse(flow, incomingEdgesPerLabel)) {
                if (!alreadyVisited.contains(nextOutCome.currentNode)) {
                    controlFlowsToCheck.push(nextOutCome);
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

        final Frame frame = new Frame();
        frame.incomingLocals = currentFlow.graphParserState.locals;
        frame.incomingStack = currentFlow.graphParserState.stack;
        region.frame = frame;

        final GraphParserState newState = currentFlow.graphParserState.controlFlowsTo(region);
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
            final Node value = currentState.locals[local];
            final Node[] newStack = new Node[currentState.stack.length + 1];
            System.arraycopy(currentState.stack, 0, newStack, 0, currentState.stack.length);
            newStack[newStack.length - 1] = value;
            final GraphParserState newState = currentState.withNewStack(newStack);
            return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
         } else if (node.getOpcode() == Opcodes.ILOAD) {
            final Node value = currentState.locals[local];
            final Node[] newStack = new Node[currentState.stack.length + 1];
            System.arraycopy(currentState.stack, 0, newStack, 0, currentState.stack.length);
            newStack[newStack.length - 1] = value;
            final GraphParserState newState = currentState.withNewStack(newStack);
            return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
        } else if (node.getOpcode() == Opcodes.ISTORE) {
            final Node[] newStack = new Node[currentState.stack.length - 1];
            System.arraycopy(currentState.stack, 0, newStack, 0, newStack.length);

            final Node value = currentState.stack[currentState.stack.length - 1];
            final GraphParserState newState = currentState.setLocalWithStack(local, value, newStack);
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
            incomingData[0] = currentState.stack[currentState.stack.length - 1];
            for (int i = 0; i < argumentTypes.length; i++) {
                incomingData[i + 1] = currentState.stack[currentState.stack.length - 2 - i];
            }
            final Node[] newStack = new Node[currentState.stack.length - argumentTypes.length];
            System.arraycopy(currentState.stack, 0, newStack, 0, newStack.length);

            final ControlTokenConsumerNode n = graph.newMethodInvocationNode(node);
            n.addIncomingData(incomingData);

            final GraphParserState newState = currentState.controlFlowsTo(n).withNewStack(newStack);
            return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
        }
        throw new IllegalStateException("Not implemented : " + node + " -> " + node.getOpcode());
    }

    private List<ControlFlow> parseIntInsnNode(final ControlFlow currentFlow) {
        final IntInsnNode node = (IntInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        if (node.getOpcode() == Opcodes.BIPUSH) {
            final Node value = graph.newIntNode(node.operand);

            final Node[] newStack = new Node[currentState.stack.length + 1];
            System.arraycopy(currentState.stack, 0, newStack, 0, currentState.stack.length);
            newStack[newStack.length - 1] = value;

            final GraphParserState newState = currentState.withNewStack(newStack);
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

            final Node[] newStack = new Node[currentState.stack.length + 1];
            System.arraycopy(currentState.stack, 0, newStack, 0, currentState.stack.length);
            newStack[newStack.length - 1] = value;

            final GraphParserState newState = currentState.withNewStack(newStack);
            return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
        }
        if (node.getOpcode() == Opcodes.IADD) {

            final Node arg2 = currentState.stack[currentState.stack.length - 1];
            final Node arg1 = currentState.stack[currentState.stack.length - 2];

            final Node value = graph.newAddInt();
            value.addIncomingData(arg1, arg2);

            final Node[] newStack = new Node[currentState.stack.length - 1];
            System.arraycopy(currentState.stack, 0, newStack, 0, currentState.stack.length - 2);
            newStack[newStack.length - 1] = value;

            final GraphParserState newState = currentState.withNewStack(newStack);
            return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));

        }
        throw new IllegalStateException("Not implemented : " + node + " -> " + node.getOpcode());
    }

    private List<ControlFlow> parseFrame(final ControlFlow currentFlow) {
        return Collections.singletonList(currentFlow.continueWith(currentFlow.currentNode.getNext()));
    }

    private GraphParserState introduceCopyInstructions(final GraphParserState graphParserState, final LabelNode targetNode) {
        final RegionNode targetRegion = graph.getOrCreateRegionNodeFor(targetNode.getLabel().toString());
        GraphParserState state = graphParserState;

        for (int i = 0; i < state.locals.length; i++) {
            final Node source = graphParserState.locals[i];
            final int index = i;
            final CopyNode.DataFlowResolver resolver = copy -> {
                final Node target = targetRegion.frame.incomingLocals[index];
                if (source != target) {
                    copy.addIncomingData(source);
                    target.addIncomingData(copy);
                }
            };
            final CopyNode copy = graph.newCopyNode(source.type, resolver);
            state = state.controlFlowsTo(copy);
        }
        for (int i = 0; i < state.stack.length; i++) {
            final Node source = state.stack[i];
            final int index = i;
            final CopyNode.DataFlowResolver resolver = copy -> {
                final Node target = targetRegion.frame.incomingStack[index];
                if (source != target) {
                    copy.addIncomingData(source);
                    target.addIncomingData(copy);
                }
            };
            final CopyNode copy = graph.newCopyNode(source.type, resolver);
            state = state.controlFlowsTo(copy);
        }
        return state.controlFlowsTo(targetRegion);
    }

    private List<ControlFlow> parseJumpInsnNode(final ControlFlow currentFlow, final Map<LabelNode, Map<AbstractInsnNode, EdgeType>> incomingEdgesPerLabel) {
        final JumpInsnNode node = (JumpInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        if (node.getOpcode() == Opcodes.GOTO) {
            final Map<AbstractInsnNode, EdgeType> edges = incomingEdgesPerLabel.get(node.label);
            final GraphParserState newState = introduceCopyInstructions(currentState, node.label);
            if (EdgeType.BACK == edges.get(node)) {
                // Back Edge, do nothing
                return Collections.emptyList();
            }
            return Collections.singletonList(currentFlow.continueWith(node.label, newState));
        } else {
            final Node arg2 = currentState.stack[currentState.stack.length - 1];
            final Node arg1 = currentState.stack[currentState.stack.length - 2];

            final Node[] newStack = new Node[currentState.stack.length - 2];
            System.arraycopy(currentState.stack, 0, newStack, 0, newStack.length);

            final IfNode ifNode = graph.newIfNode();
            ifNode.addIncomingData(arg1, arg2);
            final GraphParserState newState = currentState.controlFlowsTo(ifNode).withNewStack(newStack);

            final List<ControlFlow> results = new ArrayList<>();
            results.add(currentFlow.continueWith(node.getNext(), newState));

            final Map<AbstractInsnNode, EdgeType> edges = incomingEdgesPerLabel.get(node.label);
            final GraphParserState afterCopy = introduceCopyInstructions(newState, node.label);
            if (EdgeType.BACK == edges.get(node)) {
                // Back-Jump, do nothing
            } else {
                results.add(currentFlow.continueWith(node.label, afterCopy));
            }
            return results;
        }
    }

    private List<ControlFlow> parseIincInsnNode(final ControlFlow currentFlow) {
        final IincInsnNode node = (IincInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final int local = node.var;
        final Node currentValue = currentState.locals[local];
        final ControlTokenConsumerNode iincNode = graph.newIIncNode(node.incr);
        iincNode.addIncomingData(currentValue);

        final GraphParserState newState = currentState.controlFlowsTo(iincNode).setLocalWithStack(local, iincNode, currentState.stack);

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
                    final Node[] newLocals = new Node[graphParserState.locals.length];
                    final Node[] newStack = new Node[graphParserState.stack.length];
                    // Copy data to phi nodes

                    for (int i = 0; i < graphParserState.locals.length; i++) {
                        final Node source = graphParserState.locals[i];
                        final Node phi = graph.newPHINode(source.type);
                        final CopyNode.DataFlowResolver resolver = copy -> {
                            copy.addIncomingData(source);
                            phi.addIncomingData(copy);
                        };
                        final CopyNode copy = graph.newCopyNode(source.type, resolver);
                        graphParserState = graphParserState.controlFlowsTo(copy);

                        newLocals[i] = phi;
                    }
                    for (int i = 0; i < graphParserState.stack.length; i++) {
                        final Node source = graphParserState.stack[i];
                        final Node phi = graph.newPHINode(source.type);
                        final CopyNode.DataFlowResolver resolver = copy -> {
                            copy.addIncomingData(source);
                            phi.addIncomingData(copy);
                        };
                        final CopyNode copy = graph.newCopyNode(source.type, resolver);
                        graphParserState = graphParserState.controlFlowsTo(copy);

                        newStack[i] = phi;
                    }
                    final GraphParserState newState = graphParserState.withStackAndLocals(newStack, newLocals);
                    final ControlFlow mergedFlow = currentFlow.continueWith(newState);
                    return parseLabelNode(mergedFlow);
                }
            }

            // Do nothing with this label
            if (labelNode.getNext() == null) {
                return Collections.emptyList();
            }

            if (incomingEdgesPerLabel.containsKey(labelNode)) {
                final RegionNode targetRegion = graph.getOrCreateRegionNodeFor(labelNode.getLabel().toString());
                if (targetRegion.frame == null) {
                    final Frame frame = new Frame();
                    frame.incomingStack = currentFlow.graphParserState.stack;
                    frame.incomingLocals = currentFlow.graphParserState.locals;
                    targetRegion.frame = frame;
                }
                return Collections.singletonList(currentFlow.continueWith(labelNode.getNext()));
            }
            return Collections.singletonList(currentFlow.continueWith(labelNode.getNext()));
        }
        if (currentFlow.currentNode  instanceof LineNumberNode) {
            return parseLineNumberNode(currentFlow);
        }
        if (currentFlow.currentNode  instanceof VarInsnNode) {
            return parseVarInsnNode(currentFlow);
        }
        if (currentFlow.currentNode  instanceof MethodInsnNode) {
            return parseMethodInsNode(currentFlow);
        }
        if (currentFlow.currentNode  instanceof IntInsnNode) {
            return parseIntInsnNode(currentFlow);
        }
        if (currentFlow.currentNode  instanceof InsnNode) {
            return parseInsnNode(currentFlow);
        }
        if (currentFlow.currentNode  instanceof FrameNode) {
            return parseFrame(currentFlow);
        }
        if (currentFlow.currentNode  instanceof JumpInsnNode) {
            return parseJumpInsnNode(currentFlow, incomingEdgesPerLabel);
        }
        if (currentFlow.currentNode  instanceof IincInsnNode) {
            return parseIincInsnNode(currentFlow);
        }
        throw new IllegalStateException("Not implemented : " + currentFlow.currentNode + " -> " + currentFlow.currentNode.getOpcode());
    }

    public Graph graph() {
        return graph;
    }
}
