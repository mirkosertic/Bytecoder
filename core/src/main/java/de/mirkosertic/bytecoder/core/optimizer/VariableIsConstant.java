package de.mirkosertic.bytecoder.core.optimizer;

import de.mirkosertic.bytecoder.core.ir.Copy;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.NodeType;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.ir.Variable;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.util.Arrays;
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
        g.nodes().stream().filter(t -> (t.nodeType == NodeType.Copy) &&
                t.incomingDataFlows[0].isConstant()).map(t -> (Copy) t).forEach(workingQueue::push);

        // We perform a recursive search across the invocation graph
        while (!workingQueue.isEmpty()) {

            final Copy workingItem = workingQueue.pop();

            final Node[] outgoingDataFlows = g.outgoingDataFlowsFor(workingItem);
            if (outgoingDataFlows[0].nodeType == NodeType.Variable &&
                // Target variable is written exactly once!
                Arrays.stream(outgoingDataFlows[0].incomingDataFlows).filter(t -> t.nodeType == NodeType.Copy).count() == 1) {

                // A Copy token has one incoming dataflow
                final Node source = workingItem.incomingDataFlows[0];
                // And only one outgoing dataflow
                // At this point we are sure it is a variable
                final Variable target = (Variable) g.outgoingDataFlowsFor(workingItem)[0];

                g.remapDataFlow(target, source);

                // Reroute all other incoming data flows from this variable
                // to the source. This might be setInstanceField, ArrayStore and what ever...
                for (final Node incoming : target.incomingDataFlows) {
                    if (incoming != workingItem) {
                        source.addIncomingData(incoming);
                    }
                }

                // We remap all control flow from the predecessor of the working item to the successor
                // of the working item. After this the successor is no longer part of the control flow
                workingItem.deleteFromControlFlow();

                g.deleteNode(target);

                changed = true;
            }
        }

        //if (!workingQueue.isEmpty()) {
        //    compileUnit.getLogger().info("Next working item would be copy #{}", g.nodes().indexOf(workingQueue.peek()));
        //}

        return changed;
    }
}
