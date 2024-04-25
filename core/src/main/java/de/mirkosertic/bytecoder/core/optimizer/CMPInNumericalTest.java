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

import de.mirkosertic.bytecoder.core.backend.BackendType;
import de.mirkosertic.bytecoder.core.ir.CMP;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.NodeType;
import de.mirkosertic.bytecoder.core.ir.NumericalTest;
import de.mirkosertic.bytecoder.core.ir.PrimitiveInt;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.util.Stack;

public class CMPInNumericalTest implements Optimizer {

    @Override
    public boolean optimize(final BackendType backendType, final CompileUnit compileUnit, final ResolvedMethod method) {
        boolean changed = false;

        final Graph g = method.methodBody;

        // Variable and Constant propagation
        final Stack<NumericalTest> workingQueue = new Stack<>();

        g.nodes().stream().filter(t -> t.nodeType == NodeType.NumericalTest && t.incomingDataFlows[0].nodeType == NodeType.CMP && t.incomingDataFlows[1].nodeType == NodeType.PrimitiveInt).map(t -> (NumericalTest) t).forEach(workingQueue::push);

        while (!workingQueue.isEmpty()) {
            final NumericalTest test = workingQueue.pop();
            final CMP compareExpression = (CMP) test.incomingDataFlows[0];
            final Node a = compareExpression.incomingDataFlows[0];
            final Node b = compareExpression.incomingDataFlows[1];
            final PrimitiveInt compareValue = (PrimitiveInt) test.incomingDataFlows[1];
            if (compareValue.value == 0) {
                switch (test.operation) {
                    case LT: {
                        // Compare operation must be -1, a < b
                        final NumericalTest replacement = g.newNumericalTest(NumericalTest.Operation.LT);
                        replacement.addIncomingData(a, b);
                        g.remapDataFlow(test, replacement);

                        test.removeFromIncomingData(compareExpression);
                        g.deleteNode(test);
                        g.deleteNode(compareExpression);
                        changed = true;
                        break;
                    }
                    case GT: {
                        // Compare must be 1, a > b
                        final NumericalTest replacement = g.newNumericalTest(NumericalTest.Operation.GT);
                        replacement.addIncomingData(a, b);
                        g.remapDataFlow(test, replacement);

                        test.removeFromIncomingData(compareExpression);
                        g.deleteNode(test);
                        g.deleteNode(compareExpression);
                        changed = true;
                        break;
                    }
                    case GE: {
                        // Compare must be >= 0, a >= b
                        final NumericalTest replacement = g.newNumericalTest(NumericalTest.Operation.GE);
                        replacement.addIncomingData(a, b);
                        g.remapDataFlow(test, replacement);

                        test.removeFromIncomingData(compareExpression);
                        g.deleteNode(test);
                        g.deleteNode(compareExpression);
                        changed = true;
                        break;
                    }
                    case LE: {
                        // Compare must be <= 0, a <= b
                        final NumericalTest replacement = g.newNumericalTest(NumericalTest.Operation.LE);
                        replacement.addIncomingData(a, b);
                        g.remapDataFlow(test, replacement);

                        test.removeFromIncomingData(compareExpression);
                        g.deleteNode(test);
                        g.deleteNode(compareExpression);
                        changed = true;
                        break;
                    }
                }
            }
        }

        return changed;
    }
}
