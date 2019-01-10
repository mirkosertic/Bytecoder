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
import de.mirkosertic.bytecoder.ssa.BreakExpression;
import de.mirkosertic.bytecoder.ssa.ContinueExpression;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.Label;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
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

        public Label label() {
            return label;
        }

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

        public SimpleBlock(final Set<RegionNode> aEntries, final RegionNode aInternalLabel, final Block aNext) {
            super(aEntries, "S_");
            internalLabel = aInternalLabel;
            next = aNext;
        }

        public RegionNode internalLabel() {
            return internalLabel;
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
            if (next != null) {
                return next.containsMultipleBlock();
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
        final Block theBlock =  reloop(aGraph, theEntries, theStart.dominatedNodes());

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
            replaceGotosIn(aTraversalStack, theSimple, theInternalLabel, theInternalLabel.getExpressions());

            replaceGotosIn(aTraversalStack, theSimple.next());
            aTraversalStack.pop();

            final RegionNode theNode = theSimple.internalLabel;
            final Expression theLastExpression = theNode.getExpressions().lastExpression();
            // Breaks at the end of the internal label breaking out of the simple block
            // can be silent, they only need to set the __label__ variable
            if (theLastExpression instanceof BreakExpression) {
                final BreakExpression theBreak = (BreakExpression) theLastExpression;
                if (Objects.equals(theBreak.blockToBreak().name(), theSimple.label().name())) {
                    theBreak.silent();
                }
            }

            // At this place, we check the goto labels for the blocks successors to make them available during rendering
            // We search the successor edge
            for (final Map.Entry<RegionNode.Edge, RegionNode> theSuc : theInternalLabel.getSuccessors().entrySet()) {
                final RegionNode theTarget = theSuc.getValue();
                // We found the matching edge
                if (theSuc.getKey().getType() == RegionNode.EdgeType.NORMAL) {
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
                        if (theNestingBlock.entries().contains(theTarget)) {
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
                for (final Map.Entry<RegionNode.Edge, RegionNode> theSuc : aLabel.getSuccessors().entrySet()) {
                    if (Objects.equals(theSuc.getValue().getStartAddress(), theGoto.getJumpTarget())) {
                        theGotoFound = true;
                        final RegionNode theTarget = theSuc.getValue();
                        // We found the matching edge
                        if (theSuc.getKey().getType() == RegionNode.EdgeType.NORMAL) {
                            // We can only branch to the next block
                            // We search the whole hiararchy to find the right block to break out

                            boolean theSomethingFound = false;
                            for (int i=aTraversalStack.size() -1 ; i>= 0; i--) {
                                final Block theNestingBlock = aTraversalStack.get(i);
                                if (theNestingBlock.next() != null && theNestingBlock.next().entries().contains(theTarget)) {
                                    theNestingBlock.requireLabel();
                                    final BreakExpression theBreak = new BreakExpression(theNestingBlock.label(), theTarget.getStartAddress());
                                    aList.replace(theGoto, theBreak);

                                    if (theNestingBlock.next() instanceof SimpleBlock && theNestingBlock.next().entries().size() == 1) {
                                        theBreak.noSetRequired();
                                    }

                                    theSomethingFound = true;
                                    break;
                                } else if (theNestingBlock.entries().contains(theTarget)) {
                                    theNestingBlock.requireLabel();
                                    final ContinueExpression theContinue = new ContinueExpression(theNestingBlock.label(), theTarget.getStartAddress());
                                    aList.replace(theGoto, theContinue);
                                    theSomethingFound = true;
                                    break;
                                }
                            }

                            if (!theSomethingFound) {
                                throw new IllegalStateException("Failed to jump to " + theTarget.getStartAddress().getAddress() + " from " + aCurrent.label().name() + " : no matching entry found!");
                            }

                        } else {
                            // We can only branch back into the known stack of nested blocks
                            boolean theSomethingFound = false;
                            for (final Block theNestingBlock : aTraversalStack) {
                                if (theNestingBlock.entries().contains(theTarget)) {
                                    theSomethingFound = true;

                                    // We can return to the target in the hierarchy
                                    theNestingBlock.requireLabel();
                                    final ContinueExpression theContinue = new ContinueExpression(theNestingBlock.label(), theTarget.getStartAddress());
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
                    throw new IllegalStateException("No GOTO possible for " + theGoto.getJumpTarget().getAddress() + " in label " + aCurrent.label().name());
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

                final Set<RegionNode> theInternalLabels = theSingleEntry.dominatedNodes().stream().filter(t -> aLabelSoup.contains(t)).collect(
                        Collectors.toSet());
                final Set<RegionNode> theRestLabels = new HashSet<>(aLabelSoup);
                theRestLabels.removeAll(theInternalLabels);

                // Search for branch-outs of the current loop
                final Set<RegionNode> theRestEntries = new HashSet<>();
                for (final RegionNode theReachable : theSingleEntry.forwardReachableNodes()) {
                    if (theRestLabels.contains(theReachable)) {
                        theRestEntries.add(theReachable);
                    }
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
                final Set<RegionNode> theDominated = theEntry.dominatedNodes().stream().filter(t -> aLabelSoup.contains(t)).collect(
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
                final Set<RegionNode> theCoveredNodes = new HashSet<>();
                final Set<RegionNode> theNextNodes = new HashSet<>();
                for (final RegionNode theLabel : aLabelSoup) {
                    // All regions covered by the same catch handler
                    // are part of the try block
                    final List<RegionNode.ExceptionHandler> theActive = aGraph.exceptionHandlersActiveAt(theLabel.getStartAddress());
                    if (!theActive.isEmpty()) {
                        // We chose the first element, as this is the mose enclosing one due to
                        // the sorting in the list
                        final RegionNode.ExceptionHandler theSingleActiveHandler = theActive.get(0);
                        if (theSingleActiveHandler.sameCatchBlockAs(theSingleHandler)) {
                            theCoveredNodes.add(theLabel);
                        } else {
                            theNextNodes.add(theLabel);
                        }
                    } else {
                        for (final BytecodeExceptionTableEntry theSingleExceptionCatch : theSingleHandler.getCatchEntries()) {
                            if (theSingleExceptionCatch.coveres(theLabel.getStartAddress())) {
                                theCoveredNodes.add(theLabel);
                            } else {
                                theNextNodes.add(theLabel);
                            }
                        }
                    }
                }

                final Set<RegionNode> theInnerNext = new HashSet<>();
                final Set<RegionNode> theOuterNext = new HashSet<>();
                for (final RegionNode theCoveredNode : theCoveredNodes) {
                    for (final Map.Entry<RegionNode.Edge, RegionNode> theSucc : theCoveredNode.getSuccessors().entrySet()) {
                        final RegionNode theNode = theSucc.getValue();
                        if (theSucc.getKey().getType() == RegionNode.EdgeType.NORMAL) {
                            if (theCoveredNodes.contains(theNode)) {
                                theInnerNext.add(theNode);
                            } else{
                                theOuterNext.add(theNode);
                            }
                        }
                    }
                }

                theCoveredNodes.removeAll(aEntryLabels);
                final Block theInnerBlock = new SimpleBlock(aEntryLabels, aEntry, reloop(aGraph, theInnerNext, theCoveredNodes));

                // We finally need to compute the catchBlocks
                final Map<BytecodeOpcodeAddress, Set<BytecodeUtf8Constant>> theAssignedHandlers = new HashMap<>();
                for (final BytecodeExceptionTableEntry theEntry : theSingleHandler.getCatchEntries()) {
                    if (!theEntry.isFinally()) {
                        final Set<BytecodeUtf8Constant> theCatchTypes = theAssignedHandlers
                                .computeIfAbsent(theEntry.getHandlerPc(), k -> new HashSet<>());
                        theCatchTypes.add(theEntry.getCatchType().getConstant());
                    } else {
                        final RegionNode theFinallyNode = aGraph.nodeStartingAt(theEntry.getHandlerPc());
                        final Set<RegionNode> theEntries = new HashSet<>();
                        theEntries.add(theFinallyNode);
                        finallyBlock = reloop(aGraph, theEntries, theFinallyNode.dominatedNodes());
                    }
                }

                for (final Map.Entry<BytecodeOpcodeAddress, Set<BytecodeUtf8Constant>> theEntry : theAssignedHandlers.entrySet()) {
/*                    final Set<RegionNode> theEntries = new HashSet<>();
                    final RegionNode theHandlerStart = aGraph.nodeStartingAt(theEntry.getKey());
                    theEntries.add(theHandlerStart);

                    final Set<RegionNode> theTagSoup = theHandlerStart.dominatedNodes();

                    final TryBlock.CatchBlock theCatchBlock = new TryBlock.CatchBlock(theEntry.getValue(),
                            reloop(aGraph, theEntries, theTagSoup));
                    catchBlocks.add(theCatchBlock);*/
                }

                // If all blocks are covered, there is no next
                if (theNextNodes.isEmpty()) {
                    return new TryBlock(aEntryLabels, theInnerBlock, null, catchBlocks, finallyBlock);
                }

                return new TryBlock(aEntryLabels, theInnerBlock, reloop(aGraph, theOuterNext, theNextNodes), catchBlocks, finallyBlock);
            }
        }

        final Set<RegionNode> theNextEntries = new HashSet<>();
        final Set<RegionNode> theDominated = aEntry.dominatedNodes();
        for (final Map.Entry<RegionNode.Edge, RegionNode> theSucc : aEntry.getSuccessors().entrySet()) {
            if (theSucc.getKey().getType() == RegionNode.EdgeType.NORMAL) {
                final RegionNode theNode = theSucc.getValue();
                if (theDominated.contains(theNode) && aLabelSoup.contains(theNode)) {
                    theNextEntries.add(theSucc.getValue());
                }
            }
        }

        final Set<RegionNode> theOtherLabels = new HashSet<>(aLabelSoup);
        theOtherLabels.remove(aEntry);
        return new SimpleBlock(aEntryLabels, aEntry, reloop(aGraph, theNextEntries, theOtherLabels));
    }

    private Set<RegionNode> jumpTargetsOf(final Collection<RegionNode> aLabelSoup) {
        final Set<RegionNode> theResults = new HashSet<>();
        for (final RegionNode theNode : aLabelSoup) {
            for (final Map.Entry<RegionNode.Edge, RegionNode> theEntry : theNode.getSuccessors().entrySet()) {
                if (aLabelSoup.contains(theEntry.getValue())) {
                    theResults.add(theEntry.getValue());
                }
            }
        }
        return theResults;
    }

    private Set<RegionNode> allForwardJumpTargetsOf(final Collection<RegionNode> aLabelSoup) {
        final Set<RegionNode> theResults = new HashSet<>();
        for (final RegionNode theNode : aLabelSoup) {
            for (final Map.Entry<RegionNode.Edge, RegionNode> theEntry : theNode.getSuccessors().entrySet()) {
                if (theEntry.getKey().getType() == RegionNode.EdgeType.NORMAL) {
                    theResults.add(theEntry.getValue());
                }
            }
        }
        return theResults;
    }

    public void debugPrint(final PrintStream aStream, final Block aBlock) {
        debugPrint(aStream, aBlock, 0);
    }

    private void debugPrint(final PrintStream aStream, final Block aBlock, final int aInset) {
        printInset(aStream, aInset);
        if (aBlock == null) {
            aStream.println(" NULL");
            return;
        }
        if (aBlock instanceof SimpleBlock) {
            aStream.println("SimpleBlock " + aBlock.label().name());
            final SimpleBlock theSimple = (SimpleBlock) aBlock;
            debugPrint(aStream, aInset +1, theSimple.internalLabel().getExpressions());

            debugPrint(aStream, theSimple.next(), aInset + 1);
            return;
        }
        if (aBlock instanceof LoopBlock) {
            aStream.println("Loop " + aBlock.label().name());
            final LoopBlock theLoop = (LoopBlock) aBlock;
            debugPrint(aStream, theLoop.inner(), aInset + 1);
            debugPrint(aStream, theLoop.next(), aInset + 1);
            return;
        }
        if (aBlock instanceof MultipleBlock) {
            aStream.println("Multiple " + aBlock.label().name());
            final MultipleBlock theMultiple = (MultipleBlock) aBlock;
            for (final Block theHandler : theMultiple.handlers()) {
                debugPrint(aStream, theHandler, aInset + 1);
            }
            debugPrint(aStream, theMultiple.next, aInset + 1);
            return;
        }
        throw new IllegalStateException("No handler for " + aBlock);
    }

    private void printInset(final PrintStream aStream, final int aInset) {
        for (int i = 0; i < aInset; i++) {
            aStream.print(" ");
        }
    }

    private void debugPrint(final PrintStream aStream, final int aInset, final ExpressionList aExpressionList) {
        for (final Expression theExpression : aExpressionList.toList()) {
            if (theExpression instanceof BreakExpression) {
                final BreakExpression theBreak = (BreakExpression) theExpression;
                printInset(aStream, aInset);
                aStream.println(
                        "Break " + theBreak.blockToBreak().name() + " and jump to " + theBreak.jumpTarget().getAddress());
            } else if (theExpression instanceof ContinueExpression) {
                final ContinueExpression theContinue = (ContinueExpression) theExpression;
                printInset(aStream, aInset);
                aStream.println(
                        "Continue at " + theContinue.labelToReturnTo().name());
            } else if (theExpression instanceof ReturnExpression) {
                printInset(aStream, aInset);
                aStream.println("Return");
            } else if (theExpression instanceof GotoExpression) {
                throw new IllegalStateException("Goto should have been removed!");
            }
        }
    }
}