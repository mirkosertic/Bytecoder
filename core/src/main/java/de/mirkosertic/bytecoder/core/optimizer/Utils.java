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
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.NodeType;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class Utils {

    public static boolean isVariableOrConstant(final Node n) {
        if (n.nodeType == NodeType.Variable) {
            return true;
        }
        return n.isConstant();
    }

    public static long methodSize(final ResolvedMethod rm) {
        return rm.methodBody.nodes().stream().filter(t ->
                t.nodeType == NodeType.MonitorEnter ||
                        t.nodeType == NodeType.Goto ||
                        t.nodeType == NodeType.Unwind ||
                        t.nodeType == NodeType.TableSwitch ||
                        t.nodeType == NodeType.ReturnValue ||
                        t.nodeType == NodeType.ClassInitialization ||
                        t.nodeType == NodeType.SetInstanceField ||
                        t.nodeType == NodeType.Return ||
                        t.nodeType == NodeType.Copy ||
                        t.nodeType == NodeType.MonitorExit ||
                        t.nodeType == NodeType.Region ||
                        t.nodeType == NodeType.TryCatch ||
                        t.nodeType == NodeType.ArrayStore ||
                        t.nodeType == NodeType.Nop ||
                        t.nodeType == NodeType.If).count();
    }

    public static List<Node> evaluationOrderOf(final Node node) {
        final List<Node> result = new ArrayList<>();

        // We do a DFS here
        final Set<Node> visited = new HashSet<>();
        final Stack<Node> workingQueue = new Stack<>();

        for (int i = node.incomingDataFlows.length - 1; i >= 0; i--) {
            workingQueue.push(node.incomingDataFlows[i]);
        }
        visited.add(node);

        while (!workingQueue.isEmpty()) {
            final Node workingItem = workingQueue.pop();
            if (visited.add(workingItem)) {
                if (!(workingItem instanceof ControlTokenConsumer)) {
                    if (workingItem.hasSideSideEffect() || workingItem.nodeType == NodeType.Variable) {
                        result.add(workingItem);
                    } else {
                        for (int i = workingItem.incomingDataFlows.length - 1; i >= 0; i--) {
                            workingQueue.push(workingItem.incomingDataFlows[i]);
                        }
                    }
                }
            }
        }

        return result;
    }

    public static int maxInlineSourceSize() {
        return Integer.parseInt(System.getProperty("bytecoder.maxInlineSourceSize", "20"));
    }

    public static int maxInlineTargetSize() {
        return Integer.parseInt(System.getProperty("bytecoder.maxInlineTargetSize", "200"));
    }

    public static boolean doSanityCheck() {
        return Boolean.parseBoolean(System.getProperty("bytecoder.optimizerSanityCheck", "false"));
    }
}
