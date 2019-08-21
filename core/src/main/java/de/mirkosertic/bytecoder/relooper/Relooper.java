/*
 * Copyright 2018 Mirko Sertic
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
package de.mirkosertic.bytecoder.relooper;

import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.core.BytecodeExceptionTableEntry;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;
import de.mirkosertic.bytecoder.graph.Edge;
import de.mirkosertic.bytecoder.ssa.BreakExpression;
import de.mirkosertic.bytecoder.ssa.ContinueExpression;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.ControlFlowEdgeType;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.Label;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Implementation of the Relooper Algorithm as described in Alon Zakai's Emscripten Paper.
 */
public class Relooper {

    public abstract static class Block {

        private final Set<RegionNode> entries;
        private final Label label;
        private int labelRequired;

        protected Block(final Set<RegionNode> aEntries, final String aLabelPrefix) {
            entries = aEntries;
            final StringBuilder theBuilder = new StringBuilder();
            for (final RegionNode aLabel : aEntries) {
                if (theBuilder.length() > 0) {
                    theBuilder.append("_");
                }
                theBuilder.append(aLabel.getStartAddress().getAddress());
            }
            labelRequired = 0;
            label = new Label(aLabelPrefix + theBuilder.toString());
        }

        public Label label() {
            return label;
        }

        public boolean isLabelRequired() {
            return labelRequired > 0;
        }

        public void requireLabel() {
            labelRequired++;
        }

        public List<RegionNode> entries() {
            final ArrayList<RegionNode> theResult = new ArrayList<>(entries);
            theResult.sort(Comparator.comparingInt(o -> o.getStartAddress().getAddress()));
            return theResult;
        }

        public abstract Block next();

        public abstract boolean containsMultipleBlock();
    }

    /**
     * Simple block: A block with
     * One Internal label, and
     * a Next block, which the internal label branches to.
     * The block is later translated simply into the code for
     * that label, and the Next block appears right after it.
     */
    public static class SimpleBlock extends Block {

        private final RegionNode internalLabel;
        private final Block next;
        private final ExpressionList expressionList;

        public SimpleBlock(final Set<RegionNode> aEntries, final RegionNode aInternalLabel, final Block aNext) {
            super(aEntries, "S_");
            internalLabel = aInternalLabel;
            next = aNext;
            expressionList = aInternalLabel.getExpressions().deepCopy();
        }

        public RegionNode internalLabel() {
            return internalLabel;
        }

        public ExpressionList expressions() {
            return expressionList;
        }

        @Override
        public Block next() {
            return next;
        }

        @Override
        public boolean containsMultipleBlock() {
            if (next != null) {
                return next.containsMultipleBlock();
            }
            return false;
        }
    }

    /**
     * Block for high level if/then/else language constructs.
     */
    public static class IFThenElseBlock extends Block {

        private final ExpressionList prelude;
        private final Value condition;
        private final Block trueBlock;
        private final Block falseBlock;
        private final Block nextBlock;

        public IFThenElseBlock(final ExpressionList aPrelude, final Set<RegionNode> aEntries, final Value condition,
                final Block trueBlock, final Block falseBlock, final Block nextBlock) {
            super(aEntries, "IF_");
            this.prelude = aPrelude;
            this.condition = condition;
            this.trueBlock = trueBlock;
            this.falseBlock = falseBlock;
            this.nextBlock = nextBlock;
        }

        public Value getCondition() {
            return condition;
        }

        public Block getTrueBlock() {
            return trueBlock;
        }

        public Block getFalseBlock() {
            return falseBlock;
        }

        @Override
        public Block next() {
            return nextBlock;
        }

        @Override
        public boolean containsMultipleBlock() {
            if (trueBlock != null && trueBlock.containsMultipleBlock()) {
                return true;
            }
            if (falseBlock != null && falseBlock.containsMultipleBlock()) {
                return true;
            }
            if (nextBlock != null) {
                return nextBlock.containsMultipleBlock();
            }
            return false;
        }

        public ExpressionList getPrelude() {
            return prelude;
        }
    }

    /**
     * A Try block guards its inner block for exceptions. Without exceptions, control
     * flow is continued in the next block.
     */
    public static class TryBlock extends Block {

        public static class CatchBlock {

            private final Set<BytecodeUtf8Constant> caughtExceptions;
            private final Block handler;

            public CatchBlock(final Set<BytecodeUtf8Constant> caughtExceptions, final Block handler) {
                this.caughtExceptions = caughtExceptions;
                this.handler = handler;
            }

            public Set<BytecodeUtf8Constant> getCaughtExceptions() {
                return caughtExceptions;
            }

            public Block getHandler() {
                return handler;
            }
        }

        private final Block inner;
        private final Block next;
        private final List<CatchBlock> catchBlocks;
        private final Block finallyBlock;

        public TryBlock(final Set<RegionNode> aEntries, final Block inner, final Block next,
                final List<CatchBlock> catchBlocks, final Block finallyBlock) {
            super(aEntries, "T_");
            this.inner = inner;
            this.next = next;
            this.catchBlocks = catchBlocks;
            this.finallyBlock = finallyBlock;
        }

        public Block inner() {
            return inner;
        }

        @Override
        public Block next() {
            return next;
        }

        @Override
        public boolean containsMultipleBlock() {
            if (inner.containsMultipleBlock()) {
                return true;
            }
            for (final CatchBlock theCatch : catchBlocks) {
                if (theCatch.handler.containsMultipleBlock()) {
                    return true;
                }
            }
            if (next != null) {
                if (next.containsMultipleBlock()) {
                    return true;
                }
            }
            if (finallyBlock != null) {
                return finallyBlock.containsMultipleBlock();
            }
            return false;
        }

        public List<CatchBlock> getCatchBlocks() {
            return catchBlocks;
        }

        public Block getFinallyBlock() {
            return finallyBlock;
        }
    }

    /**
     * Loop: A block that represents a basic loop, comprised of
     * two internal sub-blocks:
     * Inner: A block that will appear inside the loop, i.e.,
     * when execution reaches the end of that block, flow
     * will return to the beginning. Typically a loop will
     * contain a conditional break defining where it is exited.
     * When we exit, we reach the Next block, below.
     * Next: A block that will appear just outside the loop,
     * in other words, that will be reached when the loop is
     * exited.
     */
    public static class LoopBlock extends Block {
        private final Block inner;
        private final Block next;

        public LoopBlock(final Set<RegionNode> aEntries, final Block aInner, final Block aNext) {
            super(aEntries, "L_");
            inner = aInner;
            next = aNext;
        }

        public Block inner() {
            return inner;
        }

        @Override
        public Block next() {
            return next;
        }

        @Override
        public boolean containsMultipleBlock() {
            if (inner.containsMultipleBlock()) {
                return true;
            }
            if (next != null) {
                return next.containsMultipleBlock();
            }
            return false;
        }
    }

    /**
     * Multiple: A block that represents a divergence into several possible branches,
     * that eventually rejoin. A Multiple
     * block can implement an ‘if’, an ‘if-else’, a ‘switch’, etc.
     * It is comprised of:
     * Handled blocks: A set of blocks to which execution
     * can enter. When we reach the multiple block, we
     * check which of them should execute, and go there.
     * When execution of that block is complete, or if none
     * of the handled blocks was selected for execution, we
     * proceed to the Next block, below.
     * Next: A block that will appear just after the Handled
     * blocks, in other words, that will be reached after code
     * flow exits the Handled blocks.
     */
    public static class MultipleBlock extends Block {

        private final Set<Block> handlers;
        private final Block next;

        public MultipleBlock(final Set<RegionNode> aEntries, final Set<Block> aHandlers, final Block aNext) {
            super(aEntries, "M_");
            handlers = aHandlers;
            next = aNext;
        }

        public Set<Block> handlers() {
            return handlers;
        }

        @Override
        public Block next() {
            return next;
        }

        @Override
        public boolean containsMultipleBlock() {
            return true;
        }
    }

    private final CompileOptions compileOptions;

    public Relooper(final CompileOptions aCompileOptions) {
        compileOptions = aCompileOptions;
    }

    public Block reloop(final ControlFlowGraph aGraph) {
        final Set<RegionNode> theEntries = new HashSet<>();
        final RegionNode theStart = aGraph.startNode();
        theEntries.add(aGraph.startNode());

        // At this point, we use the dominated nodes of our start node as
        // the initisl "label-soup" for the Relooper. This will exclude
        // exception handler and finalize blocks from the the CFG and
        // will reduce the amount of labels to be processed(hopefully)
        final Block theBlock = reloop(aGraph, theEntries, theStart.dominatedNodes());

        // At this point, we have a constructed Relooper-CFG. Now we have to replace
        // all GOTOs with either corresponding break or continue statements.
        replaceGotosIn(theBlock);

        // We are done here
        return theBlock;
    }

    private void replaceGotosIn(final Block aBlock) {
        replaceGotosIn(new Stack<>(), aBlock);
    }

    private void replaceGotosIn(final Stack<Block> aTraversalStack, final Block aBlock) {
        if (aBlock == null) {
            return;
        }
        if (aBlock instanceof TryBlock) {
            final TryBlock theTry = (TryBlock) aBlock;
            aTraversalStack.push(theTry);
            replaceGotosIn(aTraversalStack, theTry.inner());

            if (theTry.finallyBlock != null) {
                replaceGotosIn(aTraversalStack, theTry.finallyBlock);
            }
            for (final TryBlock.CatchBlock theCatch : theTry.catchBlocks) {
                replaceGotosIn(aTraversalStack, theCatch.handler);
            }

            aTraversalStack.pop();
            replaceGotosIn(aTraversalStack, theTry.next());
            return;
        }
        if (aBlock instanceof SimpleBlock) {
            final SimpleBlock theSimple = (SimpleBlock) aBlock;

            aTraversalStack.push(theSimple);
            final RegionNode theInternalLabel = theSimple.internalLabel();
            replaceGotosIn(aTraversalStack, theSimple, theInternalLabel, theSimple.expressions());

            replaceGotosIn(aTraversalStack, theSimple.next());
            aTraversalStack.pop();

            final Expression theLastExpression = theSimple.expressions().lastExpression();
            // Breaks at the end of the internal label breaking out of the simple block
            // can be silent, they only need to set the __label__ variable
            if (theLastExpression instanceof BreakExpression) {
                final BreakExpression theBreak = (BreakExpression) theLastExpression;
                if (Objects.equals(theBreak.blockToBreak().name(), theSimple.label().name())) {
                    theBreak.silent();

                    // TODO: If there is only one successor
                    // with only one precessor, its label is not required
                }
            }

            // At this place, we check the goto labels for the blocks successors to make them available during rendering
            // We search the successor edge
            for (final Edge theEdge : theInternalLabel.outgoingEdges().collect(Collectors.toList())) {
                final RegionNode theTarget = (RegionNode) theEdge.targetNode();
                // We found the matching edge
                if (theEdge.edgeType() == ControlFlowEdgeType.forward) {
                    // We can only branch to the next block
                    // We search the whole hiararchy to find the right block to break out
                    for (int i=aTraversalStack.size() -1 ; i>= 0; i--) {
                        final Block theNestingBlock = aTraversalStack.get(i);
                        if (theNestingBlock.next() != null && theNestingBlock.next().entries().contains(theTarget)) {
                            theNestingBlock.requireLabel();
                            break;
                        } else if (theNestingBlock.entries().contains(theTarget)) {
                            theNestingBlock.requireLabel();
                            break;
                        }
                    }

                } else {
                    // We can only branch back into the known stack of nested blocks
                    for (final Block theNestingBlock : aTraversalStack) {
                        if (theNestingBlock.entries().contains(theTarget) && (theNestingBlock instanceof LoopBlock)) {
                            // We can return to the target in the hierarchy
                            theNestingBlock.requireLabel();
                            break;
                        }
                    }
                }
            }

            return;
        }
        if (aBlock instanceof LoopBlock) {
            final LoopBlock theLoop = (LoopBlock) aBlock;

            aTraversalStack.push(theLoop);
            replaceGotosIn(aTraversalStack, theLoop.inner());

            replaceGotosIn(aTraversalStack, theLoop.next());
            aTraversalStack.pop();
            return;
        }
        if (aBlock instanceof IFThenElseBlock) {
            final IFThenElseBlock theIf = (IFThenElseBlock) aBlock;

            aTraversalStack.push(theIf);
            replaceGotosIn(aTraversalStack, theIf.getTrueBlock());
            replaceGotosIn(aTraversalStack, theIf.getFalseBlock());
            replaceGotosIn(aTraversalStack, theIf.next());
            aTraversalStack.pop();
            return;
        }

        if (aBlock instanceof MultipleBlock) {
            final MultipleBlock theMultiple = (MultipleBlock) aBlock;
            aTraversalStack.push(theMultiple);
            for (final Block theHandler : theMultiple.handlers()) {
                replaceGotosIn(aTraversalStack, theHandler);
            }

            replaceGotosIn(aTraversalStack, theMultiple.next());
            aTraversalStack.pop();
            return;
        }

        throw new IllegalStateException("Don't know how to handle " + aBlock);
    }

    private void replaceGotosIn(final Stack<Block> aTraversalStack, final SimpleBlock aCurrent, final RegionNode aLabel, final ExpressionList aList) {
        for (final Expression theExpression : aList.toList()) {
            if (theExpression instanceof ExpressionListContainer) {
                final ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (final ExpressionList theList : theContainer.getExpressionLists()) {
                    replaceGotosIn(aTraversalStack, aCurrent, aLabel, theList);
                }
            }
            if (theExpression instanceof GotoExpression) {
                final GotoExpression theGoto = (GotoExpression) theExpression;
                boolean theGotoFound = false;

                // We search the successor edge
                for (final Edge theEdge : aLabel.outgoingEdges().collect(Collectors.toList())) {
                    final RegionNode theTarget = (RegionNode) theEdge.targetNode();
                    if (Objects.equals(theTarget.getStartAddress(), theGoto.jumpTarget())) {
                        theGotoFound = true;
                        // We found the matching edge
                        if (theEdge.edgeType() == ControlFlowEdgeType.forward) {
                            // We can only branch to the next block
                            // We search the whole hiararchy to find the right block to break out
                            guard: {
                                for (int i = aTraversalStack.size() - 1; i >= 0; i--) {
                                    final Block theNestingBlock = aTraversalStack.get(i);
                                    if (theNestingBlock.next() != null && theNestingBlock.next().entries().contains(theTarget)) {
                                        theNestingBlock.requireLabel();
                                        final BreakExpression theBreak = new BreakExpression(theGoto.getProgram(), theGoto.getAddress(), theNestingBlock.label(), theTarget.getStartAddress());
                                        aList.replace(theGoto, theBreak);

                                        if (theNestingBlock.next() instanceof SimpleBlock && theNestingBlock.next().entries().size() == 1) {
                                            theBreak.noSetRequired();
                                        }

                                        break guard;
                                    } else if (theNestingBlock.entries().contains(theTarget)) {
                                        theNestingBlock.requireLabel();
                                        final ContinueExpression theContinue = new ContinueExpression(theGoto.getProgram(), theGoto.getAddress(), theNestingBlock.label(), theTarget.getStartAddress());
                                        aList.replace(theGoto, theContinue);

                                        break guard;
                                    }
                                }

                                throw new IllegalStateException("Failed to jump to " + theTarget.getStartAddress().getAddress() + " from " + aCurrent.label().name() + " : no matching entry found!");
                            }

                        } else {
                            // We can only branch back into the known stack of nested blocks
                            boolean theSomethingFound = false;
                            for (final Block theNestingBlock : aTraversalStack) {
                                if (theNestingBlock.entries().contains(theTarget) && (theNestingBlock instanceof LoopBlock)) {
                                    theSomethingFound = true;

                                    // We can return to the target in the hierarchy
                                    theNestingBlock.requireLabel();
                                    final ContinueExpression theContinue = new ContinueExpression(theGoto.getProgram(), theGoto.getAddress(), theNestingBlock.label(), theTarget.getStartAddress());
                                    aList.replace(theGoto, theContinue);
                                    break;
                                }
                            }
                            if (!theSomethingFound) {
                                throw new IllegalStateException(
                                        "No back edge target from " + aLabel.getStartAddress().getAddress() + " to " + theTarget.getStartAddress().getAddress());
                            }
                        }
                    }
                }
                if (!theGotoFound) {
                    throw new IllegalStateException("No GOTO possible for " + theGoto.jumpTarget().getAddress() + " in label " + aCurrent.label().name());
                }
            }
        }
    }

    /**
     * Receive a set of labels and which of them are entry
     * points. We wish to create a block comprised of all those
     * labels.
     */
    private Block reloop(final ControlFlowGraph aGraph, final Set<RegionNode> aEntryLabels, final Set<RegionNode> aLabelSoup) {

        // If there are no entry labels at all, we return null.
        // This will become the next value of the predecessor block and will mark the end of the
        // invocation chain.
        if (aEntryLabels.isEmpty()) {
            return null;
        }

        final Collection<RegionNode> theJumptargets = jumpTargetsOf(aLabelSoup);

        // If we have a single entry, and cannot return to it (by
        // some other label later on branching to it) then create a
        // Simple block, with the entry as its internal label, and the
        // Next block comprised of all the other labels. The entries
        // for the Next block are the entries to which the internal
        // label can branch.
        if (aEntryLabels.size() == 1) {
            final RegionNode theEntry = aEntryLabels.iterator().next();
            if (!theJumptargets.contains(theEntry)) {
                return createSimpleBlock(aGraph, aEntryLabels, aLabelSoup, theEntry);
            }
        }

        // If we can return to all of the entries, create a Loop
        // block, whose Inner block is comprised of all labels that
        // can reach one of the entries, and whose Next block is
        // comprised of all the others. The entry labels for the current
        // block become entry labels for the Inner block (note
        // that they must be in the Inner block by definition, as each
        // one can reach itself). The Next block’s entry labels are
        // all the labels in the Next block that can be reached by the
        // Inner block.
        if (theJumptargets.containsAll(aEntryLabels)) {
            // Here we have to distinguish two cases
            // If we have a single entry, we create a loop block with a
            // SimpleBlock as its loop body and a next block with a block
            // the loop body can branch out to.
            if (aEntryLabels.size() == 1) {
                final RegionNode theSingleEntry = aEntryLabels.iterator().next();

                final Set<RegionNode> theInternalLabels = theSingleEntry.dominatedNodes().stream().filter(aLabelSoup::contains).collect(
                        Collectors.toSet());
                final Set<RegionNode> theRestLabels = new HashSet<>(aLabelSoup);
                theRestLabels.removeAll(theInternalLabels);

                // Search for branch-outs of the current loop
                final Set<RegionNode> theRestEntries = new HashSet<>();
                for (final RegionNode theReachable : theSingleEntry.dominatedNodes()) {
                    theReachable.outgoingEdges().filter(t -> t.edgeType() == ControlFlowEdgeType.forward).forEach(edge -> {
                        if (theRestLabels.contains(edge.targetNode())) {
                            theRestEntries.add(edge.targetNode());
                        }
                    });
                }

                final Block theInternalBlock =
                        createSimpleBlock(aGraph, aEntryLabels, theInternalLabels, theSingleEntry);
                final Block theNextBlock = reloop(aGraph, theRestEntries, theRestLabels);

                return new LoopBlock(aEntryLabels, theInternalBlock, theNextBlock);
            }
        }

        // If we have more than one entry, try to create a Multiple block:
        // For each entry, find all the labels it reaches that
        // cannot be reached by any other entry. If at least one entry
        // has such labels, return a Multiple block, whose Handled
        // blocks are blocks for those labels (and whose entries are
        // those labels), and whose Next block is all the rest. Entries
        // for the next block are entries that did not become part of
        // the Handled blocks, and also labels that can be reached
        // from the Handled blocks.
        if (aEntryLabels.size() > 1) {
            final Set<RegionNode> theRest = new HashSet<>(aLabelSoup);
            final Set<RegionNode> theRestEntries = new HashSet<>();
            final Map<RegionNode, Set<RegionNode>> theEntryReaches = new HashMap<>();

            final Set<Block> theHandlers = new HashSet<>();
            for (final RegionNode theEntry : aEntryLabels) {
                final Set<RegionNode> theDominated = theEntry.dominatedNodes().stream().filter(aLabelSoup::contains).collect(
                        Collectors.toSet());
                theEntryReaches.put(theEntry, theDominated);
                theRest.removeAll(theDominated);

                final Set<RegionNode> theHandlerEntries = new HashSet<>();
                theHandlerEntries.add(theEntry);
                theHandlers.add(reloop(aGraph, theHandlerEntries, theDominated));
            }

            for (final Map.Entry<RegionNode, Set<RegionNode>> theHandler : theEntryReaches.entrySet()) {
                for (final RegionNode theJumpTarget : allForwardJumpTargetsOf(theHandler.getValue())) {
                    if (theRest.contains(theJumpTarget)) {
                        theRestEntries.add(theJumpTarget);
                    }
                }
            }

            final Block theNext = reloop(aGraph, theRestEntries, theRest);
            return new MultipleBlock(aEntryLabels, theHandlers, theNext);
        }

        // If we could not create a Multiple block, then create
        // a Loop block as described above
        throw new IllegalStateException("What do do now?");
    }

    private Block createSimpleBlock(final ControlFlowGraph aGraph, final Set<RegionNode> aEntryLabels, final Set<RegionNode> aLabelSoup,
                                    final RegionNode aEntry) {

        if (compileOptions.isEnableExceptions()) {

            final List<TryBlock.CatchBlock> catchBlocks = new ArrayList<>();
            Block finallyBlock = null;
            final List<RegionNode.ExceptionHandler> theHandlers = aGraph.exceptionHandlersStartingAt(aEntry.getStartAddress());
            if (!theHandlers.isEmpty()) {
                if (theHandlers.size() != 1) {
                    throw new IllegalStateException("Overlapping exception handling regions not yet supported!");
                }
                final RegionNode.ExceptionHandler theSingleHandler = theHandlers.get(0);

                // Ok, at this point we search for all nodes that are
                // Covered by this exception handler
                // They will become part of the inner block
                final Set<RegionNode> theInnerNodes = new HashSet<>();
                final Set<BytecodeOpcodeAddress> theIncludesHandlers = theSingleHandler.getCatchEntries().stream().map(BytecodeExceptionTableEntry::getHandlerPc).collect(
                        Collectors.toSet());

                // We collect the known finally
                // handlers here
                final Set<BytecodeOpcodeAddress> theFinallyHandlers = new HashSet<>();
                for (final BytecodeExceptionTableEntry theEntry : aGraph.getProgram().getFlowInformation().getProgram().getExceptionHandlers()) {
                    if (theIncludesHandlers.contains(theEntry.getHandlerPc())) {
                        for (final RegionNode theInner : aLabelSoup) {
                            if (theEntry.coveres(theInner.getStartAddress())) {
                                theInnerNodes.add(theInner);
                            }
                        }
                    } else {
                        if (theEntry.isFinally() && theEntry.getStartPC().equals(aEntry.getStartAddress())) {
                            theFinallyHandlers.add(theEntry.getHandlerPc());
                        }
                    }
                }

                // All of the label soup except the inner nodes will become part of the next block
                final Set<RegionNode> theNextNodes = new HashSet<>(aLabelSoup);
                theNextNodes.removeAll(theInnerNodes);

                // Now we have to search for the entry nodes of the next block
                final HashSet<RegionNode> theNextEntries = new HashSet<>();
                for (final RegionNode theInner : theInnerNodes) {
                    for (final Edge theEdge : theInner.outgoingEdges().collect(Collectors.toList())) {
                        if (theNextNodes.contains(theEdge.targetNode())) {
                            theNextEntries.add((RegionNode) theEdge.targetNode());
                        }
                    }
                }

                // Ok, now we compute the inner block of our try block
                theInnerNodes.remove(aEntry);
                final HashSet<RegionNode> theInnerEntries = new HashSet<>();
                for (final Edge theEdge : aEntry.outgoingEdges().collect(Collectors.toList())) {
                    if (theInnerNodes.contains(theEdge.targetNode())) {
                        theInnerEntries.add((RegionNode) theEdge.targetNode());
                    }
                }

                final Block theTryInner = new SimpleBlock(theInnerEntries, aEntry, reloop(aGraph, theInnerEntries, theInnerNodes));

                // We finally need to compute the catchBlocks
                final Map<BytecodeOpcodeAddress, Set<BytecodeUtf8Constant>> theAssignedHandlers = new HashMap<>();
                for (final BytecodeExceptionTableEntry theEntry : theSingleHandler.getCatchEntries()) {
                    if (!theEntry.isFinally()) {
                        final Set<BytecodeUtf8Constant> theCatchTypes = theAssignedHandlers
                                .computeIfAbsent(theEntry.getHandlerPc(), k -> new HashSet<>());
                        theCatchTypes.add(theEntry.getCatchType().getConstant());
                    }
                }

                for (final Map.Entry<BytecodeOpcodeAddress, Set<BytecodeUtf8Constant>> theEntry : theAssignedHandlers.entrySet()) {
                    final Set<RegionNode> theEntries = new HashSet<>();
                    final RegionNode theHandlerStart = aGraph.nodeStartingAt(theEntry.getKey());
                    theEntries.add(theHandlerStart);

                    final Set<RegionNode> theTagSoup = theHandlerStart.dominatedNodes();
                    // Dominated exception handler nodes are covered here
                    // Hence they cannot be part of the next block
                    theNextNodes.remove(theHandlerStart);
                    theNextEntries.remove(theHandlerStart);
                    theNextNodes.removeAll(theTagSoup);
                    theNextEntries.removeAll(theTagSoup);

                    // If a catch or finally block branches out to the next block,
                    // This branch will also become an entry
                    for (final RegionNode theInner : theTagSoup) {
                        for (final Edge theEdge : theInner.outgoingEdges().collect(Collectors.toList())) {
                            if (theNextNodes.contains(theEdge.targetNode())) {
                                theNextEntries.add((RegionNode) theEdge.targetNode());
                            }
                        }
                    }

                    final TryBlock.CatchBlock theCatchBlock = new TryBlock.CatchBlock(theEntry.getValue(),
                            reloop(aGraph, theEntries, theTagSoup));

                    catchBlocks.add(theCatchBlock);
                }

                // At this point, we have to check the finally blocks
                if (!theFinallyHandlers.isEmpty()) {
                    if (theFinallyHandlers.size() != 1) {
                        throw new IllegalStateException(
                                "More than one finally handler found ! Size = " + theFinallyHandlers.size());
                    }
                    final RegionNode theFinallyNode = aGraph.nodeStartingAt(new ArrayList<>(theFinallyHandlers).get(0));
                    final Set<RegionNode> theEntries = new HashSet<>();
                    theEntries.add(theFinallyNode);

                    final Set<RegionNode> theDominated = theFinallyNode.dominatedNodes();

                    finallyBlock = reloop(aGraph, theEntries, theDominated);

                    // The finally nodes cannot be reached anymore
                    theNextEntries.remove(theFinallyNode);
                    theNextNodes.remove(theFinallyNode);
                    theNextNodes.removeAll(theDominated);
                }

                // And of course the next block
                final Block theTryNext;
                if (theNextNodes.isEmpty()) {
                    theTryNext = null;
                } else {
                    theTryNext = reloop(aGraph, theNextEntries, theNextNodes);
                }

                Collections.reverse(catchBlocks);
                return new TryBlock(aEntryLabels, theTryInner, theTryNext, catchBlocks, finallyBlock);
            }
        }

        // Search for conditional Jumps
        final List<Expression> theExpressions = aEntry.getExpressions().toList();
        for (int i = 0; i < theExpressions.size(); i++) {
            final Expression theExpression = theExpressions.get(i);
            if (theExpression instanceof IFExpression) {
                final IFExpression theIf = (IFExpression) theExpression;
                // Ok, we found a condition
                if (i < theExpressions.size() - 1) {
                    // Were are following expressions
                    final Expression theLast = theExpressions.get(theExpressions.size() - 1);
                    if (theLast instanceof GotoExpression) {
                        final GotoExpression theGoto = (GotoExpression) theLast;
                        final RegionNode theTrueBranch = aGraph.nodeStartingAt(theIf.getGotoAddress());
                        final RegionNode theFalseBranch = aGraph.nodeStartingAt(theGoto.jumpTarget());
                        if (theTrueBranch.isImmediatelyDominatedBy(aEntry) && theFalseBranch.isImmediatelyDominatedBy(aEntry)) {
                            // We have a candidate!!
                            final Value theCondition = theIf.incomingDataFlows().get(0);

                            final Set<RegionNode> theNextEntries = new HashSet<>();
                            final Set<RegionNode> theNextTagSoup = new HashSet<>(aLabelSoup);
                            final Set<RegionNode> theTrueDominated = theTrueBranch.dominatedNodes();
                            final Set<RegionNode> theFalseDominated = theFalseBranch.dominatedNodes();
                            theNextTagSoup.removeAll(theTrueDominated);
                            theNextTagSoup.removeAll(theFalseDominated);
                            theNextTagSoup.remove(aEntry);

                            // Calculate the entries for the next block
                            for (final RegionNode theTrue : theTrueDominated) {
                                for (final Edge theEdge : theTrue.outgoingEdges().collect(Collectors.toList())) {
                                    final RegionNode theNode = (RegionNode) theEdge.targetNode();
                                    if (theEdge.edgeType() == ControlFlowEdgeType.forward && theNextTagSoup.contains(theNode)) {
                                        theNextEntries.add(theNode);
                                    }
                                }
                            }

                            for (final RegionNode theFalse : theFalseDominated) {
                                for (final Edge theEdge : theFalse.outgoingEdges().collect(Collectors.toList())) {
                                    final RegionNode theNode = (RegionNode) theEdge.targetNode();
                                    if (theEdge.edgeType() == ControlFlowEdgeType.forward && theNextTagSoup.contains(theNode)) {
                                        theNextEntries.add(theNode);
                                    }
                                }
                            }

                            final Block theTrueBranchBlock = reloop(aGraph, Collections.singleton(theTrueBranch),
                                    theTrueDominated);
                            final Block theFalseBranchBlock = reloop(aGraph, Collections.singleton(theFalseBranch),
                                    theFalseDominated);

                            final ExpressionList thePrelude = new ExpressionList();
                            for (int j = 0; j < i; j++) {
                                thePrelude.add(theExpressions.get(j));
                            }

                            Block theNextBlock = null;
                            if (!theNextEntries.isEmpty()) {
                                theNextBlock = reloop(aGraph, theNextEntries, theNextTagSoup);
                            }

                            return new IFThenElseBlock(thePrelude, Collections.singleton(aEntry), theCondition,
                                    theTrueBranchBlock, theFalseBranchBlock, theNextBlock);
                        } else if (theTrueBranch.isImmediatelyDominatedBy(aEntry)) {

                            // TODO:

                        } else if (theFalseBranch.isImmediatelyDominatedBy(aEntry)) {

                            // TODO:
                        }
                    }
                }
            }
        }

        final Set<RegionNode> theNextEntries = new HashSet<>();
        final Set<RegionNode> theDominated = aEntry.dominatedNodes();
        for (final Edge theEdge : aEntry.outgoingEdges().collect(Collectors.toList())) {
            if (theEdge.edgeType() == ControlFlowEdgeType.forward) {
                final RegionNode theNode = (RegionNode) theEdge.targetNode();
                if (theDominated.contains(theNode) && aLabelSoup.contains(theNode)) {
                    theNextEntries.add(theNode);
                }
            }
        }

        final Set<RegionNode> theOtherLabels = new HashSet<>(aEntry.dominatedNodes());
        theOtherLabels.remove(aEntry);
        return new SimpleBlock(aEntryLabels, aEntry, reloop(aGraph, theNextEntries, theOtherLabels));
    }

    private Set<RegionNode> jumpTargetsOf(final Collection<RegionNode> aLabelSoup) {
        final Set<RegionNode> theResults = new HashSet<>();
        for (final RegionNode theNode : aLabelSoup) {
            for (final Edge theEdge : theNode.outgoingEdges().collect(Collectors.toList())) {
                if (aLabelSoup.contains(theEdge.targetNode())) {
                    theResults.add((RegionNode) theEdge.targetNode());
                }
            }
        }
        return theResults;
    }

    private Set<RegionNode> allForwardJumpTargetsOf(final Collection<RegionNode> aLabelSoup) {
        final Set<RegionNode> theResults = new HashSet<>();
        for (final RegionNode theNode : aLabelSoup) {
            for (final Edge theEdge : theNode.outgoingEdges().collect(Collectors.toList())) {
                if (theEdge.edgeType() == ControlFlowEdgeType.forward) {
                    theResults.add((RegionNode) theEdge.targetNode());
                }
            }
        }
        return theResults;
    }
}