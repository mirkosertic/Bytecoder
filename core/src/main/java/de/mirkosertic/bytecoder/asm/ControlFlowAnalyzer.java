package de.mirkosertic.bytecoder.asm;

import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;

public class ControlFlowAnalyzer<T extends Value> extends Analyzer<T> {

    public ControlFlowAnalyzer(final Interpreter<T> interpreter) {
        super(interpreter);
    }

    @Override
    protected void newControlFlowEdge(int insnIndex, int successorIndex) {
        System.out.println("Flow from " + insnIndex + " to " + successorIndex);
    }

    @Override
    protected boolean newControlFlowExceptionEdge(int insnIndex, int successorIndex) {
        System.out.println("Exception flow from " + insnIndex + " to " + successorIndex);
        return true;
    }
}
