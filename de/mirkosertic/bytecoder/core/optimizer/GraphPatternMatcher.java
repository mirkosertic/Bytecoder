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
package de.mirkosertic.bytecoder.core.optimizer;

import de.mirkosertic.bytecoder.core.ir.ControlTokenConsumer;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.NodeType;
import de.mirkosertic.bytecoder.core.ir.Projection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class GraphPatternMatcher {

    private interface Context {
    }

    private static class Path {

        private final String path;

        public Path() {
            this("R");
        }

        private Path(final String path) {
            this.path = path;
        }

        public Path addIncoming(final int incomingIndex, final NodeType nodeType, final int expectedIndex) {
            return new Path(path + ".i[" + incomingIndex + ":" + nodeType + ":" + expectedIndex + "]");
        }

        public Path controlComingFrom(final int nodeIndex, final NodeType nodeType) {
            return new Path(path + ".cin[" + nodeIndex + ":" + nodeType + "]");
        }

        public Path controlGoingTo(final Projection projection, final int nodeIndex, final NodeType nodeType) {
            return new Path(path + ".cout[" + nodeIndex + ":" + nodeType + ":" + projection + ":" + projection.edgeType() + "]");
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

        public Node resolve(final Node root, final Context context) {
            Node node = null;
            final StringTokenizer st = new StringTokenizer(path, ".");
            while (st.hasMoreTokens()) {
                final String token = st.nextToken();
                if ("R".equals(token)) {
                    node = root;
                } else if (token.startsWith("i[")) {
                    final int p = token.indexOf(":");
                    final int p2 = token.lastIndexOf(":");
                    final int index = Integer.parseInt(token.substring(2, p));
                    final NodeType nodeType = NodeType.valueOf(token.substring(p + 1, p2));
                    final int expectedNodeIndex = Integer.parseInt(token.substring(p2 + 1, token.length() - 1));
                    final Node[] incoming = node.incomingDataFlows;
                    if (incoming.length > index) {
                        node = incoming[index];
                        if (node.nodeType != nodeType) {
                            // Unexpected node type
                            return null;
                        }
                        // TODO: Verify index is the same
                    } else {
                        return null;
                    }
                } else {
                    throw new IllegalStateException("what do do with " + token);
                }
            }
            return node;
        }
    }

    private static class CompiledPattern {

        private final Map<Node, List<Path>> nodeToPaths = new HashMap<>();
        private final List<Node> nodeIndex = new ArrayList<>();

        private boolean registerToIndex(final Node n) {
            if (!nodeIndex.contains(n)) {
                nodeIndex.add(n);
                return true;
            }
            return false;
        }

        public int nodeIndexOf(final Node n) {
            return nodeIndex.indexOf(n);
        }

        public boolean matchesTo(final Node analysisPoint, final Context c) {
            // Check for all nodes and paths if they resolve according to their path
            for (final Map.Entry<Node, List<Path>> entry : nodeToPaths.entrySet()) {
                Node singularValue = null;
                for (final Path p : entry.getValue()) {
                    final Node r = p.resolve(analysisPoint, c);
                    if (r != null) {
                        if (singularValue == null) {
                            singularValue = r;
                        } else {
                            if (singularValue != r) {
                                return false;
                            }
                        }
                    } else {
                        return false;
                    }
                }
                // todo: check index of singular value in template and at analysispoint??
            }
            return true;
        }
    }

    private final Node pattern;
    private final CompiledPattern compiledPattern;

    public GraphPatternMatcher(final Node patternToCompile) {
        this.pattern = patternToCompile;
        this.compiledPattern = compile(patternToCompile);
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
                    compiledPattern.registerToIndex(target);

                    final Path newPath = workingItemPath.controlGoingTo(entry.getKey(), compiledPattern.nodeIndexOf(target), target.nodeType);

                    final List<Path> paths = compiledPattern.nodeToPaths.computeIfAbsent(target, key -> new ArrayList<>());
                    if (!paths.contains(newPath)) {
                        paths.add(newPath);
                    }

                    workingQueue.add(new PathAnalysisState(target, newPath));
                }
            }
        }
    }

    private CompiledPattern compile(final Node patternRoot) {
        final CompiledPattern pattern = new CompiledPattern();

        registerPaths(patternRoot, pattern);

        return pattern;
    }

    public List<Node> findMatches(final Graph source) {
        final List<Node> result = new ArrayList<>();

        final Context c = new Context() {
        };

        // We search for all analysis candidates in source with the same nodetype as pivot
        for (final Node analysisPoint : source.nodes().stream().filter(t -> t.nodeType == pattern.nodeType).collect(Collectors.toList())) {

            if (compiledPattern.matchesTo(analysisPoint, c)) {
                result.add(analysisPoint);
            }
        }

        return result;
    }
}
