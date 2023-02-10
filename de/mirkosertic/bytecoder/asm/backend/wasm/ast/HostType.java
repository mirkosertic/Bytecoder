package de.mirkosertic.bytecoder.asm.backend.wasm.ast;

public class HostType implements ReferencableType {

    HostType() {
    }

    @Override
    public void writeTo(final TextWriter writer) {
        writer.write("externref");
    }

    @Override
    public void writeRefTo(final TextWriter writer) {
        writer.write("externref");
    }

    @Override
    public void writeTo(final BinaryWriter.Writer sectionWriter) {
        sectionWriter.writeByte((byte) -1); // TODO
    }

    @Override
    public int index() {
        throw new IllegalStateException("Not implemented!");
    }
}
