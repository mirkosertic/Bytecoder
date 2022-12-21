package de.mirkosertic.bytecoder.asm;

import org.objectweb.asm.Type;

public class ThisNode extends ConstantNode {

    public ThisNode(final Type type) {
        super(type);
    }
}
