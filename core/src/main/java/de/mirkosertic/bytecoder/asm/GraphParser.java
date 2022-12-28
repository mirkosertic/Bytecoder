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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class GraphParser {

    private final Graph graph;

    private final MethodNode methodNode;

    public GraphParser(final MethodNode methodNode) {
        this.methodNode = methodNode;
        this.graph = new Graph();

        parse();
    }

    private Region getOrCreateRegionNodeFor(final LabelNode label) {
        final String nodeLabel = label.getLabel().toString();
        final Region r = graph.regionByLabel(nodeLabel);
        if (r != null) {
            return r;
        }
        for (final TryCatchBlockNode n : methodNode.tryCatchBlocks) {
            if (n.start == label) {
                return graph.newTryCatch(nodeLabel);
            }
        }
        return graph.newRegion(nodeLabel);
    }

    private void parse() {
        // Construct the initial parsing state
        final Value[] initialStack = new Value[0];
        final Value[] initialLocals = new Value[methodNode.maxLocals];

        final Type methodType = Type.getMethodType(methodNode.desc);

        int localIndex = 0;
        // TODO: Set correct type for this
        initialLocals[localIndex++] = graph.newThis(null);
        for (int i = 0; i < methodType.getArgumentTypes().length; i ++)  {
            final Type t = methodType.getArgumentTypes()[i];
            initialLocals[localIndex] = graph.newMethodArgument(t, i);
            localIndex+= t.getSize();
        }

        final Frame startFrame = new Frame(initialLocals, initialStack);
        final Region startRegion = (Region) graph.register(new Region(Graph.START_REGION_NAME));
        startRegion.frame = startFrame;
        graph.addFixup(new ForwardControlFlowFixup(startRegion, startRegion.frame, StandardProjections.DEFAULT_FORWARD, methodNode.instructions.getFirst()));

        final GraphParserState initialState = new GraphParserState(startFrame, startRegion, -1);

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
                 for (final TryCatchBlockNode tryCatchBlockNode : methodNode.tryCatchBlocks) {
                     if (tryCatchBlockNode.type != null && tryCatchBlockNode.start == labelNode) {
                         controlFlowsToCheck.push(flow.addLabelAndContinueWith(labelNode, tryCatchBlockNode.handler));
                     }
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
                        controlFlowsToCheck.push(flow.addLabelAndContinueWith(jump.label, jump.label));
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
        for (final TryCatchBlockNode tryCatchBlockNode : methodNode.tryCatchBlocks) {
            if (tryCatchBlockNode.type != null) {
                final LabelNode start = tryCatchBlockNode.start;
                final LabelNode handler = tryCatchBlockNode.handler;

                final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerLabel.computeIfAbsent(handler, k -> new HashMap<>());
                jumps.put(start, EdgeType.FORWARD);
            }
        }

        // Step 3: We know the back edges,
        // Now we do a forward control flow analysis
        final Set<AbstractInsnNode> alreadyVisited = new HashSet<>();
        controlFlowsToCheck.push(new ControlFlow(methodNode.instructions.get(0), initialState));
        while (!controlFlowsToCheck.isEmpty()) {
            final ControlFlow flow = controlFlowsToCheck.pop();
            if (alreadyVisited.add(flow.currentNode)) {
                for (final ControlFlow nextOutCome : parse(flow, incomingEdgesPerLabel)) {
                    if (!alreadyVisited.contains(nextOutCome.currentNode)) {
                        controlFlowsToCheck.push(nextOutCome);
                    }
                }
            }
        }

        // Step 4: Fixup stuff not possible during analysis
        graph.applyFixups();
    }

    private List<ControlFlow> parseLabelNode(final ControlFlow currentFlow) {
        final LabelNode node = (LabelNode) currentFlow.currentNode;

        final Region region = getOrCreateRegionNodeFor(node);
        region.frame = currentFlow.graphParserState.frame;
        graph.registerTranslation(node, new InstructionTranslation(region, region.frame));

        final GraphParserState state = currentFlow.graphParserState;
        final List<ControlFlow> flowsToCheck = new ArrayList<>();
        if (node.getNext() != null) {
            graph.addFixup(new ForwardControlFlowFixup(region, state.frame, StandardProjections.DEFAULT_FORWARD, node.getNext()));
            flowsToCheck.add(currentFlow.continueWith(node.getNext(), state));
        }

        // Check for exceptional flows
        for (final TryCatchBlockNode tryCatchBlockNode : methodNode.tryCatchBlocks) {
            if (tryCatchBlockNode.start == node && tryCatchBlockNode.type != null) {
                final Region startRegion = getOrCreateRegionNodeFor(tryCatchBlockNode.start);
                final Frame frameWithPushedException = state.frame.pushToStack(graph.newCaughtException(Type.getObjectType(tryCatchBlockNode.type)));
                graph.addFixup(new ForwardControlFlowFixup(startRegion, state.frame, StandardProjections.EXCEPTION_FORWARD, tryCatchBlockNode.handler));
                flowsToCheck.add(currentFlow.continueWith(tryCatchBlockNode.handler, state.withFrame(frameWithPushedException)));
            }
        }

        return flowsToCheck;
    }

    private List<ControlFlow> parseLineNumberNode(final ControlFlow currentFlow) {
        final LineNumberNode node = (LineNumberNode) currentFlow.currentNode;

        final GraphParserState newState = currentFlow.graphParserState.withLineNumber(node.line);
        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_ALOAD(final ControlFlow currentFlow) {
        final VarInsnNode node = (VarInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final int local = node.var;

        final Value value = currentState.frame.incomingLocals[local];
        final Variable variable = graph.newVariable(value.type);
        final Copy copy = graph.newCopy(value.type);
        copy.addIncomingData(value);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = currentFlow.graphParserState.frame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ForwardControlFlowFixup(copy, newState.frame, StandardProjections.DEFAULT_FORWARD, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_ILOAD(final ControlFlow currentFlow) {
        final VarInsnNode node = (VarInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final int local = node.var;

        final Node value = currentState.frame.incomingLocals[local];
        final Variable variable = graph.newVariable(value.type);
        final Copy copy = graph.newCopy(value.type);
        copy.addIncomingData(value);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = currentFlow.graphParserState.frame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ForwardControlFlowFixup(copy, newState.frame, StandardProjections.DEFAULT_FORWARD, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_ISTORE(final ControlFlow currentFlow) {
        final VarInsnNode node = (VarInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final int local = node.var;

        final Frame.PopResult popResult = currentState.frame.popFromStack();

        final Value value = popResult.value;
        final Variable variable = graph.newVariable(value.type);
        final Copy copy = graph.newCopy(value.type);
        copy.addIncomingData(value);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = popResult.newFrame.setLocal(local, variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ForwardControlFlowFixup(copy, newState.frame, StandardProjections.DEFAULT_FORWARD, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_ASTORE(final ControlFlow currentFlow) {
        final VarInsnNode node = (VarInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final int local = node.var;

        final Frame.PopResult popResult = currentState.frame.popFromStack();

        final Value value = popResult.value;
        final Variable variable = graph.newVariable(value.type);
        final Copy copy = graph.newCopy(value.type);
        copy.addIncomingData(value);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = popResult.newFrame.setLocal(local, variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ForwardControlFlowFixup(copy, newState.frame, StandardProjections.DEFAULT_FORWARD, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parseVarInsnNode(final ControlFlow currentFlow) {
        final VarInsnNode node = (VarInsnNode) currentFlow.currentNode;
        switch (node.getOpcode()) {
            case Opcodes.ALOAD:
                return parse_ALOAD(currentFlow);
            case Opcodes.ILOAD:
                return parse_ILOAD(currentFlow);
            case Opcodes.ISTORE:
                return parse_ISTORE(currentFlow);
            case Opcodes.ASTORE:
                return parse_ASTORE(currentFlow);
            default:
                throw new IllegalStateException("Not implemented : " + node + " -> " + node.getOpcode());
        }
    }

    private List<ControlFlow> parse_INVOKESPECIAL(final ControlFlow currentFlow) {
        final MethodInsnNode node = (MethodInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final Type methodType = Type.getMethodType(node.desc);
        final Type[] argumentTypes = methodType.getArgumentTypes();
        final Node[] incomingData = new Node[argumentTypes.length + 1];

        Frame.PopResult latest = currentState.frame.popFromStack();
        incomingData[0] = latest.value;
        for (int i = 0; i < argumentTypes.length; i++) {
            latest = currentState.frame.popFromStack();
            incomingData[i + 1] = latest.value;
        }

        final ControlTokenConsumer n = graph.newMethodInvocation(node);
        n.addIncomingData(incomingData);
        graph.registerTranslation(node, new InstructionTranslation(n, currentState.frame));

        final GraphParserState newState = currentState.controlFlowsTo(n).withFrame(latest.newFrame);
        graph.addFixup(new ForwardControlFlowFixup(n, newState.frame, StandardProjections.DEFAULT_FORWARD, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parseMethodInsNode(final ControlFlow currentFlow) {
        final MethodInsnNode node = (MethodInsnNode) currentFlow.currentNode;
        switch (node.getOpcode()) {
            case Opcodes.INVOKESPECIAL:
                return parse_INVOKESPECIAL(currentFlow);
            default:
                throw new IllegalStateException("Not implemented : " + node + " -> " + node.getOpcode());
        }
    }

    private List<ControlFlow> parse_BIPUSH(final ControlFlow currentFlow) {
        final IntInsnNode node = (IntInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final Node value = graph.newIntNode(node.operand);
        final Variable variable = graph.newVariable(value.type);
        final Copy copyNode = graph.newCopy(value.type);
        copyNode.addIncomingData(value);
        variable.addIncomingData(copyNode);
        graph.registerTranslation(node, new InstructionTranslation(copyNode, currentState.frame));

        final Frame newFrame = currentState.frame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copyNode).withFrame(newFrame);
        graph.addFixup(new ForwardControlFlowFixup(copyNode, newState.frame, StandardProjections.DEFAULT_FORWARD, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_SIPUSH(final ControlFlow currentFlow) {
        final IntInsnNode node = (IntInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Node value = graph.newShortNode((short) node.operand);
        final Variable variable = graph.newVariable(value.type);
        final Copy copyNode = graph.newCopy(value.type);
        copyNode.addIncomingData(value);
        variable.addIncomingData(copyNode);
        graph.registerTranslation(node, new InstructionTranslation(copyNode, currentState.frame));

        final Frame newFrame = currentState.frame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copyNode).withFrame(newFrame);
        graph.addFixup(new ForwardControlFlowFixup(copyNode, newState.frame, StandardProjections.DEFAULT_FORWARD, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parseIntInsnNode(final ControlFlow currentFlow) {
        final IntInsnNode node = (IntInsnNode) currentFlow.currentNode;
        switch (node.getOpcode()) {
            case Opcodes.BIPUSH:
                return parse_BIPUSH(currentFlow);
            case Opcodes.SIPUSH:
                return parse_SIPUSH(currentFlow);
            default:
                throw new IllegalStateException("Not implemented : " + node + " -> " + node.getOpcode());
        }
    }

    private List<ControlFlow> parse_RETURN(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final ReturnNothing value = graph.newReturnNothing();
        graph.registerTranslation(node, new InstructionTranslation(value, currentState.frame));
        currentState.controlFlowsTo(value);
        return Collections.emptyList();
    }

    private List<ControlFlow> parse_ICONSTX(final ControlFlow currentFlow, final int constant) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final Node value = graph.newIntNode(constant);
        final Variable variable = graph.newVariable(value.type);
        final Copy copy = graph.newCopy(value.type);
        copy.addIncomingData(value);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = currentState.frame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ForwardControlFlowFixup(copy, newState.frame, StandardProjections.DEFAULT_FORWARD, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_IADD(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop2 = currentState.frame.popFromStack();
        final Frame.PopResult pop1 = pop2.newFrame.popFromStack();

        final Node addNode = graph.newAdd(Type.INT_TYPE);
        addNode.addIncomingData(pop1.value, pop2.value);

        final Variable variable = graph.newVariable(Type.INT_TYPE);
        final Copy copy = graph.newCopy(Type.INT_TYPE);
        copy.addIncomingData(addNode);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = pop1.newFrame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ForwardControlFlowFixup(copy, newState.frame, StandardProjections.DEFAULT_FORWARD, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_IDIV(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop2 = currentState.frame.popFromStack();
        final Frame.PopResult pop1 = pop2.newFrame.popFromStack();

        final Node divNode = graph.newDiv(Type.INT_TYPE);
        divNode.addIncomingData(pop1.value, pop2.value);

        final Variable variable = graph.newVariable(Type.INT_TYPE);
        final Copy copy = graph.newCopy(Type.INT_TYPE);
        copy.addIncomingData(divNode);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = pop1.newFrame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ForwardControlFlowFixup(copy, newState.frame, StandardProjections.DEFAULT_FORWARD, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parseInsnNode(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        switch (node.getOpcode()) {
            case Opcodes.RETURN:
                return parse_RETURN(currentFlow);
            case Opcodes.ICONST_0:
                return parse_ICONSTX(currentFlow, 0);
            case Opcodes.IADD:
                return parse_IADD(currentFlow);
            case Opcodes.IDIV:
                return parse_IDIV(currentFlow);
            default:
                throw new IllegalStateException("Not implemented : " + node + " -> " + node.getOpcode());
        }
    }

    private List<ControlFlow> parseFrame(final ControlFlow currentFlow) {
        final FrameNode frameNode = (FrameNode) currentFlow.currentNode;
        if (frameNode.stack.size() != currentFlow.graphParserState.frame.incomingStack.length) {
            System.out.println("Parser stack does not match with compiled bytecode stack!");
        }
        return Collections.singletonList(currentFlow.continueWith(currentFlow.currentNode.getNext()));
    }

    private GraphParserState introduceCopyInstructionsForBackEdge(final GraphParserState graphParserState, final Projection firstProjection, final LabelNode targetNode) {
        GraphParserState state = graphParserState;

        final InstructionTranslation translation = graph.translationFor(targetNode);
        if (translation == null) {
            throw new IllegalStateException("Unknown translation!");
        }

        final Frame targetFrame = translation.frame;
        Projection f = firstProjection;
        for (int i = 0; i < state.frame.incomingLocals.length; i++) {
            final Value source = state.frame.incomingLocals[i];
            final Value target = targetFrame.incomingLocals[i];
            if ((source != null) && (source != target)) {
                final Copy copy = graph.newCopy(source.type);
                copy.addIncomingData(source);
                target.addIncomingData(copy);

                state.lastControlTokenConsumer.addControlFlowTo(f, copy);
                state = state.controlFlowsTo(copy);
                f = StandardProjections.DEFAULT_FORWARD;
            }
        }
        for (int i = 0; i < state.frame.incomingStack.length; i++) {
            final Value source = state.frame.incomingStack[i];
            final Value target = targetFrame.incomingStack[i];
            if ((source != null) && (source != target)) {
                final Copy copy = graph.newCopy(source.type);
                copy.addIncomingData(source);
                target.addIncomingData(copy);

                state.lastControlTokenConsumer.addControlFlowTo(f, copy);
                state = state.controlFlowsTo(copy);
                f = StandardProjections.DEFAULT_FORWARD;
            }
        }
        return state;
    }

    private List<ControlFlow> parse_GOTO(final ControlFlow currentFlow, final Map<LabelNode, Map<AbstractInsnNode, EdgeType>> incomingEdgesPerLabel) {
        final JumpInsnNode node = (JumpInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Map<AbstractInsnNode, EdgeType> edges = incomingEdgesPerLabel.get(node.label);

        final Goto gotoNode = graph.newGoto();

        graph.registerTranslation(node, new InstructionTranslation(gotoNode, currentState.frame));

        final Region target = graph.regionByLabel(node.label.getLabel().toString());
        if (EdgeType.BACK == edges.get(node)) {
            // Back Edge, we know the state of the target label and can create copy instructions accordingly
            if (target == null) {
                throw new IllegalStateException("Unknown back edge!");
            }

            final GraphParserState afterCopy = introduceCopyInstructionsForBackEdge(currentState.controlFlowsTo(gotoNode), StandardProjections.DEFAULT_FORWARD, node.label);
            afterCopy.lastControlTokenConsumer.addControlFlowTo(StandardProjections.DEFAULT_BACKWARD, target);
            return Collections.emptyList();
        }
        graph.addFixup(new ForwardControlFlowFixup(gotoNode, currentState.frame, StandardProjections.DEFAULT_FORWARD, node.label));
        return Collections.singletonList(currentFlow.continueWith(node.label, currentState.controlFlowsTo(target)));
    }

    private List<ControlFlow> parse_IF(final ControlFlow currentFlow, final If.Operation operation, final Map<LabelNode, Map<AbstractInsnNode, EdgeType>> incomingEdgesPerLabel) {
        final JumpInsnNode node = (JumpInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop1 = currentState.frame.popFromStack();
        final Frame.PopResult pop2 = pop1.newFrame.popFromStack();

        final If ifNode = graph.newIf(operation);
        graph.registerTranslation(node, new InstructionTranslation(ifNode, currentState.frame));
        ifNode.addIncomingData(pop2.value, pop1.value);

        final List<ControlFlow> results = new ArrayList<>();

        final GraphParserState origin = currentState.controlFlowsTo(ifNode).withFrame(pop2.newFrame);

        final Map<AbstractInsnNode, EdgeType> edges = incomingEdgesPerLabel.get(node.label);

        // True-Case
        final Region trueTargetNode = getOrCreateRegionNodeFor(node.label);
        if (EdgeType.BACK == edges.get(node)) {
            final Projection p = new StandardProjections.TrueProjection(EdgeType.BACK);
            final GraphParserState afterTrueCopy = introduceCopyInstructionsForBackEdge(origin, p, node.label);
            if (afterTrueCopy.lastControlTokenConsumer == ifNode) {
                // No copy instruction created
                afterTrueCopy.lastControlTokenConsumer.addControlFlowTo(new StandardProjections.TrueProjection(EdgeType.BACK), trueTargetNode);
            } else {
                afterTrueCopy.lastControlTokenConsumer.addControlFlowTo(StandardProjections.DEFAULT_BACKWARD, trueTargetNode);
            }
            // Do nothing
        } else {
            final Projection p = new StandardProjections.TrueProjection(EdgeType.FORWARD);
            graph.addFixup(new ForwardControlFlowFixup(origin.lastControlTokenConsumer, origin.frame, p, node.label));
            results.add(currentFlow.continueWith(node.label, origin));
        }

        // False-Case
        final AbstractInsnNode nextNode = node.getNext();
        if (nextNode instanceof LabelNode) {
            final LabelNode falseLabel = (LabelNode) nextNode;
            final Region falseTargetNode = getOrCreateRegionNodeFor(falseLabel);
            if (EdgeType.BACK == edges.get(node)) {
                // TODO: can this happen?
                final Projection p = new StandardProjections.FalseProjection(EdgeType.BACK);
                final GraphParserState afterFalseCopy = introduceCopyInstructionsForBackEdge(origin, p, falseLabel);
                if (afterFalseCopy.lastControlTokenConsumer == ifNode) {
                    // No Copy instructions created
                    afterFalseCopy.lastControlTokenConsumer.addControlFlowTo(new StandardProjections.FalseProjection(EdgeType.BACK), falseTargetNode);
                } else {
                    afterFalseCopy.lastControlTokenConsumer.addControlFlowTo(StandardProjections.DEFAULT_BACKWARD, falseTargetNode);
                }
                // Do nothing
            } else {
                final Projection p = new StandardProjections.FalseProjection(EdgeType.FORWARD);
                graph.addFixup(new ForwardControlFlowFixup(origin.lastControlTokenConsumer, origin.frame, p, falseLabel));
                results.add(currentFlow.continueWith(nextNode, origin));
            }
        } else {
            graph.addFixup(new ForwardControlFlowFixup(origin.lastControlTokenConsumer, origin.frame, new StandardProjections.FalseProjection(EdgeType.FORWARD), nextNode));
            results.add(currentFlow.continueWith(nextNode, origin));
        }

        return results;
    }

    private List<ControlFlow> parseJumpInsnNode(final ControlFlow currentFlow, final Map<LabelNode, Map<AbstractInsnNode, EdgeType>> incomingEdgesPerLabel) {
        switch (currentFlow.currentNode.getOpcode()) {
            case Opcodes.GOTO:
                return parse_GOTO(currentFlow, incomingEdgesPerLabel);
            case Opcodes.IF_ICMPGE:
                return parse_IF(currentFlow, If.Operation.icmpge, incomingEdgesPerLabel);
            default:
                throw new IllegalStateException("Not supported opcode : " + currentFlow.currentNode.getOpcode());
        }
    }

    private List<ControlFlow> parseIincInsnNode(final ControlFlow currentFlow) {
        final IincInsnNode node = (IincInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final int local = node.var;
        final Node currentValue = currentState.frame.incomingLocals[local];
        final Node amountNode = graph.newIntNode(node.incr);

        final Node addNode = graph.newAdd(Type.INT_TYPE);
        addNode.addIncomingData(currentValue, amountNode);

        final Variable variable = graph.newVariable(Type.INT_TYPE);

        final Copy copy = graph.newCopy(Type.INT_TYPE);
        copy.addIncomingData(addNode);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = currentState.frame.setLocal(local, variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ForwardControlFlowFixup(copy, newState.frame, StandardProjections.DEFAULT_FORWARD, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse(final ControlFlow currentFlow, final Map<LabelNode, Map<AbstractInsnNode, EdgeType>> incomingEdgesPerLabel) {
        if (currentFlow.currentNode instanceof LabelNode) {
            final LabelNode labelNode = (LabelNode) currentFlow.currentNode;

            final Map<AbstractInsnNode, EdgeType> incomingEdges = incomingEdgesPerLabel.get(labelNode);
            if (incomingEdges != null) {
                if (incomingEdges.size() > 1) {
                    // We do have multiple incoming edges
                    // Now we have to make sure that every entry on the stack or on local variables
                    // is a PHI. Data copy is handled during the graph fixup phase
                    // We just have to make sure that there are phi variables there to handle everything

                    final GraphParserState graphParserState = currentFlow.graphParserState;
                    final Value[] newLocals = new Value[graphParserState.frame.incomingLocals.length];
                    final Value[] newStack = new Value[graphParserState.frame.incomingStack.length];

                    for (int i = 0; i < graphParserState.frame.incomingLocals.length; i++) {
                        final Value source = graphParserState.frame.incomingLocals[i];
                        if (source != null && !(source instanceof PHI)) {
                            newLocals[i] = graph.newPHI(source.type);
                        } else {
                            newLocals[i] = source;
                        }
                    }
                    for (int i = 0; i < graphParserState.frame.incomingStack.length; i++) {
                        final Value source = graphParserState.frame.incomingStack[i];
                        if (source != null && !(source instanceof PHI)) {
                            newStack[i] = graph.newPHI(source.type);
                        } else {
                            newStack[i] = source;
                        }
                    }

                    final Frame newFrame = graphParserState.frame.withLocalsAndStack(newLocals, newStack);

                    final GraphParserState newState = graphParserState.withFrame(newFrame);
                    final ControlFlow mergedFlow = currentFlow.continueWith(newState);
                    return parseLabelNode(mergedFlow);
                }
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
