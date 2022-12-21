package de.mirkosertic.bytecoder.asm;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.ArrayList;
import java.util.List;

public class ControlFlow {
    final AbstractInsnNode currentNode;
    private final List<LabelNode> visitedLabels;

    final GraphParserState graphParserState;

    public ControlFlow(final AbstractInsnNode currentNode, final GraphParserState graphParserState) {
        this(currentNode, new ArrayList<>(), graphParserState);
    }
    ControlFlow(final AbstractInsnNode currentNode, final List<LabelNode> visitedLabels, final GraphParserState graphParserState) {
        this.currentNode = currentNode;
        this.visitedLabels = visitedLabels;
        this.graphParserState = graphParserState;
    }
    public boolean visited(final LabelNode node) {
        return visitedLabels.contains(node);
    }
    public ControlFlow addLabelAndContinueWith(LabelNode labelNode, AbstractInsnNode next) {
        final List<LabelNode> newVisited = new ArrayList<>(visitedLabels);
        newVisited.add(labelNode);
        return new ControlFlow(next, newVisited, graphParserState);
    }
    public ControlFlow continueWith(AbstractInsnNode next) {
        return new ControlFlow(next, visitedLabels, graphParserState);
    }
    public ControlFlow continueWith(AbstractInsnNode next, final GraphParserState newParserState) {
        return new ControlFlow(next, visitedLabels, newParserState);
    }
}
