package de.mirkosertic.bytecoder.core.optimizer;

import de.mirkosertic.bytecoder.core.ir.CaughtException;
import de.mirkosertic.bytecoder.core.ir.ControlTokenConsumer;
import de.mirkosertic.bytecoder.core.ir.Copy;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.NullReference;
import de.mirkosertic.bytecoder.core.ir.ObjectString;
import de.mirkosertic.bytecoder.core.ir.PrimitiveValue;
import de.mirkosertic.bytecoder.core.ir.Projection;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.ir.RuntimeClass;
import de.mirkosertic.bytecoder.core.ir.TypeReference;
import de.mirkosertic.bytecoder.core.ir.Variable;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.util.Map;
import java.util.Stack;

public class VariableIsConstant implements Optimizer {

    private boolean isConstant(final Node node) {
        if (node instanceof PrimitiveValue) {
            return true;
        }
        if (node instanceof TypeReference) {
            return true;
        }
        if (node instanceof NullReference) {
            return true;
        }
        if (node instanceof ObjectString) {
            return true;
        }
        if (node instanceof RuntimeClass) {
            return true;
        }
        if (node instanceof CaughtException) {
            return true;
        }
        return false;
    }

    @Override
    public boolean optimize(final CompileUnit compileUnit, final ResolvedMethod method) {
        boolean changed = false;

        final Graph g = method.methodBody;

        // Variable and Constant propagation
        final Stack<Copy> workingQueue = new Stack<>();

        // We search for Constants and Variables A and check if they are copied to a variable B.
        // In this case, the variable B is redundant and can be replaced with A.
        g.nodes().stream().filter(t -> (t instanceof Copy) && isConstant(t.incomingDataFlows[0]) && t.outgoingFlows[0] instanceof Variable).map(t -> (Copy) t).forEach(workingQueue::push);

        // We perform a recursive search across the invocation graph
        while (!workingQueue.isEmpty()) {
            final Copy workingItem = workingQueue.pop();
            final Node source = workingItem.incomingDataFlows[0];
            final Variable target = (Variable) workingItem.outgoingFlows[0];

            // Step 1 : Remove copy from control flow
            //workingItem.deleteFromControlFlow();

            for (final ControlTokenConsumer pred : workingItem.controlComingFrom) {
                for (final Map.Entry<Projection, ControlTokenConsumer> entry : workingItem.controlFlowsTo.entrySet()) {
                    pred.remapControlFlowTo(workingItem, entry.getValue());
                    entry.getValue().controlComingFrom.remove(workingItem);
                }
            }

            source.removeFromOutgoingData(workingItem);
            workingItem.removeFromOutgoingData(target);

            g.remapDataFlow(target, source);

            g.deleteNode(workingItem);
            g.deleteNode(target);

            changed = true;
        }

        return changed;
    }
}
