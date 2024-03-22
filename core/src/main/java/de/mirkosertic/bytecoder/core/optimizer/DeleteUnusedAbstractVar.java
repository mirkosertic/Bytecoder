package de.mirkosertic.bytecoder.core.optimizer;

import de.mirkosertic.bytecoder.core.ir.AbstractVar;
import de.mirkosertic.bytecoder.core.ir.Copy;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.util.Stack;

public class DeleteUnusedAbstractVar implements Optimizer {

    @Override
    public boolean optimize(final CompileUnit compileUnit, final ResolvedMethod method) {
        boolean changed = false;

        final Graph g = method.methodBody;

        // Variable and Constant propagation
        final Stack<Copy> workingQueue = new Stack<>();

        // We search for Constants and Variables A and check if they are copied to a variable B.
        // In this case, the variable B is redundant and can be replaced with A.
        g.nodes().stream().filter(t -> (t instanceof Copy) && (t.incomingDataFlows[0].isConstant() || t.incomingDataFlows[0] instanceof AbstractVar) && g.outgoingDataFlowsFor(t)[0] instanceof AbstractVar).map(t -> (Copy) t).forEach(workingQueue::push);

        // We perform a search across the invocation graph
        while (!workingQueue.isEmpty()) {
            final Copy workingItem = workingQueue.pop();
            final AbstractVar target = (AbstractVar) g.outgoingDataFlowsFor(workingItem)[0];
            if (g.outgoingDataFlowsFor(target).length == 0 && target.incomingDataFlows.length == 1) {
                // Target is unused and can be removed

                // Step 1 : Remove copy from control flow
                workingItem.deleteFromControlFlow();

                g.deleteNode(workingItem);
                g.deleteNode(target);

                changed = true;
            }
        }

        return changed;
    }
}
