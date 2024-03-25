/*
 * Copyright 2023 Mirko Sertic
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

import de.mirkosertic.bytecoder.core.backend.sequencer.DominatorTree;
import de.mirkosertic.bytecoder.core.ir.ClassInitialization;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.NodeType;
import de.mirkosertic.bytecoder.core.ir.ResolvedClass;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.util.Stack;

public class DeleteRedundantClassInitializations implements GlobalOptimizer {

    public DeleteRedundantClassInitializations() {
    }

    @Override
    public boolean optimize(final CompileUnit compileUnit, final ResolvedMethod method) {

        final Graph g = method.methodBody;

        // Variable and Constant propagation
        final Stack<ClassInitialization> workingQueue = new Stack<>();

        final DominatorTree dominatorTree = new DominatorTree(g);
        g.nodes().stream().filter(t -> t.nodeType == NodeType.ClassInitialization).map(t -> (ClassInitialization) t).forEach(workingQueue::add);

        while (!workingQueue.empty()) {
            final ClassInitialization ci = workingQueue.pop();
            final ResolvedClass rc = compileUnit.findClass(ci.type);
            if (!rc.requiresClassInitializer()) {
                ci.deleteFromControlFlow();
            } else {
                for (final ClassInitialization j : workingQueue) {
                    if (j != ci && ci.type.equals(j.type)) {
                        if (dominatorTree.dominates(ci, j)) {
                            j.deleteFromControlFlow();
                            workingQueue.remove(j);
                        }
                    }
                }
            }
        }
        return false;
    }
}
