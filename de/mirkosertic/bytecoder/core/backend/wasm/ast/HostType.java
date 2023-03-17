package de.mirkosertic.bytecoder.core.backend.wasm.ast;

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
        sectionWriter.writeByte((byte) 0x6f);
    }

    @Override
    public int index() {
        throw new IllegalStateException("Not implemented!");
    }
}
