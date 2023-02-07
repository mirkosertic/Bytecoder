package de.mirkosertic.bytecoder.asm.backend.wasm.ast;

import java.io.IOException;

public class SetStruct implements WasmExpression {

    private final StructType structType;

    private final WasmValue source;

    private final int index;

    private final WasmValue value;

    SetStruct(final StructType structType, final WasmValue source, final int index, final WasmValue value) {
        this.structType = structType;
        this.source = source;
        this.index = index;
        this.value = value;
    }

    @Override
    public void writeTo(final TextWriter writer, final ExportContext context) throws IOException {
        writer.opening();
        writer.write("struct.set $");
        writer.write(structType.getName());
        writer.write(" ");
        writer.write(Integer.toString(index));
        writer.space();
        source.writeTo(writer, context);
        writer.space();
        value.writeTo(writer, context);
        writer.closing();
        writer.newLine();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer binaryWriter, final ExportContext context) {
        //TODO
    }
}
