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

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

import de.mirkosertic.bytecoder.ssa.BreakExpression;
import de.mirkosertic.bytecoder.ssa.ContinueExpression;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.ExpressionListContainer;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.GraphNode;
import de.mirkosertic.bytecoder.ssa.Label;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;

/**
 * Implementation of the Relooper Algorithm as described in Alon Zakai's Emscripten Paper.
 */
public class Relooper {

    public static abstract class Block {

        private final Set<GraphNode> entries;
        private final Label label;

        protected Block(Set<GraphNode> aEntries, String aLabelPrefix) {
            entries = aEntries;
            StringBuilder theBuilder = new StringBuilder();
            for (GraphNode aLabel : aEntries) {
                if (theBuilder.length() > 0) {
                    theBuilder.append("_");
                }
                theBuilder.append(aLabel.getStartAddress().getAddress());
            }
            label = new Label(aLabelPrefix + theBuilder.toString());
        }

        public Set<GraphNode> entries() {
            return entries;
        }

        public abstract Block next();

        public Label label() {
            return label;
        }
    }

    /**
     * Simple block: A block with
     * One Internal label, and
     * a Next block, which the internal label branches to.
     * The block is later translated simply into the code for
     * that label, and the Next block appears right after it.
     */
    public static class SimpleBlock extends Block {
        private final GraphNode internalLabel;
        private final Block next;

        public SimpleBlock(Set<GraphNode> aEntries, GraphNode aInternalLabel, Block aNext) {
            super(aEntries, "S_");
            internalLabel = aInternalLabel;
            next = aNext;
        }

        public GraphNode internalLabel() {
            return internalLabel;
        }

        @Override
        public Block next() {
            return next;
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

        public LoopBlock(Set<GraphNode> aEntries, Block aInner, Block aNext) {
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

        public MultipleBlock(Set<GraphNode> aEntries, Set<Block> aHandlers, Block aNext) {
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
    }

    public Block reloop(ControlFlowGraph aGraph) {
        Set<GraphNode> theEntries = new HashSet<>();
        GraphNode theStart = aGraph.startNode();
        theEntries.add(aGraph.startNode());

        // At this point, we use the dominated nodes of our start node as
        // the initisl "label-soup" for the Relooper. This will exclude
        // exception handler and finalize blocks from the the CFG and
        // will reduce the amount of labels to be processed(hopefully)
        Block theBlock =  reloop(theEntries, theStart.dominatedNodes());

        // At this point, we have a constructed Relooper-CFG. Now we have to replace
        // all GOTOs with either corresponding break or continue statements.
        replaceGotosIn(theBlock);

        // We are done here
        return theBlock;
    }

    private void replaceGotosIn(Block aBlock) {
        replaceGotosIn(new Stack<>(), aBlock);
    }

    private void replaceGotosIn(Stack<Block> aTraversalStack, Block aBlock) {
        if (aBlock == null) {
            return;
        }
        if (aBlock instanceof SimpleBlock) {
            SimpleBlock theSimple = (SimpleBlock) aBlock;

            aTraversalStack.push(theSimple);
            GraphNode theInternalLabel = theSimple.internalLabel();
            replaceGotosIn(aTraversalStack, theSimple, theInternalLabel, theInternalLabel.getExpressions());

            replaceGotosIn(aTraversalStack, theSimple.next());
            aTraversalStack.pop();

            return;
        }
        if (aBlock instanceof LoopBlock) {
            LoopBlock theLoop = (LoopBlock) aBlock;

            aTraversalStack.push(theLoop);
            replaceGotosIn(aTraversalStack, theLoop.inner());

            replaceGotosIn(aTraversalStack, theLoop.next());
            aTraversalStack.pop();
            return;
        }
        if (aBlock instanceof MultipleBlock) {
            MultipleBlock theMultiple = (MultipleBlock) aBlock;
            aTraversalStack.push(theMultiple);
            for (Block theHandler : theMultiple.handlers()) {
                replaceGotosIn(aTraversalStack, theHandler);
            }

            replaceGotosIn(aTraversalStack, theMultiple.next());
            aTraversalStack.pop();
            return;
        }

        throw new IllegalStateException("Don't know how to handle " + aBlock);
    }

    private void replaceGotosIn(Stack<Block> aTraversalStack, SimpleBlock aCurrent, GraphNode aLabel, ExpressionList aList) {
        for (Expression theExpression : aList.toList()) {
            if (theExpression instanceof ExpressionListContainer) {
                ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (ExpressionList theList : theContainer.getExpressionLists()) {
                    replaceGotosIn(aTraversalStack, aCurrent, aLabel, theList);
                }
            }
            if (theExpression instanceof GotoExpression) {
                GotoExpression theGoto = (GotoExpression) theExpression;
                boolean theGotoFound = false;
                // We search the successor edge
                for (Map.Entry<GraphNode.Edge, GraphNode> theSuc : aLabel.getSuccessors().entrySet()) {
                    if (Objects.equals(theSuc.getValue().getStartAddress(), theGoto.getJumpTarget())) {
                        theGotoFound = true;
                        GraphNode theTarget = theSuc.getValue();
                        // We found the matching edge
                        if (theSuc.getKey().getType() == GraphNode.EdgeType.NORMAL) {
                            // We can only branch to the next block
                            // We search the whole hiararchy to find the right block to break out

                            boolean theSomethingFound = false;
                            for (int i=aTraversalStack.size() -1 ; i>= 0; i--) {
                                Block theNestingBlock = aTraversalStack.get(i);
                                if (theNestingBlock.next() != null && theNestingBlock.next().entries().contains(theTarget)) {
                                    BreakExpression theBreak = new BreakExpression(theNestingBlock.label(), theTarget.getStartAddress());
                                    aList.replace(theGoto, theBreak);
                                    theSomethingFound = true;
                                    break;
                                } else if (theNestingBlock.entries().contains(theTarget)) {
                                    ContinueExpression theContinue = new ContinueExpression(theNestingBlock.label(), theTarget.getStartAddress());
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
                            for (Block theNestingBlock : aTraversalStack) {
                                if (theNestingBlock.entries().contains(theTarget)) {
                                    theSomethingFound = true;

                                    // We can return to the target in the hierarchy
                                    ContinueExpression theContinue = new ContinueExpression(theNestingBlock.label(), theTarget.getStartAddress());
                                    aList.replace(theGoto, theContinue);
                                    break;
                                }
                            }
                            if (!theSomethingFound) {
                                throw new IllegalStateException(
                                        "No back edge target found for " + theTarget.getStartAddress().getAddress());
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
    private Block reloop(Set<GraphNode> aEntryLabels, Set<GraphNode> aLabelSoup) {

        // If there are no entry labels at all, we return null.
        // This will become the next value of the predecessor block and will mark the end of the
        // invocation chain.
        if (aEntryLabels.isEmpty()) {
            return null;
        }

        Collection<GraphNode> theJumptargets = jumpTargetsOf(aLabelSoup);

        // If we have a single entry, and cannot return to it (by
        // some other label later on branching to it) then create a
        // Simple block, with the entry as its internal label, and the
        // Next block comprised of all the other labels. The entries
        // for the Next block are the entries to which the internal
        // label can branch.
        if (aEntryLabels.size() == 1) {
            GraphNode theEntry = aEntryLabels.iterator().next();
            if (!theJumptargets.contains(theEntry)) {
                return createSimpleBlock(aEntryLabels, aLabelSoup, theEntry);
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
                GraphNode theSingleEntry = aEntryLabels.iterator().next();

                Set<GraphNode> theInternalLabels = theSingleEntry.dominatedNodes();
                Set<GraphNode> theRestLabels = new HashSet<>(aLabelSoup);
                theRestLabels.removeAll(theInternalLabels);

                // Search for branch-outs of the current loop
                Set<GraphNode> theRestEntries = new HashSet<>();
                for (GraphNode theReachable : theSingleEntry.forwardReachableNodes()) {
                    if (theRestLabels.contains(theReachable)) {
                        theRestEntries.add(theReachable);
                    }
                }

                Block theInternalBlock =
                        createSimpleBlock(aEntryLabels, theInternalLabels, theSingleEntry);
                Block theNextBlock = reloop(theRestEntries, theRestLabels);

                return new LoopBlock(aEntryLabels, theInternalBlock, theNextBlock);
            }
            throw new IllegalStateException("Multiple entries in LoopBlock not implemented!");
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
            Set<GraphNode> theRest = new HashSet<>(aLabelSoup);
            Set<GraphNode> theRestEntries = new HashSet<>();
            Map<GraphNode, Set<GraphNode>> theEntryReaches = new HashMap<>();

            Set<Block> theHandlers = new HashSet<>();
            for (GraphNode theEntry : aEntryLabels) {
                Set<GraphNode> theDominated = theEntry.dominatedNodes();
                theEntryReaches.put(theEntry, theDominated);
                theRest.removeAll(theDominated);

                Set<GraphNode> theHandlerEntries = new HashSet<>();
                theHandlerEntries.add(theEntry);
                theHandlers.add(reloop(theHandlerEntries, theDominated));
            }

            for (Map.Entry<GraphNode, Set<GraphNode>> theHandler : theEntryReaches.entrySet()) {
                for (GraphNode theJumpTarget : allForwardJumpTargetsOf(theHandler.getValue())) {
                    if (theRest.contains(theJumpTarget)) {
                        theRestEntries.add(theJumpTarget);
                    }
                }
            }

            Block theNext = reloop(theRestEntries, theRest);
            return new MultipleBlock(aEntryLabels, theHandlers, theNext);
        }

        // If we could not create a Multiple block, then create
        // a Loop block as described above
        throw new IllegalStateException("What do do now?");
    }

    private Block createSimpleBlock(Set<GraphNode> aEntryLabels, Set<GraphNode> aLabelSoup,
            GraphNode theEntry) {
        Set<GraphNode> theNextEntries = new HashSet<>();
        Set<GraphNode> theDominated = theEntry.dominatedNodes();
        for (Map.Entry<GraphNode.Edge, GraphNode> theSucc : theEntry.getSuccessors().entrySet()) {
            if (theSucc.getKey().getType() == GraphNode.EdgeType.NORMAL) {
                if (theDominated.contains(theSucc.getValue())) {
                    theNextEntries.add(theSucc.getValue());
                }
            }
        }

        Set<GraphNode> theOtherLabels = new HashSet<>(aLabelSoup);
        theOtherLabels.remove(theEntry);
        return new SimpleBlock(aEntryLabels, theEntry, reloop(theNextEntries, theOtherLabels));
    }

    private Set<GraphNode> jumpTargetsOf(Collection<GraphNode> aLabelSoup) {
        Set<GraphNode> theResults = new HashSet<>();
        for (GraphNode theNode : aLabelSoup) {
            for (Map.Entry<GraphNode.Edge, GraphNode> theEntry : theNode.getSuccessors().entrySet()) {
                if (aLabelSoup.contains(theEntry.getValue())) {
                    theResults.add(theEntry.getValue());
                }
            }
        }
        return theResults;
    }

    private Set<GraphNode> allForwardJumpTargetsOf(Collection<GraphNode> aLabelSoup) {
        Set<GraphNode> theResults = new HashSet<>();
        for (GraphNode theNode : aLabelSoup) {
            for (Map.Entry<GraphNode.Edge, GraphNode> theEntry : theNode.getSuccessors().entrySet()) {
                if (theEntry.getKey().getType() == GraphNode.EdgeType.NORMAL) {
                    theResults.add(theEntry.getValue());
                }
            }
        }
        return theResults;
    }

    public void debugPrint(PrintStream aStream, Block aBlock) {
        debugPrint(aStream, aBlock, 0);
    }

    private void debugPrint(PrintStream aStream, Block aBlock, int aInset) {
        printInset(aStream, aInset);
        if (aBlock == null) {
            aStream.println(" NULL");
            return;
        }
        if (aBlock instanceof SimpleBlock) {
            aStream.println("SimpleBlock " + aBlock.label().name());
            SimpleBlock theSimple = (SimpleBlock) aBlock;
            debugPrint(aStream, aInset +1, theSimple.internalLabel().getExpressions());

            debugPrint(aStream, theSimple.next(), aInset + 1);
            return;
        }
        if (aBlock instanceof LoopBlock) {
            aStream.println("Loop " + aBlock.label().name());
            LoopBlock theLoop = (LoopBlock) aBlock;
            debugPrint(aStream, theLoop.inner(), aInset + 1);
            debugPrint(aStream, theLoop.next(), aInset + 1);
            return;
        }
        if (aBlock instanceof MultipleBlock) {
            aStream.println("Multiple " + aBlock.label().name());
            MultipleBlock theMultiple = (MultipleBlock) aBlock;
            for (Block theHandler : theMultiple.handlers()) {
                debugPrint(aStream, theHandler, aInset + 1);
            }
            debugPrint(aStream, theMultiple.next, aInset + 1);
            return;
        }
        throw new IllegalStateException("No handler for " + aBlock);
    }

    private void printInset(PrintStream aStream, int aInset) {
        for (int i = 0; i < aInset; i++) {
            aStream.print(" ");
        }
    }

    private void debugPrint(PrintStream aStream, int aInset, ExpressionList aExpressionList) {
        for (Expression theExpression : aExpressionList.toList()) {
            if (theExpression instanceof BreakExpression) {
                BreakExpression theBreak = (BreakExpression) theExpression;
                printInset(aStream, aInset);
                aStream.println(
                        "Break " + theBreak.blockToBreak().name() + " and jump to " + theBreak.jumpTarget().getAddress());
            } else if (theExpression instanceof ContinueExpression) {
                ContinueExpression theContinue = (ContinueExpression) theExpression;
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
