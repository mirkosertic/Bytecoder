package de.mirkosertic.bytecoder.asm.backend.wasm.ast;

import java.io.IOException;

public class GetWasmArrayLength implements WasmValue {

    private final WasmType type;

    private final WasmValue array;

    GetWasmArrayLength(final WasmType type, final WasmValue array) {
        this.type = type;
        this.array = array;
    }

    @Override
    public void writeTo(final TextWriter writer, final ExportContext context) throws IOException {
        writer.opening();
        writer.write("array.len ");
        type.writeRefTo(writer);
        writer.space();
        array.writeTo(writer, context);
        writer.closing();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer binaryWriter, final ExportContext context) {
        //TODO
    }
}
