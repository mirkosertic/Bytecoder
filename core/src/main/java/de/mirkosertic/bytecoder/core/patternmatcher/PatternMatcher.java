/*
 * Copyright 2024 Mirko Sertic
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
package de.mirkosertic.bytecoder.core.patternmatcher;

import de.mirkosertic.bytecoder.api.Logger;
import de.mirkosertic.bytecoder.core.ir.ControlTokenConsumer;
import de.mirkosertic.bytecoder.core.ir.EdgeType;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.NodeType;
import de.mirkosertic.bytecoder.core.ir.Projection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class PatternMatcher {

    private class Path {

        private final String path;

        private Path() {
            this("R");
        }

        private Path(final String path) {
            this.path = path;
        }

        private Path addIncoming(final int incomingIndex, final NodeType nodeType, final int expectedIndex) {
            return new Path(path + ".i[" + incomingIndex + ":" + nodeType + ":" + expectedIndex + "]");
        }

        private Path addOutgoing(final NodeType nodeType, final int expectedIndex) {
            return new Path(path + ".o[" + nodeType + ":" + expectedIndex + "]");
        }

        private Path controlComingFrom(final int nodeIndex, final NodeType nodeType) {
            return new Path(path + ".cin[" + nodeIndex + ":" + nodeType + "]");
        }

        private Path controlGoingTo(final Projection projection, final int nodeIndex, final NodeType nodeType) {
            return new Path(path + ".cout[" + nodeIndex + ":" + nodeType + ":" + projection.getClass().getSimpleName() + ":" + projection.edgeType() + "]");
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final Path path1 = (Path) o;
            return Objects.equals(path, path1.path);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(path);
        }

        private Node parseRoot(final EvaluationContext context) {
            return context.getRoot();
        }

        private Node parseIncomingData(final String token, final Node currentNode, final EvaluationContext evaluationContext) {
            final int p = token.indexOf(":");
            final int p2 = token.lastIndexOf(":");
            final int index = Integer.parseInt(token.substring(2, p));
            final NodeType nodeType = NodeType.valueOf(token.substring(p + 1, p2));
            final int expectedNodeIndex = Integer.parseInt(token.substring(p2 + 1, token.length() - 1));

            final Node[] incoming = currentNode.incomingDataFlows;
            if (incoming.length > index) {
                final Node node = incoming[index];
                if (node.nodeType != nodeType) {
                    // Unexpected node type
                    PatternMatcher.this.logger.debug(" -> Failed, node is {}, but should be {}", node.nodeType, nodeType);
                    return null;
                }

                if (evaluationContext.nodeKnownAt(expectedNodeIndex)) {
                    final Node knownNodeAtIndex = evaluationContext.getNodeAt(expectedNodeIndex);
                    if (knownNodeAtIndex != node) {
                        PatternMatcher.this.logger.debug(" -> Failed, node is {}, but should be {}", node, knownNodeAtIndex);
                        return null;
                    }
                    PatternMatcher.this.logger.debug(" -> Node match at index {}", expectedNodeIndex);
                } else {
                    PatternMatcher.this.logger.debug(" -> Registering {} at index {}", node, expectedNodeIndex);
                    evaluationContext.registerNodeAt(expectedNodeIndex, node);
                }

                return node;
            }

            PatternMatcher.this.logger.debug(" -> Failed, incoming length is too short : {}", incoming.length);
            return null;
        }

        private Node parseOutgoingData(final String token, final Node currentNode, final EvaluationContext evaluationContext) {
            final int p = token.indexOf(":");
            final NodeType nodeType = NodeType.valueOf(token.substring(2, p));
            final int expectedNodeIndex = Integer.parseInt(token.substring(p + 1, token.length() - 1));

            final Node[] outgoing = evaluationContext.outgoingDataFlowsFor(currentNode);
            final List<Node> candidates = Arrays.stream(outgoing).filter(t -> t.nodeType == nodeType).collect(Collectors.toList());
            if (candidates.size() == 1) {
                final Node node = candidates.get(0);

                if (evaluationContext.nodeKnownAt(expectedNodeIndex)) {
                    final Node knownNodeAtIndex = evaluationContext.getNodeAt(expectedNodeIndex);
                    if (knownNodeAtIndex != node) {
                        PatternMatcher.this.logger.debug(" -> Failed, node is {}, but should be {}", node, knownNodeAtIndex);
                        return null;
                    }
                    PatternMatcher.this.logger.debug(" -> Node match at index {}", expectedNodeIndex);
                } else {
                    PatternMatcher.this.logger.debug(" -> Registering {} at index {}", node, expectedNodeIndex);
                    evaluationContext.registerNodeAt(expectedNodeIndex, node);
                }

                return node;

            } else if (candidates.size() > 1) {
                // More than one candidate, maybe we can further strip this down
                final List<Node> furtherCheck =  candidates.stream().filter(t -> evaluationContext.nodeKnownAt(expectedNodeIndex) && t == evaluationContext.getNodeAt(expectedNodeIndex)).collect(Collectors.toList());
                if (furtherCheck.size() == 1) {
                    return furtherCheck.get(0);
                } else {
                    PatternMatcher.this.logger.debug(" -> Failed, cannot strip down nodes {} to {}", candidates, token);
                    return null;
                }
            }

            PatternMatcher.this.logger.debug(" -> Failed, matching outgoing nodes are  != 1 : {}", candidates);
            return null;
        }

        private Node parseControlflowTo(final String token, final ControlTokenConsumer currentNode, final EvaluationContext evaluationContext) {
            final int p1 = token.indexOf(":");
            final int p2 = token.indexOf(":", p1 + 1);
            final int p3 = token.indexOf(":", p2 + 1);
            final int expectedNodeIndex = Integer.parseInt(token.substring(5, p1));
            final NodeType nodeType = NodeType.valueOf(token.substring(p1 + 1, p2));
            final String projectionClassName = token.substring(p2 + 1, p3);
            final EdgeType edgeType = EdgeType.valueOf(token.substring(p3 + 1, token.length() - 1));
            final List<Map.Entry<Projection, ControlTokenConsumer>> candidates = currentNode.controlFlowsTo.entrySet().stream().filter(t -> t.getKey().edgeType() == edgeType && projectionClassName.equals(t.getKey().getClass().getSimpleName()) && t.getValue().nodeType == nodeType).collect(Collectors.toList());
            if (candidates.size() == 1) {
                final ControlTokenConsumer node = candidates.get(0).getValue();

                if (evaluationContext.nodeKnownAt(expectedNodeIndex)) {
                    final Node knownNodeAtIndex = evaluationContext.getNodeAt(expectedNodeIndex);
                    if (knownNodeAtIndex != node) {
                        PatternMatcher.this.logger.debug(" -> Failed, node is {}, but should be {}", node, knownNodeAtIndex);
                        return null;
                    }
                    PatternMatcher.this.logger.debug(" -> Node match at index {}", expectedNodeIndex);
                } else {
                    PatternMatcher.this.logger.debug(" -> Registering {} at index {}", node, expectedNodeIndex);
                    evaluationContext.registerNodeAt(expectedNodeIndex, node);
                }

                return node;
            }
            PatternMatcher.this.logger.debug(" -> Failed, no matching control flows for token {} and node {}. Found {}, expected 1", candidates, token, currentNode, candidates.size());
            return null;
        }

        private Node parseControlflowFrom(final String token, final ControlTokenConsumer currentNode, final EvaluationContext evaluationContext) {
            final int p1 = token.indexOf(":");
            final int expectedNodeIndex = Integer.parseInt(token.substring(4, p1));
            final NodeType nodeType = NodeType.valueOf(token.substring(p1 + 1, token.length() - 1));
            final List<ControlTokenConsumer> candidates = currentNode.controlComingFrom.stream().filter(t -> t.nodeType == nodeType).collect(Collectors.toList());
            if (candidates.size() == 1) {
                final ControlTokenConsumer node = candidates.get(0);

                if (evaluationContext.nodeKnownAt(expectedNodeIndex)) {
                    final Node knownNodeAtIndex = evaluationContext.getNodeAt(expectedNodeIndex);
                    if (knownNodeAtIndex != node) {
                        PatternMatcher.this.logger.debug(" -> Failed, node is {}, but should be {}", node, knownNodeAtIndex);
                        return null;
                    }
                    PatternMatcher.this.logger.debug(" -> Node match at index {}", expectedNodeIndex);
                } else {
                    PatternMatcher.this.logger.debug(" -> Registering {} at index {}", node, expectedNodeIndex);
                    evaluationContext.registerNodeAt(expectedNodeIndex, node);
                }

                return node;
            }
            PatternMatcher.this.logger.debug(" -> Failed, no matching control flows for token {} and node {}. Found {}, expected 1", candidates, token, currentNode, candidates.size());
            return null;
        }

        private Node resolve(final EvaluationContext evaluationContext) {
            Node node = null;
            PatternMatcher.this.logger.debug("Resolving path {}", path);
            final StringTokenizer st = new StringTokenizer(path, ".");
            while (st.hasMoreTokens()) {
                final String token = st.nextToken();

                PatternMatcher.this.logger.debug(" Evaluation Token is {}", token);

                if ("R".equals(token)) {
                    node = parseRoot(evaluationContext);
                } else if (token.startsWith("i[")) {
                    node = parseIncomingData(token, node, evaluationContext);
                    if (node == null) {
                        return null;
                    }
                } else if (token.startsWith("o[")) {
                    node = parseOutgoingData(token, node, evaluationContext);
                    if (node == null) {
                        return null;
                    }
                } else if (token.startsWith("cout[")) {
                    if (node instanceof ControlTokenConsumer) {
                        node = parseControlflowTo(token, (ControlTokenConsumer) node, evaluationContext);
                        if (node == null) {
                            return null;
                        }
                    } else {
                        PatternMatcher.this.logger.debug(" Node {} is not a ControlTokenConsumer, but {}", node, node.nodeType);
                        return null;
                    }
                } else if (token.startsWith("cin[")) {
                    if (node instanceof ControlTokenConsumer) {
                        node = parseControlflowFrom(token, (ControlTokenConsumer) node, evaluationContext);
                        if (node == null) {
                            return null;
                        }
                    } else {
                        PatternMatcher.this.logger.debug(" Node {} is not a ControlTokenConsumer, but {}", node, node.nodeType);
                        return null;
                    }
                } else {
                    throw new IllegalStateException("Don't know what to do with " + token);
                }
            }
            return node;
        }
    }

    private class CompiledPattern {

        private final Map<Node, List<Path>> nodeToPaths = new HashMap<>();
        private final List<Node> nodeIndex = new ArrayList<>();

        private boolean registerToIndex(final Node n) {
            if (!nodeIndex.contains(n)) {
                nodeIndex.add(n);
                return true;
            }
            return false;
        }

        private int nodeIndexOf(final Node n) {
            return nodeIndex.indexOf(n);
        }

        private Match matchesTo(final Node analysisPoint) {
            // Check for all nodes and paths if they resolve according to their path
            final EvaluationContext evaluationContext = new EvaluationContext(analysisPoint, nodeIndex.size());

            for (final Map.Entry<Node, List<Path>> entry : nodeToPaths.entrySet()) {
                final int expectedIndex = nodeIndex.indexOf(entry.getKey());

                Node singularValue = null;
                for (final Path p : entry.getValue()) {
                    final Node r = p.resolve(evaluationContext);
                    if (r != null) {
                        if (singularValue == null) {
                            singularValue = r;
                        } else {
                            if (singularValue != r) {
                                return null;
                            }
                        }
                    } else {
                        return null;
                    }
                }
                final Node expectation = evaluationContext.getNodeAt(expectedIndex);
                if (expectation != singularValue) {
                    PatternMatcher.this.logger.debug(" -> Failed! All paths for node {} resulting in : {}, but should be {}", expectedIndex, singularValue, expectation);
                    return null;
                }
            }

            final Map<Node, Node> mappings = new HashMap<>();
            for (final Map.Entry<Node, List<Path>> entry : nodeToPaths.entrySet()) {
                final int expectedIndex = nodeIndex.indexOf(entry.getKey());
                final Node targetNode = evaluationContext.getNodeAt(expectedIndex);
                mappings.put(entry.getKey(), targetNode);
            }

            return new Match(analysisPoint, mappings);
        }
    }

    private final Logger logger;
    private final Node pattern;
    private final CompiledPattern compiledPattern;

    public PatternMatcher(final Logger logger, final Node patternToCompile) {
        this.logger = logger;
        this.pattern = patternToCompile;
        this.compiledPattern = compile(patternToCompile);
    }

    public void debugOutput() {
        for (final Map.Entry<Node, List<Path>> entry : compiledPattern.nodeToPaths.entrySet()) {
            final int nodeIndex = compiledPattern.nodeIndex.indexOf(entry.getKey());
            for (final Path p : entry.getValue()) {
                logger.debug("Node #{} should be at {}", nodeIndex, p.path);
            }
        }
    }

    private static class PathAnalysisState {
        private final Node node;
        private final Path path;

        public PathAnalysisState(final Node node, final Path path) {
            this.node = node;
            this.path = path;
        }
    }

    private void registerPaths(final Node pivot, final CompiledPattern compiledPattern) {

        final Path root = new Path();

        final Stack<PathAnalysisState> workingQueue = new Stack<>();
        workingQueue.add(new PathAnalysisState(pivot, root));

        compiledPattern.registerToIndex(pivot);

        while (!workingQueue.isEmpty()) {
            final PathAnalysisState workingItem = workingQueue.pop();
            final Path workingItemPath = workingItem.path;

            final List<Path> workingItemPaths = compiledPattern.nodeToPaths.computeIfAbsent(workingItem.node, key -> new ArrayList<>());
            if (!workingItemPaths.contains(workingItemPath)) {
                workingItemPaths.add(workingItemPath);
            }

            final Node[] outgoing = workingItem.node.outgoingDataFlows();
            for (final Node n : outgoing) {
                final boolean registered = compiledPattern.registerToIndex(n);
                final Path newPath = workingItemPath.addOutgoing(n.nodeType, compiledPattern.nodeIndexOf(n));

                final List<Path> paths = compiledPattern.nodeToPaths.computeIfAbsent(n, key -> new ArrayList<>());
                if (!paths.contains(newPath)) {
                    paths.add(newPath);
                }

                if (registered) {
                    workingQueue.add(new PathAnalysisState(n, newPath));
                }
            }

            final Node[] incomingDataFlow = workingItem.node.incomingDataFlows;
            for (int i = 0; i < incomingDataFlow.length; i++) {
                final Node n = incomingDataFlow[i];
                final boolean registered = compiledPattern.registerToIndex(n);
                final Path newPath = workingItemPath.addIncoming(i, n.nodeType, compiledPattern.nodeIndexOf(n));

                final List<Path> paths = compiledPattern.nodeToPaths.computeIfAbsent(n, key -> new ArrayList<>());
                if (!paths.contains(newPath)) {
                    paths.add(newPath);
                }

                if (registered) {
                    workingQueue.add(new PathAnalysisState(n, newPath));
                }
            }

            if (workingItem.node instanceof ControlTokenConsumer) {
                final ControlTokenConsumer control = (ControlTokenConsumer) workingItem.node;
                for (final ControlTokenConsumer inc : control.controlComingFrom) {
                    final boolean registered = compiledPattern.registerToIndex(inc);
                    final Path newPath = workingItemPath.controlComingFrom(compiledPattern.nodeIndexOf(inc), inc.nodeType);

                    final List<Path> paths = compiledPattern.nodeToPaths.computeIfAbsent(inc, key -> new ArrayList<>());
                    if (!paths.contains(newPath)) {
                        paths.add(newPath);
                    }

                    if (registered) {
                        workingQueue.add(new PathAnalysisState(inc, newPath));
                    }
                }
                for (final Map.Entry<Projection, ControlTokenConsumer> entry : control.controlFlowsTo.entrySet()) {
                    final ControlTokenConsumer target = entry.getValue();
                    final boolean registered = compiledPattern.registerToIndex(target);

                    final Path newPath = workingItemPath.controlGoingTo(entry.getKey(), compiledPattern.nodeIndexOf(target), target.nodeType);

                    final List<Path> paths = compiledPattern.nodeToPaths.computeIfAbsent(target, key -> new ArrayList<>());
                    if (!paths.contains(newPath)) {
                        paths.add(newPath);
                    }

                    if (entry.getKey().edgeType() != EdgeType.BACK && registered) {
                        // Prevent recursion
                        workingQueue.add(new PathAnalysisState(target, newPath));
                    }
                }
            }
        }
    }

    private CompiledPattern compile(final Node patternRoot) {
        final CompiledPattern pattern = new CompiledPattern();

        registerPaths(patternRoot, pattern);

        return pattern;
    }

    public List<Match> findMatches(final Graph source) {
        final List<Match> result = new ArrayList<>();

        // We search for all analysis candidates in source with the same nodetype as pivot
        for (final Node analysisPoint : source.nodes().stream().filter(t -> t.nodeType == pattern.nodeType).collect(Collectors.toList())) {

            final Match match = compiledPattern.matchesTo(analysisPoint);
            if (match != null) {
                result.add(match);
            }
        }

        return result;
    }
}
