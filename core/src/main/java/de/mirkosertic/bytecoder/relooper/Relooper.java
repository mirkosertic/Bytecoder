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

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

/**
 * Implementation of the Relooper Algorithm as described in Alon Zakai's Emscripten Paper.
 */
public class Relooper {

    public static abstract class Block {

        private final Set<GraphNode> entries;
        private final Label label;

        protected Block(Set<GraphNode> aEntries) {
            entries = aEntries;
            StringBuilder theBuilder = new StringBuilder();
            for (GraphNode aLabel : aEntries) {
                if (theBuilder.length() > 0) {
                    theBuilder.append("_");
                }
                theBuilder.append(aLabel.getStartAddress().getAddress());
            }
            label = new Label(theBuilder.toString());
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
            super(aEntries);
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
            super(aEntries);
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
            super(aEntries);
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
        Block theBlock =  reloop(aGraph, theEntries, theStart.dominatedNodes());

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

            GraphNode theInternalLabel = theSimple.internalLabel();
            replaceGotosIn(aTraversalStack, theSimple, theInternalLabel, theInternalLabel.getExpressions());

            replaceGotosIn(aTraversalStack, theSimple.next());
            return;
        }
        if (aBlock instanceof LoopBlock) {
            LoopBlock theLoop = (LoopBlock) aBlock;

            aTraversalStack.push(theLoop);
            replaceGotosIn(aTraversalStack, theLoop.inner());
            aTraversalStack.pop();
            replaceGotosIn(aTraversalStack, theLoop.next());
            return;
        }
        if (aBlock instanceof MultipleBlock) {
            MultipleBlock theMultiple = (MultipleBlock) aBlock;
            aTraversalStack.push(theMultiple);
            for (Block theHandler : theMultiple.handlers()) {
                replaceGotosIn(aTraversalStack, theHandler);
            }
            aTraversalStack.pop();

            replaceGotosIn(aTraversalStack, theMultiple.next());
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
                            Block theNext = aCurrent.next();
                            if (theNext == null) {
                                // we have to leave the current block, this happens on joining control flows.
                                // We break out of the current block, so the next handler will take action and
                                // hopefully can handle the new control flow
                                BreakExpression theBreak = new BreakExpression(aTraversalStack.peek().label(), theTarget.getStartAddress());
                                aList.replace(theGoto, theBreak);
                                break;
                            }
                            if (!theNext.entries().contains(theTarget)) {
                                throw new IllegalStateException("Failed to jump to " + theTarget.getStartAddress().getAddress() + " : no matching entry found!");
                            }
                            // At this point, we replace the goto with a corresponding break
                            BreakExpression theBreak = new BreakExpression(aCurrent.label(), theTarget.getStartAddress());
                            aList.replace(theGoto, theBreak);
                        } else {
                            // We can only branch back into the known stack of nested blocks
                            boolean theSomethingFound = false;
                            for (int i=aTraversalStack.size() -1;i>=0;i--) {
                                Block theNestingBlock = aTraversalStack.get(i);
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
    private Block reloop(ControlFlowGraph aGraph, Set<GraphNode> aEntryLabels, Set<GraphNode> aLabelSoup) {

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
                return new SimpleBlock(aEntryLabels, theEntry, reloop(aGraph, theNextEntries, theOtherLabels));
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
            Set<GraphNode> theInternal = new HashSet<>();
            Set<GraphNode> theNext = new HashSet<>();
            for (GraphNode theTestNode : aLabelSoup) {
                Set<GraphNode> theReachableLabels = theTestNode.forwardReachableNodes();
                if (containsAnyOf(theReachableLabels, aEntryLabels)) {
                    theInternal.add(theTestNode);
                } else {
                    theNext.add(theTestNode);
                }
            }

            // We search for branches out of the internal node, this
            // will mark the entries of the next
            Set<GraphNode> theNextEntryLabels = new HashSet<>();
            for (GraphNode theTestNode : theInternal) {
                for (Map.Entry<GraphNode.Edge, GraphNode> theSuc : theTestNode.getSuccessors().entrySet()) {
                    if (theSuc.getKey().getType() == GraphNode.EdgeType.NORMAL) {
                        if (theNextEntryLabels.contains(theSuc.getValue())) {
                            theNextEntryLabels.add(theSuc.getValue());
                        }
                    }
                }
            }

            Block theReloopedInternal = reloop(aGraph, aEntryLabels, theInternal);
            Block theReloopedNext = reloop(aGraph, theNextEntryLabels, theNext);

            return new LoopBlock(aEntryLabels, theReloopedInternal, theReloopedNext);
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
                theHandlers.add(reloop(aGraph, theHandlerEntries, theDominated));
            }

            for (Map.Entry<GraphNode, Set<GraphNode>> theHandler : theEntryReaches.entrySet()) {
                for (GraphNode theJumpTarget : allForrwardJumpTargetsOf(theHandler.getValue())) {
                    if (theRest.contains(theJumpTarget)) {
                        theRestEntries.add(theJumpTarget);
                    }
                }
            }

            Block theNext = reloop(aGraph, theRestEntries, theRest);
            return new MultipleBlock(aEntryLabels, theHandlers, theNext);
        }

        // If we could not create a Multiple block, then create
        // a Loop block as described above
        throw new IllegalStateException("What do do now?");
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

    private Set<GraphNode> allForrwardJumpTargetsOf(Collection<GraphNode> aLabelSoup) {
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

    private boolean containsAnyOf(Collection<GraphNode> aSoup, Collection<GraphNode> aTestLabels) {
        for (GraphNode theTestLabel : aTestLabels) {
            if (aSoup.contains(theTestLabel)) {
                return true;
            }
        }
        return false;
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
