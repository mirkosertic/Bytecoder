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
        final GraphParserState initialState = new GraphParserState(initialLocals, initialStack, startRegion, -1);

        // Step 1: We collect all jump targets
        final Set<LabelNode> jumpTargets = new HashSet<>();
        final Set<LabelNode> labelsWithBackEdges = new HashSet<>();

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
                        jumpTargets.add(jump.label);
                        labelsWithBackEdges.add(jump.label);
                    } else {
                        jumpTargets.add(jump.label);
                        controlFlowsToCheck.push(flow.addLabelAndContinueWith(jump.label, next));
                    }
                } else {
                    if (flow.visited(jump.label)) {
                        // Back-Edge
                        jumpTargets.add(jump.label);
                        labelsWithBackEdges.add(jump.label);
                    } else {
                        jumpTargets.add(jump.label);
                        controlFlowsToCheck.push(flow.addLabelAndContinueWith(jump.label, next));
                    }

                    final LabelNode nextNode = (LabelNode) jump.getNext();
                    if (flow.visited(nextNode)) {
                        // Back-Edge
                        jumpTargets.add(nextNode);
                        labelsWithBackEdges.add(nextNode);
                    } else {
                        jumpTargets.add(nextNode);
                        controlFlowsToCheck.push(flow.addLabelAndContinueWith(nextNode, next));
                    }
                }
            } else {
                if (next != null) {
                    controlFlowsToCheck.push(flow.continueWith(next));
                }
            }
        }

        // Step 3: We know the back edges,
        // Now we do a forward control flow analysis
        controlFlowsToCheck.push(new ControlFlow(methodNode.instructions.get(0), initialState));
        while (!controlFlowsToCheck.isEmpty()) {
            final ControlFlow flow = controlFlowsToCheck.pop();
            List<ControlFlow> outcomes = parse(flow, jumpTargets, labelsWithBackEdges);
            for (final ControlFlow nextOutCome : outcomes) {
                controlFlowsToCheck.push(nextOutCome);
            }
        }
    }

    private List<ControlFlow> parseLabelNode(final ControlFlow currentFlow) {
        final LabelNode node = (LabelNode) currentFlow.currentNode;
        final Label l = node.getLabel();

        final RegionNode region = graph.getOrCreateRegionNodeFor(l.toString());
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
            final Node[] incomingData = new Node[argumentTypes.length];
            for (int i = 0; i < argumentTypes.length; i++) {
                incomingData[i] = currentState.stack[currentState.stack.length - 1 - i];
            }
            final Node[] newStack = new Node[currentState.stack.length - argumentTypes.length];
            System.arraycopy(currentState.stack, 0, newStack, 0, newStack.length);

            final ControlTokenConsumerNode n = graph.newMethodInvocationNode(node);
            n.addIncomingData(newStack);

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

    private List<ControlFlow> parseJumpInsnNode(final ControlFlow currentFlow, final Set<LabelNode> jumpTargets, final Set<LabelNode> labelsWithBackEdges) {
        final JumpInsnNode node = (JumpInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        if (node.getOpcode() == Opcodes.GOTO) {
            if (labelsWithBackEdges.contains(node.label)) {
                // Back-Jump, do nothing
                currentState.controlFlowsTo(graph.getOrCreateRegionNodeFor(node.label.getLabel().toString()));
                return Collections.emptyList();
            }
            final GraphParserState newState = currentState.controlFlowsTo(graph.getOrCreateRegionNodeFor(node.label.getLabel().toString()));
            return Collections.singletonList(currentFlow.continueWith(node.label, newState));
        } else {
            final Node arg2 = currentState.stack[currentState.stack.length - 1];
            final Node arg1 = currentState.stack[currentState.stack.length - 2];

            final Node[] newStack = new Node[currentState.stack.length - 2];
            System.arraycopy(currentState.stack, 0, newStack, 0, newStack.length);

            final IfNode ifNode = graph.newIfNode();
            ifNode.addIncomingData(arg1, arg2);
            final GraphParserState newState = currentState.controlFlowsTo(ifNode).withNewStack(newStack);

            final RegionNode jumpTarget = graph.getOrCreateRegionNodeFor(node.label.getLabel().toString());
            ifNode.addControlFlowTo(jumpTarget);

            final List<ControlFlow> results = new ArrayList<>();

            results.add(currentFlow.continueWith(node.getNext(), newState));

            if (labelsWithBackEdges.contains(node.label)) {
                // Back-Jump, do nothing
            } else {
                results.add(currentFlow.continueWith(node.label, newState));
            }
            return results;
        }
    }

    private List<ControlFlow> parseIincInsnNode(final ControlFlow currentFlow) {
        final IincInsnNode node = (IincInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final int local = node.var;
        final Node currentValue = currentState.locals[local];
        final Node intConstant = graph.newIntNode(node.incr);
        final Node addNode = graph.newAddInt();
        addNode.addIncomingData(currentValue, intConstant);
        final GraphParserState newState = currentState.setLocalWithStack(local, addNode, currentState.stack);

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse(final ControlFlow currentFlow, final Set<LabelNode> jumpTargets, final Set<LabelNode> labelsWithBackEdges) {
        if (currentFlow.currentNode instanceof LabelNode) {
            final LabelNode labelNode = (LabelNode) currentFlow.currentNode;

            if (jumpTargets.contains(labelNode)) {
                return parseLabelNode(currentFlow);
            }

            // Do nothing with this label
            if (labelNode.getNext() == null) {
                return Collections.emptyList();
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
            return parseJumpInsnNode(currentFlow, jumpTargets, labelsWithBackEdges);
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
