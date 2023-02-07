package de.mirkosertic.bytecoder.asm.backend.wasm.ast;

import java.io.IOException;

public class GetStruct implements WasmValue{

    private final StructType structType;

    private final WasmValue source;

    private final int index;


    GetStruct(final StructType structType, final WasmValue source, final int index) {
        this.structType = structType;
        this.source = source;
        this.index = index;
    }

    @Override
    public void writeTo(final TextWriter writer, final ExportContext context) throws IOException {
        writer.opening();
        writer.write("struct.get $");
        writer.write(structType.getName());
        writer.write(" ");
        writer.write(Integer.toString(index));
        writer.space();
        source.writeTo(writer, context);
        writer.closing();

    }

    @Override
    public void writeTo(final BinaryWriter.Writer binaryWriter, final ExportContext context) {
        //TODO
    }
}
