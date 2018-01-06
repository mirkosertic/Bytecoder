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

import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.GraphNode;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the Relooper Algorithm as described in Alon Zakai's Emscripten Paper.
 */
public class Relooper {

    interface Block {
    }

    /**
     * Simple block: A block with
     * One Internal label, and
     * a Next block, which the internal label branches to.
     * The block is later translated simply into the code for
     * that label, and the Next block appears right after it.
     */
    public static class simpleBlock implements Block {
        private final GraphNode internalLabel;
        private final Block next;

        public simpleBlock(GraphNode aInternalLabel, Block aNext) {
            internalLabel = aInternalLabel;
            next = aNext;
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
    public static class LoopBlock implements Block {
        private final Block internal;
        private final Block next;

        public LoopBlock(Block aInternal, Block aNext) {
            internal = aInternal;
            next = aNext;
        }
    }

    public Block reloop(ControlFlowGraph aGraph) {
        Set<GraphNode> theLabelSoup = new HashSet<>(aGraph.getKnownNodes());
        Set<GraphNode> theEntries = new HashSet<>();
        theEntries.add(aGraph.startNode());

        return reloop(theEntries, theLabelSoup);
    }

    /**
     * Receive a set of labels and which of them are entry
     * points. We wish to create a block comprised of all those
     * labels.
     */
    private Block reloop(Collection<GraphNode> aEntryLabels, Collection<GraphNode> aLabelSoup) {

        // If there are no entry labels at all, re return null.
        // This will become the next value of the predecessor block and will mark the end of the
        // invocation chain.
        if (aEntryLabels.isEmpty()) {
            return null;
        }

        Collection theJumptargets = jumpTargetsOf(aLabelSoup);

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
                for (Map.Entry<GraphNode.Edge, GraphNode> theBranch : theEntry.getSuccessors().entrySet()) {
                    if (theBranch.getKey().getType() == GraphNode.EdgeType.NORMAL) {
                        theNextEntries.add(theBranch.getValue());
                    }
                }
                Set<GraphNode> theOtherLabels = new HashSet<>(aLabelSoup);
                theOtherLabels.remove(theEntry);
                return new simpleBlock(theEntry, reloop(theNextEntries, theOtherLabels));
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
                Set<GraphNode> theReachableLabels = theTestNode.reachableNodes();
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

            Block theReloopedInternal = reloop(aEntryLabels, theInternal);
            Block theReloopedNext = reloop(theNextEntryLabels, theNext);

            return new LoopBlock(theReloopedInternal, theReloopedNext);
        }

        throw new IllegalStateException("What do do now?");
    }

    private Set<GraphNode> jumpTargetsOf(Collection<GraphNode> aNode) {
        Set<GraphNode> theResults = new HashSet<>();
        for (GraphNode theNode : aNode) {
            for (Map.Entry<GraphNode.Edge, GraphNode> theEntry : theNode.getSuccessors().entrySet()) {
                theResults.add(theEntry.getValue());
            }
        }
        return theResults;
    }

    public boolean containsAnyOf(Collection<GraphNode> aSoup, Collection<GraphNode> aTestLabels) {
        for (GraphNode theTestLabel : aTestLabels) {
            if (aSoup.contains(theTestLabel)) {
                return true;
            }
        }
        return false;
    }
}
