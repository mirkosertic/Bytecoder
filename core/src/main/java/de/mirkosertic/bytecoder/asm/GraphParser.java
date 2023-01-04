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
import org.objectweb.asm.tree.TypeInsnNode;
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

    private final CompileUnit compileUnit;

    private final AnalysisStack analysisStack;

    public GraphParser(final CompileUnit compileUnit, final Type ownerType, final MethodNode methodNode, final AnalysisStack analysisStack) {
        this.methodNode = methodNode;
        this.graph = new Graph();
        this.compileUnit = compileUnit;
        this.analysisStack = analysisStack;

        parse(ownerType);
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

    private void parse(final Type ownerType) {
        // Construct the initial parsing state
        final Value[] initialStack = new Value[0];
        final Value[] initialLocals = new Value[methodNode.maxLocals];

        final Type methodType = Type.getMethodType(methodNode.desc);

        int localIndex = 0;
        // TODO: Set correct type for this
        if ((methodNode.access & Opcodes.ACC_STATIC) == 0) {
            initialLocals[localIndex++] = graph.newThis(ownerType);
        }
        for (int i = 0; i < methodType.getArgumentTypes().length; i ++)  {
            final Type t = methodType.getArgumentTypes()[i];
            initialLocals[localIndex] = graph.newMethodArgument(t, i);
            localIndex+= t.getSize();
        }

        final Frame startFrame = new Frame(initialLocals, initialStack);
        final Region startRegion = (Region) graph.register(new Region(Graph.START_REGION_NAME));
        startRegion.frame = startFrame;

        final GraphParserState initialState = new GraphParserState(startFrame, startRegion, -1, new TryCatchGuardStackEntry[0]);

        // Step 1: We collect all jump targets
        final Map<AbstractInsnNode, Map<AbstractInsnNode, EdgeType>> incomingEdgesPerInstruction = new HashMap<>();

        // Step 2: Check for back edges
        final Stack<ControlFlow> controlFlowsToCheck = new Stack<>();
        controlFlowsToCheck.push(new ControlFlow(methodNode.instructions.get(0), initialState));
        while (!controlFlowsToCheck.isEmpty()) {
            final ControlFlow flow = controlFlowsToCheck.pop();
            AbstractInsnNode next = flow.currentNode.getNext();
            while ((next instanceof LineNumberNode) || (next instanceof FrameNode)) {
                next = next.getNext();
            }

            if (flow.currentNode.getOpcode() == Opcodes.RETURN) {
                // Stop parsing here
                continue;
            }
            if (flow.currentNode.getOpcode() == Opcodes.ATHROW) {
                // Stop parsing here
                continue;
            }
            if ((flow.currentNode instanceof LineNumberNode) || (flow.currentNode instanceof FrameNode)) {
                controlFlowsToCheck.push(flow.addInstructionAndContinueWith(next, next));
            } else if (flow.currentNode instanceof LabelNode) {
                final LabelNode labelNode = (LabelNode) flow.currentNode;
                 if (next != null) {
                     controlFlowsToCheck.push(flow.addInstructionAndContinueWith(labelNode, next));
                     final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(next, k -> new HashMap<>());
                     jumps.put(flow.currentNode, EdgeType.FORWARD);
                 }
                 for (final TryCatchBlockNode tryCatchBlockNode : methodNode.tryCatchBlocks) {
                     if (tryCatchBlockNode.start == labelNode) {
                         controlFlowsToCheck.push(flow.addInstructionAndContinueWith(labelNode, tryCatchBlockNode.handler));
                     }
                 }
            } else if (flow.currentNode instanceof JumpInsnNode) {
                final JumpInsnNode jump = (JumpInsnNode) flow.currentNode;
                switch (jump.getOpcode()) {
                    case Opcodes.GOTO: {
                        if (flow.visited(jump.label)) {
                            // Back-Edge
                            final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(jump.label, k -> new HashMap<>());
                            jumps.put(jump, EdgeType.BACK);
                        } else {
                            final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(jump.label, k -> new HashMap<>());
                            jumps.put(jump, EdgeType.FORWARD);
                            controlFlowsToCheck.push(flow.addInstructionAndContinueWith(jump.label, jump.label));
                        }
                        break;
                    }
                    case Opcodes.IF_ICMPGE:
                    case Opcodes.IF_ICMPEQ:
                    case Opcodes.IF_ICMPNE:
                    case Opcodes.IF_ICMPLT:
                    case Opcodes.IF_ICMPGT:
                    case Opcodes.IF_ICMPLE: {
                        if (flow.visited(jump.label)) {
                            // Back-Edge
                            final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(jump.label, k -> new HashMap<>());
                            jumps.put(jump, EdgeType.BACK);
                        } else {
                            final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(jump.label, k -> new HashMap<>());
                            jumps.put(jump, EdgeType.FORWARD);
                            controlFlowsToCheck.push(flow.addInstructionAndContinueWith(jump.label, jump.label));
                        }

                        if (flow.visited(next)) {
                            // Back-Edge
                            final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(next, k -> new HashMap<>());
                            jumps.put(jump, EdgeType.BACK);
                        } else {
                            final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(next, k -> new HashMap<>());
                            jumps.put(jump, EdgeType.FORWARD);

                            controlFlowsToCheck.push(flow.addInstructionAndContinueWith(next, next));
                        }
                        break;
                    }
                    default:
                        throw new IllegalStateException("Not implemented jump : " + jump.getOpcode());
                }
            } else {
                if (next != null) {
                    final Map<AbstractInsnNode, EdgeType> jumps = incomingEdgesPerInstruction.computeIfAbsent(next, k -> new HashMap<>());
                    jumps.put(flow.currentNode, EdgeType.FORWARD);
                    controlFlowsToCheck.push(flow.addInstructionAndContinueWith(next, next));
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
            if (alreadyVisited.add(flow.currentNode)) {
                for (final ControlFlow nextOutCome : parse(flow, incomingEdgesPerInstruction)) {
                    if (!alreadyVisited.contains(nextOutCome.currentNode)) {
                        controlFlowsToCheck.push(nextOutCome);
                    }
                }
            }
        }

        // Step 4: Fix the initial control flow
        startRegion.addControlFlowTo(StandardProjections.DEFAULT,
                graph.translationFor(methodNode.instructions.getFirst()).main);


        // Step 5: Fixup stuff not possible during analysis
        graph.applyFixups(incomingEdgesPerInstruction);
    }

    private List<ControlFlow> parseLabelNode(final ControlFlow currentFlow) {
        final LabelNode node = (LabelNode) currentFlow.currentNode;

        final Region region = getOrCreateRegionNodeFor(node);
        region.frame = currentFlow.graphParserState.frame;
        graph.registerTranslation(node, new InstructionTranslation(region, region.frame));

        final GraphParserState state = currentFlow.graphParserState;
        final List<ControlFlow> flowsToCheck = new ArrayList<>();
        if (node.getNext() != null) {
            if (region instanceof TryCatch) {
                LabelNode endNode = null;
                for (final TryCatchBlockNode tryCatchBlockNode : methodNode.tryCatchBlocks) {
                    if (tryCatchBlockNode.start == node) {
                        if (endNode == null) {
                            endNode = tryCatchBlockNode.end;
                        } else {
                            if (endNode != tryCatchBlockNode.end) {
                                throw new IllegalStateException("All try catch regions must have the same end for a given label!");
                            }
                        }
                    }
                }
                final TryCatchGuardStackEntry tryCatchGuardStackEntry = new TryCatchGuardStackEntry((TryCatch) region, node, endNode);
                final GraphParserState nextState = state.withNewTryCatchOnStack(tryCatchGuardStackEntry);

                graph.addFixup(new ControlFlowFixup(node, nextState.frame, StandardProjections.TRYCATCHGUARD, node.getNext()));
                flowsToCheck.add(currentFlow.continueWith(node.getNext(), nextState));

            } else {

                GraphParserState nextState = state;
                if (nextState.isEndOfTryCatchGuardBlock(node)) {
                    final TryCatchGuardStackEntry topEntry = nextState.tryCatchGuardStack[nextState.tryCatchGuardStack.length - 1];
                    topEntry.tryCatch.addControlFlowTo(StandardProjections.TRYCATCHEXIT, region);
                    nextState = nextState.popLatestTryBatchGuardBlock();
                }

                graph.addFixup(new ControlFlowFixup(node, nextState.frame, StandardProjections.DEFAULT, node.getNext()));
                flowsToCheck.add(currentFlow.continueWith(node.getNext(), nextState));
            }
        }

        if (region instanceof TryCatch) {
            // Check for exceptional flows
            for (final TryCatchBlockNode tryCatchBlockNode : methodNode.tryCatchBlocks) {
                if (tryCatchBlockNode.start == node) {

                    final Type exceptionType =
                            tryCatchBlockNode.type != null ? Type.getObjectType(tryCatchBlockNode.type) : Type.getType(Throwable.class);

                    final Region startRegion = getOrCreateRegionNodeFor(tryCatchBlockNode.handler);
                    final Frame frameWithPushedException = state.frame.pushToStack(graph.newCaughtException(exceptionType));
                    region.addControlFlowTo(new Projection.ExceptionHandler(exceptionType), startRegion);
                    flowsToCheck.add(currentFlow.continueWith(tryCatchBlockNode.handler, state.withFrame(frameWithPushedException)));
                }
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
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

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
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

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
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

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
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

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
        final Type targetClass = Type.getObjectType(node.owner);
        final Type[] argumentTypes = methodType.getArgumentTypes();
        final Node[] incomingData = new Node[argumentTypes.length + 1];

        final ResolvedClass rc = compileUnit.resolveClass(targetClass, analysisStack);
        final ResolvedMethod rm = rc.resolveMethod(node.name, methodType, analysisStack);

        Frame.PopResult latest = currentState.frame.popFromStack();
        incomingData[0] = latest.value;
        for (int i = 0; i < argumentTypes.length; i++) {
            latest = currentState.frame.popFromStack();
            incomingData[i + 1] = latest.value;
        }

        final GraphParserState newState;

        if (methodType.getReturnType().equals(Type.VOID_TYPE)) {

            final InstanceMethodInvocation n = graph.newInstanceMethodInvocation(node, rm);
            n.addIncomingData(incomingData);
            graph.registerTranslation(node, new InstructionTranslation(n, currentState.frame));

            newState = currentState.controlFlowsTo(n).withFrame(latest.newFrame);
            graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));
        } else {
            final InstanceMethodInvocationExpression n = graph.newInstanceMethodInvocationExpression(node, rm);
            n.addIncomingData(incomingData);

            final Variable var = graph.newVariable(methodType.getReturnType());
            final Copy copy = graph.newCopy(methodType.getReturnType());
            copy.addIncomingData(n);
            var.addIncomingData(copy);

            graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

            newState = currentState.controlFlowsTo(copy).withFrame(latest.newFrame.pushToStack(var));
            graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));
        }
        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_INVOKEVIRTUAL(final ControlFlow currentFlow) {
        final MethodInsnNode node = (MethodInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final Type methodType = Type.getMethodType(node.desc);
        final Type targetClass = Type.getObjectType(node.owner);
        final Type[] argumentTypes = methodType.getArgumentTypes();
        final Node[] incomingData = new Node[argumentTypes.length + 1];

        final ResolvedClass rc = compileUnit.resolveClass(targetClass, analysisStack);
        rc.resolveMethod(node.name, methodType, analysisStack);

        Frame.PopResult latest = currentState.frame.popFromStack();
        incomingData[0] = latest.value;
        for (int i = 0; i < argumentTypes.length; i++) {
            latest = currentState.frame.popFromStack();
            incomingData[i + 1] = latest.value;
        }

        final GraphParserState newState;
        if (methodType.getReturnType().equals(Type.VOID_TYPE)) {

            final VirtualMethodInvocation n = graph.newVirtualMethodInvocation(node);
            n.addIncomingData(incomingData);

            graph.registerTranslation(node, new InstructionTranslation(n, currentState.frame));

            newState = currentState.controlFlowsTo(n).withFrame(latest.newFrame);
            graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        } else {
            final VirtualMethodInvocationExpression n = graph.newVirtualMethodInvocationExpression(node);
            n.addIncomingData(incomingData);

            final Variable var = graph.newVariable(methodType.getReturnType());
            final Copy copy = graph.newCopy(methodType.getReturnType());
            copy.addIncomingData(n);
            var.addIncomingData(copy);

            graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

            newState = currentState.controlFlowsTo(copy).withFrame(latest.newFrame.pushToStack(var));
            graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));
        }

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_INVOKESTATIC(final ControlFlow currentFlow) {
        final MethodInsnNode node = (MethodInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final Type methodType = Type.getMethodType(node.desc);
        final Type targetClass = Type.getObjectType(node.owner);
        final Type[] argumentTypes = methodType.getArgumentTypes();
        final Node[] incomingData = new Node[argumentTypes.length];

        final ResolvedClass rc = compileUnit.resolveClass(targetClass, analysisStack);
        final ResolvedMethod rm = rc.resolveMethod(node.name, methodType, analysisStack);

        Frame latest = currentState.frame;
        for (int i = 0; i < argumentTypes.length; i++) {
            final Frame.PopResult popresult = latest.popFromStack();
            incomingData[i] = popresult.value;
            latest = popresult.newFrame;
        }

        final GraphParserState newState;

        if (methodType.getReturnType().equals(Type.VOID_TYPE)) {

            final StaticMethodInvocation n = graph.newStaticMethodInvocation(node, rm);
            n.addIncomingData(incomingData);
            graph.registerTranslation(node, new InstructionTranslation(n, currentState.frame));

            newState = currentState.controlFlowsTo(n).withFrame(latest);
            graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        } else {

            final StaticMethodInvocationExpression n = graph.newStaticMethodInvocationExpression(node, rm);
            n.addIncomingData(incomingData);

            final Variable var = graph.newVariable(methodType.getReturnType());
            final Copy copy = graph.newCopy(methodType.getReturnType());
            copy.addIncomingData(n);
            var.addIncomingData(copy);

            graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

            newState = currentState.controlFlowsTo(copy).withFrame(latest.pushToStack(var));
            graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        }

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parseMethodInsNode(final ControlFlow currentFlow) {
        final MethodInsnNode node = (MethodInsnNode) currentFlow.currentNode;
        switch (node.getOpcode()) {
            case Opcodes.INVOKESPECIAL:
                return parse_INVOKESPECIAL(currentFlow);
            case Opcodes.INVOKEVIRTUAL:
                return parse_INVOKEVIRTUAL(currentFlow);
            case Opcodes.INVOKESTATIC:
                return parse_INVOKESTATIC(currentFlow);
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
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

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
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

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

    private List<ControlFlow> parse_IRETURN(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;
        final ReturnPrimitive value = graph.newReturnPrimitive();
        final Frame.PopResult popResult = currentState.frame.popFromStack();
        value.addIncomingData(popResult.value);
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
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

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
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_ISUB(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop2 = currentState.frame.popFromStack();
        final Frame.PopResult pop1 = pop2.newFrame.popFromStack();

        final Node addNode = graph.newSub(Type.INT_TYPE);
        addNode.addIncomingData(pop1.value, pop2.value);

        final Variable variable = graph.newVariable(Type.INT_TYPE);
        final Copy copy = graph.newCopy(Type.INT_TYPE);
        copy.addIncomingData(addNode);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = pop1.newFrame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_ATHROW(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop1 = currentState.frame.popFromStack();
        final ControlTokenConsumer throwNode = graph.newUnwind(pop1.value.type);
        throwNode.addIncomingData(pop1.value);

        graph.registerTranslation(node, new InstructionTranslation(throwNode, currentState.frame));
        return Collections.emptyList();
    }

    private List<ControlFlow> parse_DUP(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop1 = currentState.frame.popFromStack();

        final Variable dest = graph.newVariable(pop1.value.type);
        final Copy c = graph.newCopy(pop1.value.type);
        c.addIncomingData(pop1.value);
        dest.addIncomingData(c);

        graph.registerTranslation(node, new InstructionTranslation(c, currentState.frame));

        final Frame newFrame = pop1.newFrame.pushToStack(pop1.value).pushToStack(dest);

        final GraphParserState newState = currentState.controlFlowsTo(c).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_POP(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop1 = currentState.frame.popFromStack();

        final Variable dest = graph.newVariable(pop1.value.type);
        final Copy c = graph.newCopy(pop1.value.type);
        c.addIncomingData(pop1.value);
        dest.addIncomingData(c);

        graph.registerTranslation(node, new InstructionTranslation(c, currentState.frame));

        final GraphParserState newState = currentState.controlFlowsTo(c).withFrame(pop1.newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

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
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parseInsnNode(final ControlFlow currentFlow) {
        final InsnNode node = (InsnNode) currentFlow.currentNode;
        switch (node.getOpcode()) {
            case Opcodes.RETURN:
                return parse_RETURN(currentFlow);
            case Opcodes.IRETURN:
                return parse_IRETURN(currentFlow);
            case Opcodes.ICONST_M1:
                return parse_ICONSTX(currentFlow, -1);
            case Opcodes.ICONST_0:
                return parse_ICONSTX(currentFlow, 0);
            case Opcodes.ICONST_1:
                return parse_ICONSTX(currentFlow, 1);
            case Opcodes.ICONST_2:
                return parse_ICONSTX(currentFlow, 2);
            case Opcodes.ICONST_3:
                return parse_ICONSTX(currentFlow, 3);
            case Opcodes.ICONST_4:
                return parse_ICONSTX(currentFlow, 4);
            case Opcodes.ICONST_5:
                return parse_ICONSTX(currentFlow, 5);
            case Opcodes.IADD:
                return parse_IADD(currentFlow);
            case Opcodes.IDIV:
                return parse_IDIV(currentFlow);
            case Opcodes.ISUB:
                return parse_ISUB(currentFlow);
            case Opcodes.ATHROW:
                return parse_ATHROW(currentFlow);
            case Opcodes.DUP:
                return parse_DUP(currentFlow);
            case Opcodes.POP:
                return parse_POP(currentFlow);
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

    private List<ControlFlow> parse_GOTO(final ControlFlow currentFlow) {
        final JumpInsnNode node = (JumpInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Goto gotoNode = graph.newGoto();

        graph.registerTranslation(node, new InstructionTranslation(gotoNode, currentState.frame));

        final Region target = graph.regionByLabel(node.label.getLabel().toString());
        graph.addFixup(new ControlFlowFixup(node, currentState.frame, StandardProjections.DEFAULT, node.label));
        return Collections.singletonList(currentFlow.continueWith(node.label, currentState.controlFlowsTo(target)));
    }

    private List<ControlFlow> parse_IF(final ControlFlow currentFlow, final If.Operation operation) {
        final JumpInsnNode node = (JumpInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Frame.PopResult pop1 = currentState.frame.popFromStack();
        final Frame.PopResult pop2 = pop1.newFrame.popFromStack();

        final If ifNode = graph.newIf(operation);
        graph.registerTranslation(node, new InstructionTranslation(ifNode, currentState.frame));
        ifNode.addIncomingData(pop2.value, pop1.value);

        final List<ControlFlow> results = new ArrayList<>();

        final GraphParserState origin = currentState.controlFlowsTo(ifNode).withFrame(pop2.newFrame);

        // True-Case
        graph.addFixup(new ControlFlowFixup(node, origin.frame, StandardProjections.TRUE, node.label));
        results.add(currentFlow.continueWith(node.label, origin));

        // False-Case
        final AbstractInsnNode nextNode = node.getNext();
        graph.addFixup(new ControlFlowFixup(node, origin.frame, StandardProjections.FALSE, nextNode));
        results.add(currentFlow.continueWith(nextNode, origin));

        return results;
    }

    private List<ControlFlow> parseJumpInsnNode(final ControlFlow currentFlow) {
        switch (currentFlow.currentNode.getOpcode()) {
            case Opcodes.GOTO:
                return parse_GOTO(currentFlow);
            case Opcodes.IF_ICMPEQ:
                return parse_IF(currentFlow, If.Operation.EQ);
            case Opcodes.IF_ICMPNE:
                return parse_IF(currentFlow, If.Operation.NE);
            case Opcodes.IF_ICMPLT:
                return parse_IF(currentFlow, If.Operation.LT);
            case Opcodes.IF_ICMPGE:
                return parse_IF(currentFlow, If.Operation.GE);
            case Opcodes.IF_ICMPGT:
                return parse_IF(currentFlow, If.Operation.GT);
            case Opcodes.IF_ICMPLE:
                return parse_IF(currentFlow, If.Operation.LE);
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
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parse_NEW(final ControlFlow currentFlow) {
        final TypeInsnNode node = (TypeInsnNode) currentFlow.currentNode;
        final GraphParserState currentState = currentFlow.graphParserState;

        final Type type = Type.getObjectType(node.desc);

        final TypeReference typeReference = graph.newTypeReference(type);
        final New n = graph.newNew(type);
        n.addIncomingData(typeReference);

        final Variable variable = graph.newVariable(type);

        final Copy copy = graph.newCopy(type);
        copy.addIncomingData(n);
        variable.addIncomingData(copy);
        graph.registerTranslation(node, new InstructionTranslation(copy, currentState.frame));

        final Frame newFrame = currentState.frame.pushToStack(variable);

        final GraphParserState newState = currentState.controlFlowsTo(copy).withFrame(newFrame);
        graph.addFixup(new ControlFlowFixup(node, newState.frame, StandardProjections.DEFAULT, node.getNext()));

        return Collections.singletonList(currentFlow.continueWith(node.getNext(), newState));
    }

    private List<ControlFlow> parseTypeInsnNode(final ControlFlow currentFlow) {
        switch (currentFlow.currentNode.getOpcode()) {
            case Opcodes.NEW:
                return parse_NEW(currentFlow);
            default:
                throw new IllegalStateException("Not supported opcode : " + currentFlow.currentNode.getOpcode());
        }
    }

    private List<ControlFlow> parse(final ControlFlow currentFlow, final Map<AbstractInsnNode, Map<AbstractInsnNode, EdgeType>> incomingEdgesPerInstruction) {
        if (currentFlow.currentNode instanceof LabelNode) {
            final LabelNode labelNode = (LabelNode) currentFlow.currentNode;

            final Map<AbstractInsnNode, EdgeType> incomingEdges = incomingEdgesPerInstruction.get(labelNode);
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
            return parseJumpInsnNode(currentFlow);
        }
        if (currentFlow.currentNode instanceof IincInsnNode) {
            return parseIincInsnNode(currentFlow);
        }
        if (currentFlow.currentNode instanceof TypeInsnNode) {
            return parseTypeInsnNode(currentFlow);
        }
        throw new IllegalStateException("Not implemented : " + currentFlow.currentNode + " -> " + currentFlow.currentNode.getOpcode());
    }

    public Graph graph() {
        return graph;
    }
}
