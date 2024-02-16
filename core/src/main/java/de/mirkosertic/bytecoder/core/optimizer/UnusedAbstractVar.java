package de.mirkosertic.bytecoder.core.optimizer;

import de.mirkosertic.bytecoder.core.ir.AbstractVar;
import de.mirkosertic.bytecoder.core.ir.Constant;
import de.mirkosertic.bytecoder.core.ir.Copy;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.util.Stack;

public class UnusedAbstractVar implements Optimizer {

    @Override
    public boolean optimize(final CompileUnit compileUnit, final ResolvedMethod method) {
        boolean changed = false;

        final Graph g = method.methodBody;

        // Variable and Constant propagation
        final Stack<Copy> workingQueue = new Stack<>();

        // We search for Constants and Variables A and check if they are copied to a variable B.
        // In this case, the variable B is redundant and can be replaced with A.
        g.nodes().stream().filter(t -> (t instanceof Copy) && (t.incomingDataFlows[0] instanceof Constant || t.incomingDataFlows[0] instanceof AbstractVar) && t.outgoingFlows[0] instanceof AbstractVar).map(t -> (Copy) t).forEach(workingQueue::push);

        // We perform a recursive search across the invocation graph
        while (!workingQueue.isEmpty()) {
            final Copy workingItem = workingQueue.pop();
            final Node source = workingItem.incomingDataFlows[0];
            final AbstractVar target = (AbstractVar) workingItem.outgoingFlows[0];
            if (target.outgoingFlows.length == 0 && target.incomingDataFlows.length == 1) {
                // Target is unused and can be removed

                // Step 1 : Remove copy from control flow
                workingItem.deleteFromControlFlow();

                /*for (var pred : workingItem.controlComingFrom) {
                    for (final var entry : workingItem.controlFlowsTo.entrySet()) {
                        pred.remapControlFlowTo(workingItem, entry.getValue());
                        entry.getValue().controlComingFrom.remove(workingItem);
                    }
                }*/

                source.removeFromOutgoingData(workingItem);
                workingItem.removeFromOutgoingData(target);
                g.deleteNode(workingItem);
                g.deleteNode(target);

                changed = true;
            }
        }

        return changed;
    }
}
