package de.mirkosertic.bytecoder.core.backend.wasm.ast;

import java.io.IOException;

public class GetWasmArray implements WasmValue {

    private final WasmType type;

    private final WasmValue array;

    private final WasmValue index;

    GetWasmArray(final WasmType type, final WasmValue array, final WasmValue index) {
        this.type = type;
        this.array = array;
        this.index = index;
    }

    @Override
    public void writeTo(final TextWriter writer, final ExportContext context) throws IOException {
        writer.opening();
        writer.write("array.get ");
        type.writeRefTo(writer);
        writer.space();
        array.writeTo(writer, context);
        writer.space();
        index.writeTo(writer, context);
        writer.closing();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer binaryWriter, final ExportContext context) {
        //TODO
    }
}
