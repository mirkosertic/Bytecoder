package de.mirkosertic.bytecoder.asm.backend.wasm.ast;

import java.io.IOException;

public class GetStruct implements WasmValue {

    private final StructType structType;

    private final WasmValue source;

    private final String fieldName;


    GetStruct(final StructType structType, final WasmValue source, final String fieldName) {
        this.structType = structType;
        this.source = source;
        this.fieldName = fieldName;
    }

    @Override
    public void writeTo(final TextWriter writer, final ExportContext context) throws IOException {
        writer.opening();
        writer.write("struct.get $");
        writer.write(structType.getName());
        writer.write(" $");
        writer.write(fieldName);
        writer.space();
        source.writeTo(writer, context);
        writer.closing();

    }

    @Override
    public void writeTo(final BinaryWriter.Writer binaryWriter, final ExportContext context) {
        //TODO
    }
}
