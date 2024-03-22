package de.mirkosertic.bytecoder.core.optimizer;

import de.mirkosertic.bytecoder.core.ir.Copy;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.ir.Variable;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.util.Stack;

public class VariableIsConstant implements Optimizer {

    @Override
    public boolean optimize(final CompileUnit compileUnit, final ResolvedMethod method) {
        boolean changed = false;

        final Graph g = method.methodBody;

        // Variable and Constant propagation
        final Stack<Copy> workingQueue = new Stack<>();

        // We search for Constants and Variables A and check if they are copied to a variable B.
        // In this case, the variable B is redundant and can be replaced with A.
        g.nodes().stream().filter(t -> (t instanceof Copy) && t.incomingDataFlows[0].isConstant() && g.outgoingDataFlowsFor(t)[0] instanceof Variable).map(t -> (Copy) t).forEach(workingQueue::push);

        // We perform a recursive search across the invocation graph
        while (!workingQueue.isEmpty()) {
            final Copy workingItem = workingQueue.pop();
            // A Copy token has one incoming dataflow
            final Node source = workingItem.incomingDataFlows[0];
            // And only one outgoing dataflow
            // At this point we are sure it is a variable
            final Variable target = (Variable) g.outgoingDataFlowsFor(workingItem)[0];

            // We remap all control flow from the predecessor of the working item to the successor
            // of the working item. After this the successor is no longer part of the control flow
            workingItem.deleteFromControlFlow();

            g.remapDataFlow(target, source);

            g.deleteNode(workingItem);
            g.deleteNode(target);

            changed = true;
        }

        return changed;
    }
}
